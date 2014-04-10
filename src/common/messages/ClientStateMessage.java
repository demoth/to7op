package common.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

import java.util.Date;

@Serializable
public class ClientStateMessage extends AbstractMessage {
    public Date date;

    public ClientStateMessage() {
        setReliable(false);
    }

    public ClientStateMessage(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "ClientStateMessage{" +
                "date=" + date +
                '}';
    }
}
