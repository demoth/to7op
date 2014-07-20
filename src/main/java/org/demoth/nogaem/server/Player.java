package org.demoth.nogaem.server;

import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.*;
import com.jme3.network.HostedConnection;
import com.jme3.scene.Node;
import org.demoth.nogaem.common.entities.*;
import org.demoth.nogaem.common.messages.fromServer.GameStateChange;

import java.util.*;

import static org.demoth.nogaem.common.Config.*;
import static org.demoth.nogaem.common.Config.g_spawn_point;

/**
 * @author demoth
 */
public class Player {

    public HostedConnection conn;
    public final Collection<GameStateChange> notConfirmedMessages = new ArrayList<>();
    public CharacterControl physics;
    public long    lastReceivedMessageIndex = 0;
    public boolean isReady                  = false;
    public String name;
    public ServerEntity entity;
    // debug
    public float axeCooldown = 0f;
    public int   axeQuantity = 3;
    public float hp          = 1;

    public Player(HostedConnection conn, String name) {
        this.conn = conn;
        this.name = name;
    }

    public CharacterControl createPlayerPhysics() {
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(g_player_radius, g_player_height, g_player_axis);
        physics = new CharacterControl(capsuleShape, g_player_step);
        physics.setJumpSpeed(g_player_jumpheight);
        physics.setFallSpeed(g_player_fallspeed);
        physics.setGravity(g_player_gravity);
        physics.setPhysicsLocation(g_spawn_point);
        Node node = new Node("player");
        node.setUserData("entity", entity);
        physics.setSpatial(node);
        return physics;
    }
}
