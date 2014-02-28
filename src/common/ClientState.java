package common;

import com.jme3.math.Vector3f;
import java.io.Serializable;

public class ClientState implements Serializable {
    public int entityId;
    public long time;
    public Vector3f view;
    public Vector3f position;
    public Vector3f speed;
    public long buttons;
}
