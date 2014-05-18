package org.demoth.nogaem.server;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.network.HostedConnection;
import org.demoth.nogaem.common.entities.Entity;
import org.demoth.nogaem.common.messages.fromServer.GameStateChange;

import java.util.*;

/**
 * @author demoth
 */
public class Player extends Entity {

    public final HostedConnection conn;
    public final Collection<GameStateChange> notConfirmedMessages = new ArrayList<>();
    public CharacterControl physics;
    public long lastReceivedMessageIndex = 0;
    public boolean isReady = false;

    public Player(HostedConnection conn, String name, CharacterControl physics) {
        super();
        this.id = conn.getId();
        this.state.id = conn.getId();
        this.conn = conn;
        this.name = name;
        this.physics = physics;
    }

}
