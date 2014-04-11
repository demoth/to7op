package common.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

import java.util.Date;

@Serializable
public class ResponseMessage extends AbstractMessage {
    public Date date;

    public ResponseMessage() {
        setReliable(false);
    }

    public ResponseMessage(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "ResponseMessage{" +
                "date=" + date +
                '}';
    }
}
