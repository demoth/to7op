package common.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Sent to client to indicate that credentials are ok and player is now in game on server
 * (with fields map - map name, id - player's id).
 *
 * @author demoth
 */
@Serializable
public class LoggedInMessage extends AbstractMessage{
    public String login;
    public String map;
    public Integer id;

    public LoggedInMessage() {
        setReliable(true);
    }

    public LoggedInMessage(String login, String password, Integer id, long startTime, String map) {
        this.login = login;
        this.id = id;
        this.map = map;
    }
}