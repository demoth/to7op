package org.demoth.nogaem.server;

import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.*;
import com.jme3.network.HostedConnection;
import com.jme3.scene.Node;
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
    public String       name;
    public ServerEntity entity;
    // debug
    public float       axeCooldown  = 0f;
    public float       fbCooldown  = 0f;
    public float       respawnTimer = 1;
    public PlayerStats stats        = new PlayerStats();

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

    public boolean isAlive() {
        return stats.hp > 0;
    }

    public boolean isReadyToRespawn() {
        return respawnTimer >= 0;
    }

    public void respawn() {
        respawnTimer = 0; // 5 seconds
        stats.reset();
        physics.setEnabled(true);
        physics.setPhysicsLocation(g_spawn_point);
        axeCooldown = -3f;
        fbCooldown = -3f;
    }

    public void die() {
        entity.state.setPos(new Vector3f(entity.state.getPos()).addLocal(0, -3, 0));
        entity.state.setRot(new Quaternion().fromAngleAxis(FastMath.HALF_PI, new Vector3f(1, 0, 0)));
        physics.setEnabled(false);
        respawnTimer = -5f;
    }

    public void damage(int damage) {
        if (isAlive()) {
            stats.hp -= damage;
            if (stats.hp <= 0)
                die();
        }
    }
}
