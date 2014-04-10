package common.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

import java.util.Date;

/**
 * Created by daniil on 2/28/14.
 */
@Serializable
public class ActionMessage extends AbstractMessage {
    public Date date;

    public ActionMessage() {
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
