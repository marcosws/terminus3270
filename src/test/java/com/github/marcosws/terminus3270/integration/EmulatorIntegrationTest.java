package com.github.marcosws.terminus3270.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;


import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.marcosws.terminus3270.S3270Emulator;
import com.github.marcosws.terminus3270.S3270Session;
import com.github.marcosws.terminus3270.enums.PFKey;
import com.github.marcosws.terminus3270.integration.utils.ConfigFakeS3270;

public class EmulatorIntegrationTest {
	
	private S3270Session session;
	private S3270Emulator emulator;
	
	
	@BeforeAll
	static void setUpFakeS3270() throws IOException {
		
		ConfigFakeS3270.configSetup();
		
	}
	
	@BeforeEach
	void setUp() {
		
		String path = System.getProperty("s3270.path");
		session = new S3270Session(List.of(path), Map.of());
		emulator = new S3270Emulator(session);
		session.connect("fake", "23");
		
	}
	
	@AfterEach
	void tearDown() {
		session.disconnect();
	    session.quit();
	}
	
	
	@Test
	@DisplayName("Mainframe navigation logon test successful")
	void testNavigationMainframeLogonWithSuccess() {
		
		emulator.sendTextByField("Userid:", "TESTE");
		emulator.tab();
		emulator.sendTextByField("Password:", "1234");
		emulator.enter();
		assertTrue(emulator.asciiScreen().contains("MENU PRINCIPAL"));

	}
	
	@Test
	@DisplayName("Mainframe navigation Get Saldo test successful")
	void testNavigationMainframeGetSaldoWithSuccess() {
		
		emulator.sendTextByField("Userid:", "TESTE");
		emulator.tab();
		emulator.sendTextByField("Password:", "1234");
		emulator.enter();
		String option = emulator.getTextBeforeField("- SALDO", 3);
		emulator.sendTextByField("Opcao ==>", option.trim());
		emulator.enter();
		assertTrue(emulator.asciiScreen().contains("CONSULTA DE SALDO"));
		assertEquals("R$ 1.234,56", emulator.getTextByField("Saldo atual:", 18).trim());
		
	}
	
	@Test
	@DisplayName("Mainframe navigation return Menu test successful")
	void testNavigationMainframeReturnMenuWithSuccess() {
		
		emulator.sendTextByField("Userid:", "TESTE");
		emulator.tab();
		emulator.sendTextByField("Password:", "1234");
		emulator.enter();
		String option = emulator.getTextBeforeField("- SALDO", 5);
		emulator.sendTextByField("Opcao ==>", option.trim());
		emulator.enter();
		emulator.pressPF(PFKey.PF3);
		assertTrue(emulator.asciiScreen().contains("MENU PRINCIPAL"));
		
	}

}
