package org.demoth.nogaem.server;

import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.demoth.nogaem.common.Util;
import org.demoth.nogaem.common.entities.EntityDetailedInfo;
import org.demoth.nogaem.common.entities.EntityInfo;
import org.demoth.nogaem.common.entities.EntityState;

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
        EntityInfo entityInfo = new EntityInfo(id, typeId, detailedInfo.modelName, 2);
        Vector3f position = new Vector3f(pos.add(dir.mult(4f)));
        ServerEntity entity = new ServerEntity(entityInfo, new EntityState(id, rot, position));
        RigidBodyControl physics = new RigidBodyControl(new BoxCollisionShape(new Vector3f(detailedInfo.size, detailedInfo.size, detailedInfo.size)), detailedInfo.mass);
        physics.setPhysicsLocation(position);
        physics.setLinearVelocity(dir.mult(50f));
        physics.setFriction(1f);
        Node node = new Node("missile");
        node.setLocalTranslation(position);
        node.setUserData("entity", entity);
        physics.setSpatial(node);
        bulletAppState.getPhysicsSpace().add(physics);
        float timeToLive = 10f;
        if (typeId == 2) {
            entity.update = tpf -> {
                if (entity.time > timeToLive)
                    server.removeEntity(entityInfo.id, physics);
                entity.time += tpf;
                entity.state.setPos(new Vector3f(physics.getPhysicsLocation()));
                entity.state.setRot(new Quaternion(physics.getPhysicsRotation()));
            };
            entity.touch = e -> {
                entity.removed = true;
                if (e.info.typeId == 1) {
                    server.removeEntity(entityInfo.id, physics);
                    server.getPlayer(e.info.id).damage(4);
                    server.sendHitSound();
                }
            };

        } else if (typeId == 3) {
            physics.setKinematic(true);
            entity.update = tpf -> {
                if (entity.time > timeToLive)
                    server.removeEntity(entityInfo.id, physics);
                entity.time += tpf;
                physics.setPhysicsLocation(physics.getPhysicsLocation().add(dir));
                entity.state.setPos(new Vector3f(physics.getPhysicsLocation()));
                entity.state.setRot(new Quaternion(physics.getPhysicsRotation()));
            };
            entity.touch = e -> {
                entity.removed = true;
                server.removeEntity(entityInfo.id, physics);
                if (e.info.typeId == 1) {
                    server.getPlayer(e.info.id).damage(4);
                    server.sendHitSound();
                }
            };

        }
        return entity;
    }

    public ServerEntity createPlayerEntity(Player player) {
        return new ServerEntity(new EntityInfo(player.conn.getId(), 1, player.name, 0), new EntityState(player.conn.getId()), tpf -> {
            if (player.isAlive()) {
                player.entity.state.setPos(player.physics.getPhysicsLocation());
                player.axeCooldown += tpf;
                player.fbCooldown += tpf;
                if (player.stats.axeCount < player.stats.axeCountMax && player.axeCooldown > 10f) {
                    player.stats.axeCount++;
                    player.axeCooldown = 0f;
                }
                if (player.stats.mp < player.stats.mpMax && player.fbCooldown > 2f) {
                    player.stats.mp++;
                    player.fbCooldown = 0;
                }
            } else {
                player.respawnTimer += tpf;
            }
        });
    }
}