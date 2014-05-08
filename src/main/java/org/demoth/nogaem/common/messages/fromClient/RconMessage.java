package org.demoth.nogaem.common.messages.fromClient;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import org.demoth.nogaem.common.RconCommand;

/**
 * @author demoth
 */
@Serializable
public class RconMessage extends AbstractMessage {
    public RconCommand command;
    public String      args;
    public String      password;

    public RconMessage() {
        setReliable(true);
    }

    public RconMessage(RconCommand command, String args, String password) {
        this();
        this.command = command;
        this.password = password;
        this.args = args;
    }

    @Override
    public String toString() {
        return "RconMessage{" +
                "command=" + command +
                ", args='" + args + '\'' +
                '}';
    }
}
