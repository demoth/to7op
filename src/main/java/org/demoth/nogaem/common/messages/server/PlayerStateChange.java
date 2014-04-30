package org.demoth.nogaem.common.messages.server;

import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializable;


/**
 * This structure contains information on players' state changes (like position and view direction).
 * {@link ResponseMessage}
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

    @Override
    public String toString() {
        return "PlayerStateChange{" +
                "playerId=" + playerId +
                ", pos=" + pos +
                ", view=" + view +
                '}';
    }
}
