package org.demoth.nogaem.common.messages.fromClient;

import com.jme3.math.*;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Sent each frame to server by all clients to indicate their actions.
 *
 * @author demoth
 */
@Serializable
public class ActionMessage extends AbstractMessage {
    public int        playerId;
    public long       buttons;
    public Vector3f   dir;
    public Quaternion rot;

    public ActionMessage(long buttons, Vector3f direction, Quaternion rotation) {
        this.buttons = buttons;
        this.dir = direction;
        this.rot = rotation;
    }

    public ActionMessage() {
    }

    @Override
    public String toString() {
        return "ActionMessage{" +
                "playerId=" + playerId +
                ", buttons=" + buttons +
                ", dir=" + dir +
                ", rot=" + rot +
                '}';
    }
}
