package io.github.marcosws.terminus3270.integration;

import io.github.marcosws.terminus3270.S3270Emulator;
import io.github.marcosws.terminus3270.S3270Session;
import io.github.marcosws.terminus3270.enums.EmulatorMode;

public class EmulatorIntegrationTestRealSession {

	public static void main(String[] args) throws InterruptedException {
		
		
		S3270Session session = new S3270Session();
		S3270Emulator emulator = new S3270Emulator(session, EmulatorMode.VIEWER_MODE);
		
		emulator.getSession().connect("192.168.15.20", "23");
		
		emulator.waitForText("W E L C O M E  T O", 3000);
		
		System.out.println(emulator.asciiScreen());
		
		emulator.pauseScript();
		
		emulator.sendString("LOGON IBMUSER");
		emulator.enter();
		
		System.out.println(emulator.asciiScreen());
		
		emulator.waitForText("TSO/E LOGON", 3000);
		
		emulator.sendTextByField("Password  ===>", "SYS1");
		emulator.enter();
		
		System.out.println(emulator.asciiScreen());
		
		emulator.pauseScript();
		
		System.out.println(emulator.asciiScreen());
		emulator.waitForText("WELCOME TO THE P390 SYSTEM!", 3000);
		emulator.enter();
		
		emulator.pauseScript();
		
		emulator.getSession().disconnect();
		emulator.getSession().quit();
		

	}

}
