package common.entities;

import common.ClientState;

import java.util.*;

/**
 * Created by daniil on 2/24/14.
 */
public class Player {
    public Integer id;
    public String  login;
    public ClientState lastAck;
    public ClientState currentState;
    public List<ClientState> history = new ArrayList<>(20);

    public Player(int id, String login) {
        this.id = id;
        this.login = login;
        this.currentState = new ClientState(id);
    }
}
