package org.demoth.nogaem.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @author demoth
 */
public class SwingConsole extends JFrame {
    private final JTextArea  area;
    private final JTextField field;

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SwingConsole(Action sendAction) {
        super("Nogaem console");

        setLayout(new BorderLayout());
        area = new JTextArea();
        area.setEditable(false);
        add(new JScrollPane(area), BorderLayout.CENTER);

        field = new JTextField();
        field.addActionListener(e -> {
            String cmd = field.getText();
            area.append(cmd);
            area.append("\n");
            field.setText("");
            sendAction.execCommand(cmd);
        });
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F1)
                    SwingUtilities.invokeLater(() -> SwingConsole.this.setVisible(false));
            }
        });
        add(field, BorderLayout.SOUTH);
        setSize(640, 480);
    }

    public void print(String s) {
        SwingUtilities.invokeLater(() -> area.append(s + '\n'));
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        field.requestFocusInWindow();
    }

    public static interface Action {
        void execCommand(String cmd);
    }
}
