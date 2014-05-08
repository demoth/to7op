package org.demoth.nogaem.common.messages.fromClient;

import com.jme3.network.AbstractMessage;

/**
 * @author demoth
 *         <p>
 *         Client only (is not sent)
 */
public class CommandMessage extends AbstractMessage {
    public final String cmd;

    public CommandMessage(String cmd) {
        this.cmd = cmd;
    }
}
