package org.demoth.nogaem.common.messages.fromServer;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import org.demoth.nogaem.common.entities.Entity;

import java.util.List;

/**
 * @author demoth
 */
@Serializable
public class AddedEntities extends AbstractMessage {
    public List<Entity> added;

    @Override
    public String toString() {
        return "AddedEntities size:" + (added == null ? "0" : added.size());
    }
}
