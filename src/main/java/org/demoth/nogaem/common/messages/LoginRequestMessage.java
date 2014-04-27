package org.demoth.nogaem.common.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Sent to server to indicate that player wants to join.
 *
 * @author demoth
 */

@Serializable
public class LoginRequestMessage extends AbstractMessage {
    public String login;
    public String password;

    public LoginRequestMessage(String login, String password) {
        this();
        this.login = login;
        this.password = password;
    }

    public LoginRequestMessage() {
        setReliable(true);
    }
}
