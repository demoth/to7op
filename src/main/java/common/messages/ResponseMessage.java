package common.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

@Serializable
public class ResponseMessage extends AbstractMessage {
    public Vector3f position;

    public ResponseMessage() {
        setReliable(false);
    }

    public ResponseMessage(Vector3f position) {
        this();
        this.position = position;
    }

    @Override
    public String toString() {
        return "ResponseMessage{" +
                "position=" + position +
                '}';
    }
}
