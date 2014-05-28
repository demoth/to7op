package org.demoth.nogaem.common.entities;

import com.jme3.math.*;
import com.jme3.network.serializing.Serializable;
import org.demoth.nogaem.common.messages.fromServer.EntityState;

/**
 * @author demoth
 */
@Serializable
public class Entity {
    public int         id;
    public String      modelName;
    public String      name;
    public EntityState state;

    public Entity(int id, String modelName, String name, EntityState state) {
        this.id = id;
        this.modelName = modelName;
        this.name = name;
        this.state = state;
    }

    public Entity() {
        state = new EntityState(id, new Quaternion(), new Vector3f(0, 0, 0));
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id=" + id +
                ", modelName='" + modelName + '\'' +
                ", name='" + name + '\'' +
                ", state=" + state +
                '}';
    }
}
