package common.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Sent to server to indicate that client is going to disconnect.
 * Sent to all client to tell them the player with <code>playerId</code> is leaving.
 *
 * @author demoth
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
