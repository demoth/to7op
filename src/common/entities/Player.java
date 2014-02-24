package common.entities;

import com.jme3.math.Vector2f;

/**
 * Created by daniil on 2/24/14.
 */
public class Player {
    public Integer id;
    public String login;
    public Vector2f position;

    public Player(int id, String login, Vector2f zero) {
        this.id = id;
        this.login = login;
        this.position = zero;
    }
}
