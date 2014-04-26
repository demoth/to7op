package common.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by demoth on 22.04.14.
 */
@Serializable
public class DisconnectMessage extends AbstractMessage {
    public int playerId;

    public DisconnectMessage(int playerId) {
        this();
        this.playerId = playerId;
    }

    public DisconnectMessage() {
        setReliable(true);
    }
}
