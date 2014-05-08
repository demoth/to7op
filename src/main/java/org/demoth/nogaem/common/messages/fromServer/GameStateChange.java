package org.demoth.nogaem.common.messages.fromServer;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import org.demoth.nogaem.common.entities.Entity;

import java.util.Collection;

/**
 * @author demoth
 */
@Serializable
public class GameStateChange extends AbstractMessage {
    public long                    index;
    public Collection<EntityState> changes;
    public Collection<Entity>      added;
    public Collection<Integer>     removedIds;

    @Override
    public String toString() {
        return "GameStateChange{" +
                "index=" + index +
                ", changes=" + (changes == null ? "0" : changes.size()) +
                ", added=" + (added == null ? "0" : added.size()) +
                ", removedIds=" + (removedIds == null ? "0" : removedIds.size()) +
                '}';
    }
}
