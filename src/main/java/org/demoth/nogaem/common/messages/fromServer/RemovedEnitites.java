package org.demoth.nogaem.common.messages.fromServer;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

import java.util.List;

/**
 * @author demoth
 */
@Serializable
public class RemovedEnitites extends AbstractMessage {
    public List<Integer> removedIds;
}
