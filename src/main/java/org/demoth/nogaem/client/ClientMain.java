package org.demoth.nogaem.client;

/**
 * @author demoth
 */
public interface ClientMain {
    void pushButton(String actionName, boolean pressed, float tpf);

    void enqueue(String cmd);

    void enableIngameState(boolean enable);

    boolean isConnected();
}
