package common.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by daniil on 2/24/14.
 */
@Serializable
public class LoginMessage extends AbstractMessage{
    public String login;
    public String password;
    public Integer id;

    public LoginMessage() {
        setReliable(true);
    }
    public LoginMessage(String login, String password, Integer id) {
        this.login = login;
        this.password = password;
        this.id = id;
    }
}
