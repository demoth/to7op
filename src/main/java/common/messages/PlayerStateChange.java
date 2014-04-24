package common.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializable;


/**
 * Created by demoth on 22.04.14.
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
