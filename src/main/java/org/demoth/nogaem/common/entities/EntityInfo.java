package org.demoth.nogaem.common.entities;

import com.jme3.network.serializing.Serializable;

/**
 * @author demoth
 */
@Serializable
public class EntityInfo {
    public int id;
    public int typeId;
    public String name;
    public long effects = 0;

    public EntityInfo() {
    }

    public EntityInfo(int id, int typeId, String name, long effects) {
        this.id = id;
        this.typeId = typeId;
        this.name = name;
        this.effects = effects;
    }

    @Override
    public String toString() {
        return "EntityInfo{" +
                "id=" + id +
                ", typeId=" + typeId +
                ", name='" + name + '\'' +
                ", effects=" + effects +
                '}';
    }
}