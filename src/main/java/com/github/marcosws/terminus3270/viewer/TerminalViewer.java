package com.github.marcosws.terminus3270.viewer;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class TerminalViewer {

    private JFrame frame;
    private JTextArea areaScreen;
    private JLabel statusBar;

    private JButton continueButton;
    private JButton copyButton;

    private JTextField fieldNameField;
    private JTextField textToSendField;
    private JComboBox<String> directionCombo;
    private JButton sendTextButton;
    private JButton getTextButton;

    private String lastScreen = "";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * @param onContinue   Continue
     * @param onSendText   (field, text, direction)
     * @param onGetText    (field, size, direction) → valor
     * @param onKeyAction  nome da tecla: "Enter", "Tab", "PF1", "PA2", etc.
     */
    public TerminalViewer(Runnable onContinue,
                          TriConsumer<String, String, String> onSendText,
                          TriFunction<String, Integer, String, String> onGetText,
                          Consumer<String> onKeyAction) {

        frame = new JFrame("Terminal 3270 Viewer");

        // ===== Tela 3270 =====
        areaScreen = new JTextArea(28, 84);
        areaScreen.setFont(new Font("Monospaced", Font.PLAIN, 14));
        areaScreen.setEditable(false);
        areaScreen.setFocusable(false);
        areaScreen.setBackground(Color.BLACK);
        areaScreen.setForeground(Color.GREEN);

        // ===== Status Bar =====
        statusBar = new JLabel(" Ready ");
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusBar.setOpaque(true);
        statusBar.setBackground(new Color(240, 240, 240));

        // ===== Topo =====
        continueButton = new JButton("Continue");
        continueButton.setEnabled(false);
        continueButton.addActionListener(e -> onContinue.run());

        copyButton = new JButton("Copy Screen");
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(areaScreen.getText());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
            setStatus("Screen copied to clipboard");
        });

        fieldNameField = new JTextField(12);
        textToSendField = new JTextField(15);
        directionCombo = new JComboBox<>(new String[]{
                "Right Field", "Left Field", "Top Field", "Botton Field"
        });

        sendTextButton = new JButton("Send Text");
        sendTextButton.addActionListener(e -> {
            String field = fieldNameField.getText().trim();
            String text = textToSendField.getText();
            String direction = (String) directionCombo.getSelectedItem();
            if (field.isEmpty()) {
                setStatus("Informe o nome do campo");
                JOptionPane.showMessageDialog(frame, "Informe o nome do campo.",
                        "Campo obrigatório", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                setStatus("Sending to '" + field + "'...");
                onSendText.accept(field, text, direction);
                setStatus("Text sent to '" + field + "'");
            } catch (Exception ex) {
                setStatus("Error: " + ex.getMessage());
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        getTextButton = new JButton("Get Text");
        getTextButton.addActionListener(e -> {
            String field = fieldNameField.getText().trim();
            String sizeStr = textToSendField.getText().trim();
            String direction = (String) directionCombo.getSelectedItem();
            if (field.isEmpty()) {
                setStatus("Informe o nome do campo");
                return;
            }
            int size;
            try {
                size = Integer.parseInt(sizeStr);
                if (size <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                setStatus("Tamanho inválido");
                JOptionPane.showMessageDialog(frame, "Informe o tamanho (número positivo).",
                        "Tamanho inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                setStatus("Reading '" + field + "'...");
                String value = onGetText.apply(field, size, direction);
                textToSendField.setText(value);
                setStatus("Got: \"" + value + "\"");
            } catch (Exception ex) {
                setStatus("Error: " + ex.getMessage());
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        topPanel.add(continueButton);
        topPanel.add(copyButton);
        topPanel.add(Box.createHorizontalStrut(8));
        topPanel.add(new JLabel("Field:"));
        topPanel.add(fieldNameField);
        topPanel.add(new JLabel("Text/Size:"));
        topPanel.add(textToSendField);
        topPanel.add(directionCombo);
        topPanel.add(sendTextButton);
        topPanel.add(getTextButton);

        // ===== Painel lateral de teclas =====
        JPanel keysPanel = createKeysPanel(onKeyAction);

        // ===== Montagem =====
        JScrollPane screenScroll = new JScrollPane(areaScreen);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(screenScroll, BorderLayout.CENTER);
        centerPanel.add(keysPanel, BorderLayout.EAST);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(statusBar, BorderLayout.SOUTH);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setSize(960, 700);
        frame.setResizable(false);
    }

    // -------------------------------------------------------------------------
    // Painel de teclas (direita)
    // -------------------------------------------------------------------------
    private JPanel createKeysPanel(Consumer<String> onKeyAction) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        panel.setPreferredSize(new Dimension(160, 0));

        // --- Teclas comuns ---
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

        // --- PF1..PF24 (grid 3 colunas) ---
        panel.add(sectionLabel("PF Keys"));
        JPanel pfGrid = new JPanel(new GridLayout(0, 3, 3, 3));
        for (int i = 1; i <= 24; i++) {
            pfGrid.add(smallKeyButton("PF" + i, onKeyAction));
        }
        panel.add(pfGrid);
        panel.add(Box.createVerticalStrut(8));

        // --- PA1..PA3 ---
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
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
        return label;
    }

    private JButton keyButton(String name, Consumer<String> onKeyAction) {
        JButton btn = new JButton(name);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        btn.setFocusable(false);
        btn.addActionListener(e -> {
            setStatus("Key: " + name);
            try {
                onKeyAction.accept(name);
            } catch (Exception ex) {
                setStatus("Error: " + ex.getMessage());
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return btn;
    }

    private JButton smallKeyButton(String name, Consumer<String> onKeyAction) {
        JButton btn = new JButton(name);
        btn.setMargin(new Insets(2, 2, 2, 2));
        btn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        btn.setFocusable(false);
        btn.addActionListener(e -> {
            setStatus("Key: " + name);
            try {
                onKeyAction.accept(name);
            } catch (Exception ex) {
                setStatus("Error: " + ex.getMessage());
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return btn;
    }

    // ===== Interfaces =====
    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    // ===== Métodos públicos =====
    public void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public void updateScreen(String screen) {
        if (screen == null || screen.equals(lastScreen)) return;
        lastScreen = screen;
        SwingUtilities.invokeLater(() -> {
            areaScreen.setText(screen);
            areaScreen.setCaretPosition(0);
        });
    }

    public void setStatus(String message) {
        if (message == null) message = "";
        String text = " [" + LocalTime.now().format(TIME_FMT) + "]  " + message + " ";
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
}