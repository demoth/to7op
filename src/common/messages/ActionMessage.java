package common.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

import java.util.Date;

@Serializable
public class ActionMessage extends AbstractMessage {
    public Date date;

    public ActionMessage() {
        setReliable(false);
    }

    public ActionMessage(Date date) {
        this.date = date;
    }

    public ActionMessage(Vector3f view, long buttons, long lastAckMessageTime) {
    }

    @Override
    public String toString() {
        return "ActionMessage{" +
                "date=" + date +
                '}';
    }
}
