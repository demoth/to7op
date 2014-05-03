package org.demoth.nogaem.client;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            sendAction.execCommand(field.getText());
            field.setText("");
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
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%logger{0} - %msg%n");
        ple.setContext(lc);
        ple.start();
        SwingConsoleAppender<ILoggingEvent> appender = new SwingConsoleAppender<>(this);
        appender.setContext(lc);
        appender.start();
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);

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
