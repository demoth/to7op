package common.entities;

import common.ClientState;

import java.util.*;

/**
 * Created by daniil on 2/24/14.
 */
public class Player {
    public Integer id;
    public String login;
    public List<ClientState> states = new ArrayList<>(20);

    public Player(int id, String login) {
        this.id = id;
        this.login = login;
    }
}
