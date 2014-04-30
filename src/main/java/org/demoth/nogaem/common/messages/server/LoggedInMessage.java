package org.demoth.nogaem.common.messages.server;

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

    public LoggedInMessage(String login, Integer id, String map) {
        this.login = login;
        this.id = id;
        this.map = map;
    }

    @Override
    public String toString() {
        return "LoggedInMessage{" +
                "login='" + login + '\'' +
                ", map='" + map + '\'' +
                ", id=" + id +
                '}';
    }
}