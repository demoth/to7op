package common.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by demoth on 24.04.14.
 */
@Serializable
public class PlayerJoinedMessage extends AbstractMessage {
    public int      id;
    public String   login;
    public Vector3f location;

    public PlayerJoinedMessage() {
        setReliable(true);
    }

    public PlayerJoinedMessage(int id, String login, Vector3f location) {
        this();
        this.id = id;
        this.login = login;
        this.location = location;
    }
}
