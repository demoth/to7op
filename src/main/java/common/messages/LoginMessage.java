package common.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Sent to server to indicate that player wants to join (with fields login & password).
 * Sent to client to indicate that credentials are ok and player is now in game on server
 * (with fields map - map name, id - player's id).
 *
 * @author demoth
 */
@Serializable
public class LoginMessage extends AbstractMessage{
    public String login;
    public String password;
    public String map;
    public Integer id;
    public long startTime;

    public LoginMessage() {
        setReliable(true);
    }

    public LoginMessage(String login, String password, Integer id, long startTime, String map) {
        this.login = login;
        this.password = password;
        this.id = id;
        this.startTime = startTime;
        this.map = map;
    }

    @Override
    public String toString() {
        return "LoginMessage{" +
                "login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", map='" + map + '\'' +
                ", id=" + id +
                ", startTime=" + startTime +
                '}';
    }
}
