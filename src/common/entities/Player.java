package common.entities;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.network.HostedConnection;
import common.ClientState;

import java.util.*;

/**
 * Created by daniil on 2/24/14.
 */
public class Player {
    public long        startTime;
    public Integer     id;
    public String      login;
    public ClientState lastAck;
    public ClientState currentState;
    public List<ClientState> history = new ArrayList<>(20);
    public CharacterControl control;
    public HostedConnection conn;

    public Player(int id, String login, long startTime) {
        this.id = id;
        this.login = login;
        this.currentState = new ClientState(id);
        this.startTime = startTime;
    }
}
