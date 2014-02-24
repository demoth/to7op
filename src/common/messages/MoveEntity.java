package common.messages;

import com.jme3.math.Vector2f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

@Serializable
public class MoveEntity extends AbstractMessage {
    public Integer id;
    public Vector2f delta;

    public MoveEntity() {
    }

    public MoveEntity(Integer id, Vector2f delta) {
        this.id = id;
        this.delta = delta;
    }
}
