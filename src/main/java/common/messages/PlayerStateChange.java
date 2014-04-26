package common.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializable;


/**
 * This structure contains information on players' state changes (like position and view direction).
 * {@link common.messages.ResponseMessage}
 *
 * @author demoth
 */
@Serializable
public class PlayerStateChange {
    public  int      playerId;
    public  Vector3f pos;
    public Vector3f view;

    public PlayerStateChange() {
    }

    public PlayerStateChange(int playerId, Vector3f view, Vector3f position) {
        this.playerId = playerId;
        this.pos = position;
        this.view = view;
    }
}
