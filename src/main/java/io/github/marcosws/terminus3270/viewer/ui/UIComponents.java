package io.github.marcosws.terminus3270.viewer.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;

public abstract class UIComponents {
	
    protected static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    protected static final Dimension BTN_SIZE = new Dimension(158, 30);
	
    protected JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
        return label;
    }
    
    protected JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(BTN_SIZE);
        btn.setFocusable(false);
        return btn;
    }
    
    @FunctionalInterface
    protected interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    @FunctionalInterface
    protected interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

}
