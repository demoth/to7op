package org.demoth.nogaem.common.entities;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializable;


/**
 * This structure contains information on entitys' state changes (like position and view direction).
 *
 * @author demoth
 */
@Serializable
public class EntityState {
    public int id;
    private Vector3f pos;
    private Quaternion rot;

    private boolean isDirty;

    public EntityState() {
    }

    public EntityState(int id, Quaternion rot, Vector3f position) {
        this.id = id;
        this.rot = new Quaternion(rot);
        this.pos = new Vector3f(position);
        isDirty = true;
    }

    public EntityState(int id) {
        this(id, new Quaternion(), new Vector3f());
    }

    public Vector3f getPos() {
        return pos;
    }

    public void setPos(Vector3f pos) {
        if (!pos.equals(this.pos)) {
            this.pos = pos;
            isDirty = true;
        }
    }

    public Quaternion getRot() {
        return rot;
    }

    public void setRot(Quaternion rot) {
        if (!rot.equals(this.rot)) {
            this.rot = rot;
            this.isDirty = true;
        }
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void clean() {
        isDirty = false;
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
