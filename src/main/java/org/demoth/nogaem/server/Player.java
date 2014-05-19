package org.demoth.nogaem.server;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.network.HostedConnection;
import com.jme3.network.serializing.Serializable;
import org.demoth.nogaem.common.entities.Entity;
import org.demoth.nogaem.common.messages.fromServer.GameStateChange;

import java.util.*;

/**
 * @author demoth
 */
public class Player {

    public HostedConnection conn;
    public final Collection<GameStateChange> notConfirmedMessages = new ArrayList<>();
    public CharacterControl physics;
    public long lastReceivedMessageIndex = 0;
    public boolean isReady = false;
    public Entity entity;

    public Player(HostedConnection conn, String name, CharacterControl physics) {
        entity = new Entity();
        entity.modelName = "ninja";
        entity.id = conn.getId();
        entity.state.id = conn.getId();
        entity.name = name;
        this.conn = conn;
        this.physics = physics;
    }

    public Player() {
    }
}
