package org.demoth.nogaem.server;

import org.demoth.nogaem.common.entities.*;

import java.util.function.Consumer;

/**
 * @author demoth
 */
public class ServerEntity {
    // static entity information
    public EntityInfo      info;
    // dynamic entity information
    public EntityState     state;
    // update function
    public Consumer<Float> update;

    public float time;

    public ServerEntity(EntityInfo info, EntityState state, Consumer<Float> update) {
        this.info = info;
        this.state = state;
        this.update = update;
    }

    public ServerEntity(EntityInfo info, EntityState state) {
        this.info = info;
        this.state = state;
    }

    public ServerEntity() {
    }

}
