package io.github.marcosws.terminus3270.integration.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import io.github.marcosws.terminus3270.integration.EmulatorIntegrationTest;

public class ConfigFakeS3270 {
	
	
	public static void configSetup() throws IOException {
		
		// 1. Identificar o Sistema Operacional
        String osName = System.getProperty("os.name").toLowerCase();
        String binarySubPath;
        String binaryFileName;
		
        if (osName.contains("win")) {
            binarySubPath = "fakes3270/bin/windows-x64/fakes3270.exe";
            binaryFileName = "fakes3270.exe";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("mac")) {
            binarySubPath = "fakes3270/bin/linux-x64/fakes3270";
            binaryFileName = "fakes3270";
        } else {
            throw new UnsupportedOperationException("Sistema operacional não suportado para testes do s3270: " + osName);
        }

        // 2. Criar diretório temporário para a execução
        File tempDir = Files.createTempDirectory("fakes3270").toFile();
        tempDir.deleteOnExit();

        File targetBinary = new File(tempDir, binaryFileName);

        // 3. Tentar carregar via ClassLoader (sem a barra inicial para compatibilidade com o Maven)
        try (InputStream inputStream = EmulatorIntegrationTest.class.getClassLoader().getResourceAsStream(binarySubPath)) {
            if (inputStream != null) {
                Files.copy(inputStream, targetBinary.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Fallback: Tentar ler diretamente do sistema de arquivos (ótimo para rodar direto da IDE)
                File sourceFile = new File("src/test/resources/" + binarySubPath);
                if (sourceFile.exists()) {
                    Files.copy(sourceFile.toPath(), targetBinary.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    throw new IllegalStateException("Binário do s3270 não encontrado nem no classpath nem em: " + sourceFile.getAbsolutePath());
                }
            }
        }

        // 4. Garantir permissão de execução (crucial para Linux/Mac no GitHub Actions)
        if (!targetBinary.setExecutable(true)) {
            throw new RuntimeException("Não foi possível dar permissão de execução ao binário temporário.");
        }

        String absolutePath = targetBinary.getAbsolutePath();
        
        // 5. Definir a propriedade de sistema para a sua biblioteca consumir
        System.setProperty("s3270.path", absolutePath);
        
        System.out.println(">>> Binário s3270 configurado para testes em: " + absolutePath);
		
	}

}
