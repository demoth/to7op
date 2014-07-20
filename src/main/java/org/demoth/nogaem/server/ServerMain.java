package org.demoth.nogaem.server;

import com.jme3.bullet.control.RigidBodyControl;

/**
 * @author demoth
 */
public interface ServerMain {
    void removeEntity(int id, RigidBodyControl control);

    Player getPlayer(int id);

    void sendHitSound();
}
