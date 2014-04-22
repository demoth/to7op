package common.entities;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.network.HostedConnection;
import common.messages.ResponseMessage;

/**
 * Created by daniil on 2/24/14.
 */
public class Player {
    public long        startTime;
    public Integer     id;
    public String      login;
    public ResponseMessage currentState;
    public CharacterControl control;
    public HostedConnection conn;

    public Player(int id, String login, long startTime) {
        this.id = id;
        this.login = login;
        this.currentState = new ResponseMessage();
        this.startTime = startTime;
    }
}