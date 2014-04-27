package org.demoth.nogaem.common.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Sent to clients to indicate new player has joined the game.
 *
 * @author demoth
 */
@Serializable
public class PlayerJoinedMessage extends AbstractMessage {
    public int      id;
    public String   login;
    public Vector3f location;

    public PlayerJoinedMessage() {
        setReliable(true);
    }

    public PlayerJoinedMessage(int id, String login, Vector3f location) {
        this();
        this.id = id;
        this.login = login;
        this.location = location;
    }
}
