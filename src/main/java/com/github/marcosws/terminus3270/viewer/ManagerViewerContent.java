package com.github.marcosws.terminus3270.viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.marcosws.terminus3270.components.ScreenContent;
import com.github.marcosws.terminus3270.enums.EmulatorMode;
import com.github.marcosws.terminus3270.interfaces.IScreenContent;

public abstract class ManagerViewerContent implements IScreenContent {
	
	private static final Logger logger = LoggerFactory.getLogger(ManagerViewerContent.class);
	private final Object pauseLock = new Object();
	private TerminalViewer terminalViewer;
	private EmulatorMode emulatorMode;
	private boolean paused;
	

	
	public EmulatorMode getEmulatorMode() {
		return emulatorMode;
	}

	public ManagerViewerContent(EmulatorMode mode) {
		this.emulatorMode = mode;
	}
	
	protected void showViewer() {
		if (emulatorMode == EmulatorMode.VIEWER_MODE) {
			terminalViewer = new TerminalViewer(this::continueScreen,
                    this::handleSendTextFromViewer,
                    this::handleGetCursorFromViewer,
                    this::handleGetTextFromViewer,
                    this::handleGetTextByCursorFromViewer,
                    this::handleKeyFromViewer,
                    this::handleAbortSessionFromViewer);
			terminalViewer.show();
			setStatus("Viewer ready");
		}
	}
	
	/**
	 * Método chamado pelo botão "Send Text" do TerminalViewer.
	 * Deve ser sobrescrito ou implementado pela classe filha (S3270Emulator).
	 */
	protected void handleSendTextFromViewer(String field, String text, String direction) {
	    // implementação padrão vazia – a classe filha sobrescreve
	}
	
	/** Chamado pelo botão Get Text – deve retornar o valor lido */
	protected String handleGetTextFromViewer(String field, int size, String direction) {
	    return ""; // sobrescrito no S3270Emulator
	}
	
	protected void handleKeyFromViewer(String keyName) {
	    // sobrescrito no S3270Emulator
	}
	
	/** fieldLabel → "row,col" */
	protected String handleGetCursorFromViewer(String fieldLabel) {
	    return "0,0";
	}

	/** (fieldLabel, size) → texto */
	protected String handleGetTextFromViewer(String fieldLabel, Integer size) {
	    return "";
	}

	/** (row, col, size) → texto */
	protected String handleGetTextByCursorFromViewer(Integer row, Integer col, Integer size) {
	    return "";
	}

	protected void handleAbortSessionFromViewer() {
	    // sobrescrito no S3270Emulator
	}
	
	protected final void pauseScreenViewer() {
		
		if (emulatorMode != EmulatorMode.VIEWER_MODE) {
			logger.warn("pauseScript() was called without a viewer; continuing execution");
			return;
		}
		
		synchronized (pauseLock) {
			paused = true;
			terminalViewer.setContinueEnabled(true);
			logger.info("Script paused; wainting for Continue in the Terminal Viewer");
			while(paused) {
				try {
					pauseLock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					paused  = false;
					terminalViewer.setContinueEnabled(false);
					throw new IllegalStateException("Interrupted while wanting for Continue",e);
				}
			}
		}
		
	}
	
	private void continueScreen() {
		synchronized (pauseLock) {
			if(!paused) {
				return;
			}
			paused = false;
			terminalViewer.setContinueEnabled(false);
			pauseLock.notifyAll();
		}
	}
	
	protected void updateScreenViewer() {
		
		ScreenContent screenContent = this.getScreenContent();
		switch (emulatorMode) {
			case HEADLESS_MODE:
				logger.debug("Current screen:\n{}", screenContent.getContent());
				break;
			case VIEWER_MODE:
				logger.debug("[VIEWER MODE] Current screen:\n{}", screenContent.getContent());
				this.terminalViewer.updateScreen(screenContent.getContent());
				break;
			default:
				throw new IllegalArgumentException("Modo de emulador desconhecido: " + emulatorMode);
		}
		
	}
	
	protected void closeViewer() {
		continueScreen();
		if (terminalViewer != null) {
			terminalViewer.close();
			terminalViewer = null;
		}
	}
	
	protected void setStatus(String message) {
	    if (terminalViewer != null) {
	        terminalViewer.setStatus(message);
	    }
	}
	

}
