package org.demoth.nogaem.server;

import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.*;
import com.jme3.scene.Node;
import org.demoth.nogaem.common.Util;
import org.demoth.nogaem.common.entities.*;

import java.util.Map;

import static org.demoth.nogaem.common.Config.gamedir;

/**
 * @author demoth
 */
public class ServerEntityFactory {

    private final Map<Integer, EntityDetailedInfo> detailedInfoMap;
    BulletAppState bulletAppState;
    private final ServerMain server;

    public ServerEntityFactory(ServerMain server) {
        this.server = server;
        detailedInfoMap = Util.parseCsv(gamedir + "/entities.csv");

    }

    public ServerEntity create(int id, int typeId, Quaternion rot, Vector3f pos, Vector3f dir) {
        EntityDetailedInfo detailedInfo = detailedInfoMap.get(typeId);
        EntityInfo axeInfo = new EntityInfo(id, typeId, "axe", 2);
        Vector3f position = new Vector3f(pos.add(dir.mult(4f)));
        ServerEntity axe = new ServerEntity(axeInfo, new EntityState(id, rot, position));
        RigidBodyControl control = new RigidBodyControl(new BoxCollisionShape(new Vector3f(detailedInfo.size, detailedInfo.size, detailedInfo.size)), detailedInfo.mass);
        control.setPhysicsLocation(position);
        control.setLinearVelocity(dir.mult(50f));
        control.setFriction(1f);
        Node missile = new Node("missile");
        missile.setLocalTranslation(position);
        missile.setUserData("entity", axe);
        control.setSpatial(missile);
        bulletAppState.getPhysicsSpace().add(control);
        float ttl = 10f;
        axe.update = tpf -> {
            if (axe.time > ttl)
                server.removeEntity(axeInfo.id, control);
            axe.time += tpf;
            axe.state.setPos(new Vector3f(control.getPhysicsLocation()));
            axe.state.setRot(new Quaternion(control.getPhysicsRotation()));
        };
        axe.touch = e -> {
            axe.removed = true;
            System.out.println("touched:");
            if (e.info.typeId == 1) {
                server.removeEntity(axeInfo.id, control);
                server.getPlayer(e.info.id).damage(4);
                server.sendHitSound();
            }
        };
        return axe;
    }

    public ServerEntity createPlayerEntity(Player player) {
        return new ServerEntity(new EntityInfo(player.conn.getId(), 1, player.name, 0), new EntityState(player.conn.getId()), tpf -> {
            if (player.isAlive()) {
                player.entity.state.setPos(player.physics.getPhysicsLocation());
                player.axeCooldown += tpf;
                if (player.stats.axeCount < player.stats.axeCountMax && player.axeCooldown > 10f) {
                    player.stats.axeCount++;
                    player.axeCooldown = 0f;
                }
            } else {
                player.respawnTimer += tpf;
            }
        });
    }
}