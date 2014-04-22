package common.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

import java.util.*;

@Serializable
public class ResponseMessage extends AbstractMessage {
    public List<PlayerStateChange> changes;

    public ResponseMessage() {
        setReliable(false);
    }

    public ResponseMessage(List<PlayerStateChange> changes) {
        this.changes = changes;
    }
}
