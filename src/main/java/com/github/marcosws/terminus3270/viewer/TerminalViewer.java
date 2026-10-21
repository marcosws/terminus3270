package com.github.marcosws.terminus3270.viewer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.github.marcosws.terminus3270.viewer.utils.Utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
/**
 * ToDo: adicionar Botão: Send text by Position usando o moveAndSendString(row, col, text);
 */
public class TerminalViewer {

    private static final Dimension BTN_SIZE = new Dimension(158, 30);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JFrame frame;
    private final JTextArea areaScreen;
    private final JLabel statusBar;
    private final JTextArea actionLogArea;

    private final JButton continueButton;
    private final JButton copyButton;
    private final JButton sendTextButton;
    private final JButton getPositionButton;
    private final JButton getTextButton;
    private final JButton getTextByPositionButton;
    private final JButton abortButton;

    private final JTextField fieldNameField;
    private final JTextField textField;
    private final JComboBox<String> directionCombo;
    private final JTextField sizeField;
    private final JTextField rowField;
    private final JTextField colField;

    private String lastScreen = "";

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
                          Function<String, String> onGetCursor,
                          TriFunction<String, Integer, String, String> onGetText,
                          TriFunction<Integer, Integer, Integer, String> onGetTextByCursor,
                          Consumer<String> onKeyAction,
                          Runnable onAbortSession) {

        frame = new JFrame("Terminal 3270 Viewer");
        
        List<Image> icons = new ArrayList<>();
        addIcons(icons, "/icons/terminus3270_16.png");
        addIcons(icons, "/icons/terminus3270_32.png");

        if (!icons.isEmpty()) {
            frame.setIconImages(icons);
        }

        // ===== Tela 3270 =====
        areaScreen = new JTextArea(28, 84);
        areaScreen.setFont(new Font("Monospaced", Font.PLAIN, 16));
        areaScreen.setEditable(false);
        areaScreen.setFocusable(false);
        areaScreen.setBackground(Color.BLACK);
        areaScreen.setForeground(Color.CYAN);

        // ===== Status Bar =====
        statusBar = new JLabel(" Ready ");
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        statusBar.setOpaque(true);
        statusBar.setBackground(new Color(240, 240, 240));
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 12));

        actionLogArea = new JTextArea(5, 84);
        actionLogArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        actionLogArea.setEditable(false);
        actionLogArea.setLineWrap(false);
        actionLogArea.setFocusable(true);
        actionLogArea.setBackground(new Color(245, 247, 250));
        actionLogArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        actionLogArea.append("=== Terminal actions log ===\n");

        // ===== Campos =====
        fieldNameField = new JTextField(20);
        fieldNameField.setToolTipText("Label do campo (ex: User:)");

        textField = new JTextField(20);
        textField.setToolTipText("Texto a enviar (Send Text) ou resultado dos Gets");

        directionCombo = new JComboBox<>(new String[]{
                "Right Field",
                "Left Field",
                "Top Field",
                "Botton Field"
        });
        directionCombo.setPreferredSize(new Dimension(120, 20));

        sizeField = new JTextField(4);
        sizeField.setText("10");
        sizeField.setToolTipText("Tamanho para Get Text / Get Text By Cursor");

        rowField = new JTextField(4);
        rowField.setToolTipText("Linha (preenchida por Get Cursor)");

        colField = new JTextField(4);
        colField.setToolTipText("Coluna (preenchida por Get Cursor)");

        // ===== Botões da barra superior =====
        continueButton = createButton("Continue");
        continueButton.setEnabled(false);
        continueButton.setForeground(new Color(0, 160, 0));
        continueButton.addActionListener(e -> {
            recordAction("Continue", "button=Continue");
            onContinue.run();
        });

        copyButton = createButton("Copy Text");
        copyButton.addActionListener(e -> {
            String screenText = areaScreen.getText();
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(screenText), null);
            recordAction("Copy Text", "screenCopied=true; chars=" + screenText.length());
            setStatus("Screen copied to clipboard");
        });

        sendTextButton = createButton("Send Text");
        sendTextButton.addActionListener(e -> {
            String field = fieldNameField.getText().trim();
            String text = textField.getText();
            String direction = (String) directionCombo.getSelectedItem();
            if (field.isEmpty()) {
                warn("Informe o Field (label).");
                return;
            }
            try {
                setStatus("Sending to '" + field + "' (" + direction + ")...");
                recordAction("Send Text", "field='" + field + "'; text='" + text + "'; direction='" + direction + "'");
                onSendText.accept(field, text, direction);
                setStatus("Text sent to '" + field + "'");
            } catch (Exception ex) {
                setStatus("Error: " + ex.getMessage());
                error(ex.getMessage());
            }
        });

        getPositionButton = createButton("Get Position");
        getPositionButton.addActionListener(e -> {
            String field = fieldNameField.getText().trim();
            if (field.isEmpty()) {
                warn("Informe o Field (texto a localizar), ex: FAKE3270 v2.1 - SIMULADOR MAINFRAME");
                return;
            }
            try {
                setStatus("Getting start position of '" + field + "'...");
                recordAction("Get Position", "field='" + field + "'");

                // Retorna a posição inicial (row,col) do texto no Field
                String coords = onGetCursor.apply(field); // "row,col"
                String[] parts = coords.split("\\s*,\\s*");
                if (parts.length < 2) {
                    throw new IllegalArgumentException("Resposta de posição inválida: '" + coords + "'");
                }

                String rowValue = parts[0].trim();
                String colValue = parts[1].trim();
                rowField.setText(rowValue);
                colField.setText(colValue);
                updateGetTextByCursorEnabled();

                // Size = comprimento do texto localizado; Text = o próprio texto encontrado
                int fieldSize = field.length();
                int row = Integer.parseInt(rowValue);
                int col = Integer.parseInt(colValue);
                String value = onGetTextByCursor.apply(row, col, fieldSize);
                String normalized = value == null ? field : value;
                textField.setText(normalized);
                sizeField.setText(String.valueOf(fieldSize));

                setStatus("Position → Row: " + rowValue + "  Col: " + colValue + "  Size: " + fieldSize);
                recordAction("Get Position", "field='" + field + "'; startPosition='row=" + rowValue + ", col=" + colValue + "'; text='" + normalized + "'; size=" + fieldSize);
            } catch (Exception ex) {
                setStatus("Error: " + ex.getMessage());
                error(ex.getMessage());
            }
        });

        getTextButton = createButton("Get Text");
        getTextButton.addActionListener(e -> {
            String field = fieldNameField.getText().trim();
            Integer size = parsePositiveInt(sizeField.getText().trim(), "Size");
            String direction = (String) directionCombo.getSelectedItem();
            if (field.isEmpty()) {
                warn("Informe o Field (label).");
                return;
            }
            if (size == null) return;
            try {
                setStatus("Reading field '" + field + "' (" + direction + ") size=" + size + "...");
                recordAction("Get Text", "field='" + field + "'; size=" + size + "; direction='" + direction + "'");
                String value = onGetText.apply(field, size, direction);
                textField.setText(value);
                setStatus("Get Text → \"" + value + "\"");
                recordAction("Get Text", "field='" + field + "'; size=" + size + "; direction='" + direction + "'; result='" + value + "'");
            } catch (Exception ex) {
                setStatus("Error: " + ex.getMessage());
                error(ex.getMessage());
            }
        });

        getTextByPositionButton = createButton("Get Text By Position");
        getTextByPositionButton.setEnabled(false);
        getTextByPositionButton.addActionListener(e -> {
            Integer row = parsePositiveInt(rowField.getText().trim(), "Row");
            Integer col = parsePositiveInt(colField.getText().trim(), "Col");
            Integer size = parsePositiveInt(sizeField.getText().trim(), "Size");
            if (row == null || col == null || size == null) return;
            try {
                setStatus("Reading at (" + row + "," + col + ") size=" + size + "...");
                recordAction("Get Text By Position", "row=" + row + "; col=" + col + "; size=" + size);
                String value = onGetTextByCursor.apply(row, col, size);
                textField.setText(value);
                setStatus("Get Text By Position → \"" + value + "\"");
                recordAction("Get Text By Position", "row=" + row + "; col=" + col + "; size=" + size + "; result='" + value + "'");
            } catch (Exception ex) {
                setStatus("Error: " + ex.getMessage());
                error(ex.getMessage());
            }
        });

        abortButton = createButton("Abort Session");
        abortButton.setForeground(new Color(160, 0, 0));
        abortButton.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(frame,
                    "Encerrar a sessão e fechar o viewer?",
                    "Abort Session",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (opt == JOptionPane.YES_OPTION) {
                setStatus("Aborting session...");
                recordAction("Abort Session", "confirmed=true");
                try {
                    onAbortSession.run();
                } catch (Exception ex) {
                    setStatus("Error: " + ex.getMessage());
                    close();
                }
            }
        });

        DocumentListener coordListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateGetTextByCursorEnabled(); }
            public void removeUpdate(DocumentEvent e) { updateGetTextByCursorEnabled(); }
            public void changedUpdate(DocumentEvent e) { updateGetTextByCursorEnabled(); }
        };
        rowField.getDocument().addDocumentListener(coordListener);
        colField.getDocument().addDocumentListener(coordListener);

        // ===== Layout superior: botões + campos =====
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttonsPanel.add(continueButton);
        buttonsPanel.add(copyButton);
        buttonsPanel.add(sendTextButton);
        buttonsPanel.add(getTextButton);
        buttonsPanel.add(getPositionButton);
        buttonsPanel.add(getTextByPositionButton);
        buttonsPanel.add(abortButton);

        JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        fieldsPanel.add(new JLabel("Field:"));
        fieldsPanel.add(fieldNameField);
        fieldsPanel.add(new JLabel("Text:"));
        fieldsPanel.add(textField);
        fieldsPanel.add(directionCombo);
        fieldsPanel.add(new JLabel("Size:"));
        fieldsPanel.add(sizeField);
        fieldsPanel.add(new JLabel("Row:"));
        fieldsPanel.add(rowField);
        fieldsPanel.add(new JLabel("Col:"));
        fieldsPanel.add(colField);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(buttonsPanel);
        northPanel.add(fieldsPanel);

        // ===== Painel lateral de teclas =====
        JPanel keysPanel = createKeysPanel(onKeyAction);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JScrollPane(areaScreen), BorderLayout.CENTER);
        centerPanel.add(keysPanel, BorderLayout.EAST);

        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.add(statusBar, BorderLayout.NORTH);
        footerPanel.add(new JScrollPane(actionLogArea), BorderLayout.CENTER);

        frame.add(northPanel, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(footerPanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setSize(1160, 820);
        frame.setMinimumSize(new Dimension(1020, 820));
        frame.setResizable(true);
    }

    // -------------------------------------------------------------------------
    // Painel lateral
    // -------------------------------------------------------------------------
    private JPanel createKeysPanel(Consumer<String> onKeyAction) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        panel.setPreferredSize(new Dimension(160, 0));

        panel.add(sectionLabel("Navigation"));
        panel.add(keyButton("Enter", onKeyAction));
        panel.add(keyButton("Tab", onKeyAction));
        panel.add(keyButton("Home", onKeyAction));
        panel.add(keyButton("Backspace", onKeyAction));
        panel.add(Box.createVerticalStrut(6));

        panel.add(sectionLabel("Edit"));
        panel.add(keyButton("Clear", onKeyAction));
        panel.add(keyButton("Reset", onKeyAction));
        panel.add(keyButton("DeleteField", onKeyAction));
        panel.add(keyButton("EraseEOF", onKeyAction));
        panel.add(keyButton("EraseInput", onKeyAction));
        panel.add(Box.createVerticalStrut(8));

        panel.add(sectionLabel("PF Keys"));
        JPanel pfGrid = new JPanel(new GridLayout(0, 3, 3, 3));
        for (int i = 1; i <= 24; i++) {
            pfGrid.add(smallKeyButton("PF" + i, onKeyAction));
        }
        panel.add(pfGrid);
        panel.add(Box.createVerticalStrut(8));

        panel.add(sectionLabel("PA Keys"));
        JPanel paRow = new JPanel(new GridLayout(1, 3, 3, 3));
        paRow.add(smallKeyButton("PA1", onKeyAction));
        paRow.add(smallKeyButton("PA2", onKeyAction));
        paRow.add(smallKeyButton("PA3", onKeyAction));
        panel.add(paRow);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
        return label;
    }

    private JButton keyButton(String name, Consumer<String> onKeyAction) {
        JButton btn = new JButton(name);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        btn.setFocusable(false);
        btn.addActionListener(e -> fireKey(name, onKeyAction));
        return btn;
    }

    private JButton smallKeyButton(String name, Consumer<String> onKeyAction) {
        JButton btn = new JButton(name);
        btn.setMargin(new Insets(2, 2, 2, 2));
        btn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        btn.setFocusable(false);
        btn.addActionListener(e -> fireKey(name, onKeyAction));
        return btn;
    }

    private void fireKey(String name, Consumer<String> onKeyAction) {
        setStatus("Key: " + name);
        recordAction("Control Key", "key='" + name + "'");
        try {
            onKeyAction.accept(name);
            setStatus(name + " sent");
            recordAction("Control Key", "key='" + name + "'; result='sent'");
        } catch (Exception ex) {
            setStatus("Error: " + ex.getMessage());
            recordAction("Control Key", "key='" + name + "'; error='" + ex.getMessage() + "'");
            error(ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private void recordAction(String action, String details) {
        String time = LocalTime.now().format(TIME_FMT);
        String log = "[" + time + "] " + action + (details != null && !details.isEmpty() ? " | " + details : "");
        actionLogArea.append(log + "\n");
        actionLogArea.setCaretPosition(actionLogArea.getDocument().getLength());
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(BTN_SIZE);
        btn.setFocusable(false);
        return btn;
    }

    private void updateGetTextByCursorEnabled() {
        boolean ok = !rowField.getText().trim().isEmpty()
                  && !colField.getText().trim().isEmpty();
        getTextByPositionButton.setEnabled(ok);
    }

    private Integer parsePositiveInt(String value, String name) {
        try {
            int n = Integer.parseInt(value);
            if (n <= 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            warn(name + " deve ser um número positivo.");
            return null;
        }
    }

    /**
     * Sincroniza o campo de tamanho com o texto atual do campo de texto.
     * Facilita o uso do Get Text By Position após obter a posição.
     */
//    private void syncSizeFromCurrentText() {
//        String text = textField.getText();
//        if (text != null) {
//            sizeField.setText(String.valueOf(calculateFieldSize(text)));
//        }
//    }

//    /**
//     * Calcula o tamanho real do campo descontando 4 colunas da moldura.
//     */
//    private int calculateFieldSize(String text) {
//        if (text == null) return 0;
//        int len = text.length();
//        return Math.max(0, len - 4);
//    }

    private void warn(String msg) {
        setStatus(msg);
        JOptionPane.showMessageDialog(frame, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
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
            areaScreen.setText(Utils.frameText(screen));
            areaScreen.setCaretPosition(0);
        });
    }

    public void setStatus(String message) {
        String text = " [" + LocalTime.now().format(TIME_FMT) + "]  "
                + (message == null ? "" : message) + " ";
        SwingUtilities.invokeLater(() -> statusBar.setText(text));
    }

    public void setContinueEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            continueButton.setEnabled(enabled);
            if (enabled) setStatus("Script paused — click Continue");
        });
    }

    public void close() {
        SwingUtilities.invokeLater(() -> {
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
}
