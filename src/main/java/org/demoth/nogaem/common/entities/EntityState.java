package org.demoth.nogaem.common.entities;

import com.jme3.math.*;
import com.jme3.network.serializing.Serializable;


/**
 * This structure contains information on entitys' state changes (like position and view direction).
 *
 * @author demoth
 */
@Serializable
public class EntityState {
    public int        id;
    public Vector3f   pos;
    public Quaternion rot;

    public EntityState() {
        pos = new Vector3f();
        rot = new Quaternion();
    }

    public EntityState(int id, Quaternion rot, Vector3f position) {
        this.id = id;
        this.rot = new Quaternion(rot);
        this.pos = new Vector3f(position);
    }

    public EntityState(int id) {
        this(id, new Quaternion(), new Vector3f());
    }

    @Override
    public String toString() {
        return "EntityState{" +
                "id=" + id +
                ", pos=" + pos +
                ", rot=" + rot +
                '}';
    }
}
