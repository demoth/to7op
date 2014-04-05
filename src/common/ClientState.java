package common;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;
import java.io.Serializable;

public class ClientState implements Serializable {
    public int      entityId;
    public long     time;
    public Vector3f view;
    public Vector3f position;
    public Vector3f speed;
    public boolean  acknowledged;

    public ClientState(CharacterControl control) {
        view = control.getViewDirection();
        position = control.getPhysicsLocation();
        speed = new Vector3f(0f, 0f, 0f);
    }

    public ClientState diff(ClientState other) {
        return new ClientState(
                other.entityId,
                this.speed.subtract(other.speed),
                this.position.subtract(other.position),
                this.view.subtract(other.view));
    }

    public ClientState(int entityId, Vector3f view, Vector3f speed, Vector3f position) {
        this.entityId = entityId;
        this.view = view;
        this.speed = speed;
        this.position = position;
    }

    public ClientState(int entityId) {
        this.entityId = entityId;
        this.time = System.currentTimeMillis();
        this.view = new Vector3f();
        this.position = new Vector3f();
        this.speed = new Vector3f();
        this.acknowledged = false;
    }
}
