package org.demoth.nogaem.common.messages.fromClient;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * @author demoth
 */
@Serializable
public class Acknowledgement extends AbstractMessage {
    // special value -1 indicates that player is ready
    public long index;

    public Acknowledgement() {
    }

    public Acknowledgement(long index) {
        this.index = index;
    }
}
