package org.demoth.nogaem.server;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.*;
import com.jme3.network.HostedConnection;
import org.demoth.nogaem.common.entities.EntityInfo;
import org.demoth.nogaem.common.messages.fromServer.*;

import java.util.*;

/**
 * @author demoth
 */
public class Player {

    public HostedConnection conn;
    public final Collection<GameStateChange> notConfirmedMessages = new ArrayList<>();
    public CharacterControl physics;
    public long    lastReceivedMessageIndex = 0;
    public boolean isReady                  = false;
    public EntityInfo  info;
    public EntityState state;

    // debug
    public int projectileEffect;

    public Player(HostedConnection conn, String name, CharacterControl physics) {
        info = new EntityInfo(conn.getId(), 1, name, 0);
        state = new EntityState(conn.getId(), new Quaternion(), new Vector3f());
        this.conn = conn;
        this.physics = physics;
    }

    public Player() {
    }
}
