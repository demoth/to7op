package org.demoth.nogaem.common.messages.server;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Sent to clients to indicate new player has joined the game.
 *
 * @author demoth
 */
@Serializable
public class NewPlayerJoinedMessage extends AbstractMessage {
    public int      id;
    public String   login;
    public Vector3f location;

    public NewPlayerJoinedMessage() {
        setReliable(true);
    }

    public NewPlayerJoinedMessage(int id, String login, Vector3f location) {
        this();
        this.id = id;
        this.login = login;
        this.location = location;
    }

    @Override
    public String toString() {
        return "NewPlayerJoinedMessage{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", location=" + location +
                '}';
    }
}
