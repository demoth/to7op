package org.demoth.nogaem.client;

import ch.qos.logback.core.AppenderBase;

/**
 * @author demoth
 */
public class SwingConsoleAppender<E> extends AppenderBase<E> {

    private final SwingConsole console;

    public SwingConsoleAppender(SwingConsole console) {
        this.setName("swing console");
        this.console = console;
    }

    @Override
    protected void append(E eventObject) {
        console.print(eventObject.toString());
    }
}
