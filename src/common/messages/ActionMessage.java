package common.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by daniil on 2/28/14.
 */
@Serializable
public class ActionMessage extends AbstractMessage {
    public long     lastAckMessageTime;
    public Vector3f view;
    public long     buttons;

    public ActionMessage() {
    }

    public ActionMessage(Vector3f view, long buttons, long lastAckMessageTime) {
        this.view = view;
        this.buttons = buttons;
        this.lastAckMessageTime = lastAckMessageTime;
    }
}
