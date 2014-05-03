package org.demoth.nogaem.common.messages.client;

import com.jme3.network.*;

/**
 * Created by demoth on 03.05.14.
 */
public class CommandMessage extends AbstractMessage {
    public final String cmd;

    public CommandMessage(String cmd) {
        this.cmd = cmd;
    }
}
