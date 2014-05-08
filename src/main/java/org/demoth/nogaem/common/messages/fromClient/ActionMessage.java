package org.demoth.nogaem.common.messages.fromClient;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Sent each frame to server by all clients to indicate their actions.
 *
 * @author demoth
 */
@Serializable
public class ActionMessage extends AbstractMessage {
    public int      playerId;
    public long     buttons;
    public Vector3f view;

    public ActionMessage(long buttons, Vector3f direction) {
        this.buttons = buttons;
        this.view = direction;
    }

    public ActionMessage() {
    }

    @Override
    public String toString() {
        return "ActionMessage{" +
                "playerId=" + playerId +
                ", buttons=" + buttons +
                ", view=" + view +
                '}';
    }
}
