package common.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by daniil on 2/28/14.
 */
@Serializable
public class TextMessage extends AbstractMessage {
    public String text;

    public TextMessage() {
    }

    public TextMessage(String text) {
        this.setReliable(true);
        this.text = text;
    }
}
