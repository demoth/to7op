package common.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

import java.util.Date;

@Serializable
public class RequestMessage extends AbstractMessage {
    public Date date;

    public RequestMessage() {
        setReliable(false);
    }

    public RequestMessage(Date date) {
        this.date = date;
    }

    public RequestMessage(Vector3f view, long buttons, long lastAckMessageTime) {
    }

    @Override
    public String toString() {
        return "RequestMessage{" +
                "date=" + date +
                '}';
    }
}
