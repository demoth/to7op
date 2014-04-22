package common.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

@Serializable
public class RequestMessage extends AbstractMessage {
    public int playerId;
    public long buttons;
    public Vector3f view;

    public RequestMessage() {
        setReliable(false);
    }

    public RequestMessage(long buttons, Vector3f view) {
        this();
        this.buttons = buttons;
        this.view = view;
    }

    @Override
    public String toString() {
        return "RequestMessage{" +
                "playerId=" + playerId +
                ", buttons=" + buttons +
                ", view=" + view +
                '}';
    }
}
