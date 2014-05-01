package org.demoth.nogaem.common.messages.server;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * @author demoth
 */
@Serializable
public class ChangeMapMessage extends AbstractMessage {

    private String mapName;

    public ChangeMapMessage() {
        setReliable(true);
    }

    public ChangeMapMessage(String mapName) {
        this();
        this.mapName = mapName;
    }
}
