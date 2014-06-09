package org.demoth.nogaem.server;

import org.demoth.nogaem.common.entities.Entity;

import java.util.function.Consumer;

/**
 * @author demoth
 */
public class ServerEntity {
    public Entity          entity;
    public Consumer<Float> update;

    public ServerEntity(Entity entity, Consumer<Float> update) {
        this.entity = entity;
        this.update = update;
    }
}
