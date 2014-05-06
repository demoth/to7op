package org.demoth.nogaem.tests;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.util.StatusPrinter;
import org.demoth.nogaem.client.swing.SwingConsole;
import org.demoth.nogaem.client.swing.SwingConsoleAppender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author demoth
 */
@RunWith(JUnit4.class)
public class TestAppender {

    @Test
    public void test() {
        Logger logger = LoggerFactory.getLogger(TestAppender.class);
        logger.info("logging without swing console");

        SwingConsole console = new SwingConsole(logger::info);
        console.setVisible(true);
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        SwingConsoleAppender<ILoggingEvent> appender = new SwingConsoleAppender<>(console);
        appender.setContext(lc);
        appender.start();
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ((ch.qos.logback.classic.Logger) logger).addAppender(appender);
        root.addAppender(appender);

        StatusPrinter.print(lc);


    }

    public static void main(String[] args) {
        new TestAppender().test();
    }
}
