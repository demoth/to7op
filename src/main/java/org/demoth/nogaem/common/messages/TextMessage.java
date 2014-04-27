package org.demoth.nogaem.common.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Simple text message.
 *
 * @author demoth
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

    @Override
    public String toString() {
        return "TextMessage{" +
                "text='" + text + '\'' +
                '}';
    }
}
