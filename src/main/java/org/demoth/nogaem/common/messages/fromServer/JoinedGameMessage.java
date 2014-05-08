package org.demoth.nogaem.common.messages.fromServer;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Sent to client to indicate that credentials are ok and player is now in game on server
 * (with fields map - map name, id - player's id).
 *
 * @author demoth
 */
@Serializable
public class JoinedGameMessage extends AbstractMessage {

    public String  map;
    public Integer id;

    public JoinedGameMessage() {
        setReliable(true);
    }

    public JoinedGameMessage(int id, String map) {
        this();
        this.id = id;
        this.map = map;
    }
}