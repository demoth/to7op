package org.demoth.nogaem.server;

import com.jme3.export.*;
import org.demoth.nogaem.common.entities.*;

import java.io.IOException;
import java.util.function.*;

/**
 * @author demoth
 */
public class ServerEntity implements Savable {
    // static entity information
    public EntityInfo             info;
    // dynamic entity information
    public EntityState            state;
    // update function
    public Consumer<Float>        update;
    // touch function
    public Consumer<ServerEntity> touch;

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

    @Override
    public void write(JmeExporter jmeExporter) throws IOException {

    }

    @Override
    public void read(JmeImporter jmeImporter) throws IOException {

    }
}
