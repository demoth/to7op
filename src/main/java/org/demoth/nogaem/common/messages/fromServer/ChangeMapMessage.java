package org.demoth.nogaem.common.messages.fromServer;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * @author demoth
 */
@Serializable
public class ChangeMapMessage extends AbstractMessage {
    public String mapName;

    public ChangeMapMessage(String mapName) {
        this();
        this.mapName = mapName;
    }

    public ChangeMapMessage() {
        setReliable(true);
    }

    @Override
    public String toString() {
        return "ChangeMapMessage{" +
                "mapName='" + mapName + '\'' +
                '}';
    }
}
