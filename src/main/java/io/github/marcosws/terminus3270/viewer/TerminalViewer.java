package io.github.marcosws.terminus3270.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import io.github.marcosws.terminus3270.utils.Utils;
import io.github.marcosws.terminus3270.viewer.ui.ActionLogPanel;
import io.github.marcosws.terminus3270.viewer.ui.ActionScriptPanel;
import io.github.marcosws.terminus3270.viewer.ui.CommandsDialog;
import io.github.marcosws.terminus3270.viewer.ui.KeysDialog;
import io.github.marcosws.terminus3270.viewer.ui.ScreenPanel;
import io.github.marcosws.terminus3270.viewer.ui.StatusBar;
import io.github.marcosws.terminus3270.viewer.ui.UIComponents;

/**
 * TODO implements records scripts in buttons for features
 */
public class TerminalViewer extends UIComponents implements ActionListener {

    private final JFrame frame;
    private JTabbedPane tabbedPane;

    private final JButton continueButton;
    private final JButton abortButton;
    private String lastScreen = "";
    private JButton keysButton;
    private JButton commandsButton;
    private Runnable onContinue;
    private Runnable onAbortSession;
    
    private KeysDialog keysDialog;
    private CommandsDialog commandsDialog;
    private ScreenPanel screenPanel;
    private ActionLogPanel actionLogPanel;
    private ActionScriptPanel actionScriptPanel;
    private StatusBar statusBar;
    
	public StatusBar getStatusBar() {
		return statusBar;
	}

	public JFrame getFrame() {
		return frame;
	}

	/**
     * @param onContinue          Continue
     * @param onSendText          (field, text, direction)
     * @param onGetCursor         field → "row,col"
     * @param onGetText           (field, size, direction) → value
     * @param onGetTextByCursor   (row, col, size) → value
     * @param onKeyAction         nome da tecla: "Enter", "PF3", "PA1"...
     * @param onAbortSession      encerra sessão + fecha viewer
     */
    public TerminalViewer(Runnable onContinue,
            TriConsumer<String, String, String> onSendText,
            Consumer<String> onSendTextOnly,
            Function<String, String> onGetCursor,
            Function<String, String> onGetPositionAfterField,
            TriFunction<String, Integer, String, String> onGetText,
            TriFunction<Integer, Integer, Integer, String> onGetTextByCursor,
            TriConsumer<Integer, Integer, String> onSendTextByPosition,
            Consumer<String> onKeyAction,
            Runnable onAbortSession) {
    	
    	this.onContinue = onContinue;
    	this.onAbortSession = onAbortSession;

        frame = new JFrame("Terminus 3270 - Terminal Viewer");
        tabbedPane = new JTabbedPane();
        actionLogPanel = new ActionLogPanel();
        actionScriptPanel = new ActionScriptPanel();
        screenPanel = new ScreenPanel();
        statusBar = new StatusBar();
        keysDialog = new KeysDialog(this, actionLogPanel, actionScriptPanel ,statusBar, onKeyAction);
        commandsDialog = new CommandsDialog(
        		this, 
        		statusBar, 
        		actionLogPanel,
        		actionScriptPanel,
        		screenPanel,
        		onSendText, 
        		onSendTextOnly, 
        		onGetCursor,
        		onGetPositionAfterField, 
        		onGetText, 
        		onGetTextByCursor, 
        		onSendTextByPosition);

        
        List<Image> icons = new ArrayList<>();
        addIcons(icons, "/icons/terminus3270_16.png");
        addIcons(icons, "/icons/terminus3270_32.png");

        if (!icons.isEmpty()) {
            frame.setIconImages(icons);
        }

        continueButton = createButton("Continue");
        continueButton.setEnabled(false);
        continueButton.setForeground(new Color(0, 160, 0));
        continueButton.addActionListener(this);
        
        abortButton = createButton("Abort Session");
        abortButton.setForeground(new Color(160, 0, 0));
        abortButton.addActionListener(this);
        
        commandsButton = createButton("Commands");
        commandsButton.setToolTipText("Abre a janela com os comandos do terminal (Send Text, Get Text, etc.)");
        commandsButton.setForeground(new Color(0, 0, 160));
        commandsButton.addActionListener(this);
        
        keysButton = createButton("Keys");
        keysButton.setToolTipText("Abre a janela com as teclas PF, PA, Enter, Tab, etc.");
        keysButton.setForeground(new Color(0, 0, 160));
        keysButton.addActionListener(this);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttonsPanel.add(continueButton);
        buttonsPanel.add(commandsButton);
        buttonsPanel.add(keysButton);
        buttonsPanel.add(abortButton);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(buttonsPanel, BorderLayout.WEST);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JScrollPane(screenPanel.getAreaScreen()), BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new BorderLayout());
        
        tabbedPane.addTab("Log", new JScrollPane(actionLogPanel.getActionLogArea()));
        tabbedPane.addTab("Script", new JScrollPane(actionScriptPanel.getActionScriptArea()));
        
        footerPanel.add(tabbedPane, BorderLayout.CENTER);
        footerPanel.add(statusBar.getStatusBar(), BorderLayout.SOUTH);

        frame.add(northPanel, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(footerPanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setSize(880, 720);
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setResizable(true);
        
    }

	public void warn(String msg) {
        statusBar.setStatus(msg);
        JOptionPane.showMessageDialog(frame, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    public void error(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }



    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------
    public void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public void updateScreen(String screen) {
        if (screen == null || screen.equals(lastScreen)) return;
        lastScreen = screen;
        SwingUtilities.invokeLater(() -> {
        	screenPanel.getAreaScreen().setText(Utils.frameText(screen));
        	screenPanel.getAreaScreen().setCaretPosition(0);
        });
    }



    public void setContinueEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            continueButton.setEnabled(enabled);
            if (enabled) statusBar.setStatus("Script paused — click Continue");
        });
    }

    public void close() {
        SwingUtilities.invokeLater(() -> {
            if (commandsDialog.getCommandsDialog() != null) {
                commandsDialog.getCommandsDialog().dispose();
                commandsDialog.setCommandsDialog(null);
                commandsDialog = null;
            }
            if (keysDialog.getKeysDialog() != null) {
                keysDialog.getKeysDialog().dispose();
                keysDialog.setKeysDialog(null);
                keysDialog = null;
            }
            if (frame != null) frame.dispose();
        });
    }
    
    
    private static void addIcons(List<Image> listIcons, String caminho) {
        URL url = TerminalViewer.class.getResource(caminho);
        if (url != null) {
        	listIcons.add(new ImageIcon(url).getImage());
        } else {
            System.err.println("Ícone não encontrado: " + caminho);
        }
    }

	@Override
	public void actionPerformed(ActionEvent event) {
		
		if(event.getSource().equals(keysButton)) {
			keysDialog.showKeysWindow();
		}
		else if(event.getSource().equals(commandsButton)) {
			commandsDialog.showCommandsWindow();
		}
		else if(event.getSource().equals(continueButton)) {
            actionLogPanel.recordAction("Continue", "button=Continue");
            onContinue.run();
		}
		else if(event.getSource().equals(abortButton)) {
            int opt = JOptionPane.showConfirmDialog(frame,
                    "Encerrar a sessão e fechar o viewer?",
                    "Abort Session",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (opt == JOptionPane.YES_OPTION) {
                try {
                    onAbortSession.run();
                } catch (Exception ex) {
                    close();
                }
            }
		}
	
	}
	
	
	
	
	
	
}