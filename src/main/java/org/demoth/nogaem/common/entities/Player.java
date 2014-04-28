package org.demoth.nogaem.common.entities;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.network.HostedConnection;
import org.demoth.nogaem.common.messages.server.ResponseMessage;

/**
 * Created by daniil on 2/24/14.
 */
public class Player {
    public long             startTime;
    public Integer          id;
    public String           login;
    public ResponseMessage  currentState;
    public CharacterControl control;
    public HostedConnection conn;
    public Vector3f view = new Vector3f();

    public Player(int id, String login) {
        this.id = id;
        this.login = login;
        this.currentState = new ResponseMessage();
    }
}
