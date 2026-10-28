package io.github.marcosws.terminus3270.integration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.github.marcosws.terminus3270.S3270Emulator;
import io.github.marcosws.terminus3270.S3270Session;
import io.github.marcosws.terminus3270.components.CursorPosition;
import io.github.marcosws.terminus3270.enums.EmulatorMode;
import io.github.marcosws.terminus3270.enums.PFKey;
import io.github.marcosws.terminus3270.integration.utils.ConfigFakeS3270;

public class EmulatorIntegrationTestLocal {

	private static S3270Session session;
	private static S3270Emulator emulator;
	
	
	public static void main(String[] args) throws IOException {
		
		ConfigFakeS3270.configSetup();
		
		String path = System.getProperty("s3270.path");
		session = new S3270Session(List.of(path), Map.of());
		emulator = new S3270Emulator(session, EmulatorMode.VIEWER_MODE);

		emulator.getSession().connect("fake", "23");
		
		System.out.println(emulator.asciiScreen());
		emulator.pauseScript();
		emulator.sendTextByField("Userid:", "TESTE");
		emulator.tab();
		emulator.pauseScript();
		emulator.sendTextByField("Password:", "1234");
		System.out.println(emulator.asciiScreen());
		emulator.pauseScript();
		emulator.enter();
		System.out.println(emulator.asciiScreen());
		emulator.pauseScript();
		
		String option1 = emulator.getTextBeforeField("- SALDO", 3);
		emulator.sendTextByField("Opcao ==>", option1.trim());
		emulator.pauseScript();
		emulator.enter();
		
		// row=2; col=26; size=17; result='CONSULTA DE SALDO' <- Information from TerminalViwer
		System.out.println( "GET TEXT FROM POSITION: [" + emulator.getText(new CursorPosition(2, 26), 17) + "]"); 
		
		System.out.println(emulator.asciiScreen());
		emulator.pauseScript();
		emulator.pressPF(PFKey.PF3);
		
		System.out.println(emulator.asciiScreen());
		emulator.pauseScript();
		
		String option2 = emulator.getTextBeforeField("- CADASTRO", 3);
		emulator.sendTextByField("Opcao ==>", option2.trim());
		
		System.out.println(emulator.asciiScreen());
		emulator.pauseScript();
		
		emulator.enter();
		
		// [18:58:53] Get Text By Position | row=2; col=26; size=19; result='CADASTRO DE CLIENTE'
		System.out.println( "GET TEXT FROM POSITION: [" + emulator.getText(new CursorPosition(2, 26), 19) + "]"); 
		
		System.out.println(emulator.asciiScreen());
		emulator.pauseScript();
		
		emulator.getSession().disconnect();
		emulator.getSession().quit();
		
	}

}
