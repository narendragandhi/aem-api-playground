package com.aemtools.aem.gui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

public class ConsolePanel extends JPanel {

    private JTextPane consoleArea;

    public ConsolePanel() {
        setLayout(new BorderLayout());
        consoleArea = new JTextPane();
        consoleArea.setEditable(false);
        consoleArea.setBackground(new Color(20, 20, 20));
        consoleArea.setForeground(Color.LIGHT_GRAY);
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(consoleArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(null, "Developer Console", 0, 0, null, Color.GRAY));
        add(scrollPane, BorderLayout.CENTER);
        
        redirectSystemStreams();
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                updateConsole(String.valueOf((char) b), Color.LIGHT_GRAY);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                updateConsole(new String(b, off, len), Color.LIGHT_GRAY);
            }
        };

        OutputStream err = new OutputStream() {
            @Override
            public void write(int b) {
                updateConsole(String.valueOf((char) b), Color.RED);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                updateConsole(new String(b, off, len), Color.RED);
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(err, true));
    }

    private void updateConsole(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = consoleArea.getStyledDocument();
            Style style = consoleArea.addStyle("ColorStyle", null);
            StyleConstants.setForeground(style, color);
            try {
                doc.insertString(doc.getLength(), text, style);
                consoleArea.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                // Ignore
            }
        });
    }
}
