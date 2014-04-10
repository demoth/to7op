package common.entities;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.network.HostedConnection;
import common.messages.ClientStateMessage;

import java.util.*;

/**
 * Created by daniil on 2/24/14.
 */
public class Player {
    public long        startTime;
    public Integer     id;
    public String      login;
    public ClientStateMessage currentState;
    public CharacterControl control;
    public HostedConnection conn;

    public Player(int id, String login, long startTime) {
        this.id = id;
        this.login = login;
        this.currentState = new ClientStateMessage();
        this.startTime = startTime;
    }
}
