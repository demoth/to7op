package org.demoth.nogaem.common.messages.fromServer;

import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializable;
import org.demoth.nogaem.server.Player;


/**
 * This structure contains information on entitys' state changes (like position and view direction).
 *
 * @author demoth
 */
@Serializable
public class EntityState {
    public int      id;
    public Vector3f pos;
    public Vector3f view;

    public EntityState() {
        pos = new Vector3f();
        view = new Vector3f();
    }

    public EntityState(int id, Vector3f view, Vector3f position) {
        this.id = id;
        this.pos = position;
        this.view = view;
    }

    public EntityState(Player player) {
        this(player.entity.id, player.entity.state.view, player.physics.getPhysicsLocation());
    }

    @Override
    public String toString() {
        return "EntityState{" +
                "id=" + id +
                ", pos=" + pos +
                ", view=" + view +
                '}';
    }
}
