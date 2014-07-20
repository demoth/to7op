package org.demoth.nogaem.server;

import com.jme3.bullet.control.RigidBodyControl;

/**
 * Created by demoth on 17.07.14.
 */
public interface ServerMain {
    void removeEntity(int id, RigidBodyControl control);

    Player getPlayer(int id);

    void sendHitSound();
}
