package common.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import common.ClientState;

@Serializable
public class ClientStateMessage extends AbstractMessage {
    public ClientState diff;

    public ClientStateMessage() {
    }

    public ClientStateMessage(ClientState diff) {
        this.diff = diff;
    }
}
