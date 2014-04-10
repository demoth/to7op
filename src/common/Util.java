package common;

import com.jme3.network.serializing.Serializer;
import common.messages.*;

/**
 * Created by daniil on 4/6/14.
 */
public class Util {
    private Util () {}
    public static void registerMessages() {
        Serializer.registerClass(LoginMessage.class);
        Serializer.registerClass(ActionMessage.class);
        Serializer.registerClass(TextMessage.class);
        Serializer.registerClass(ClientStateMessage.class);
        Serializer.registerClass(ClientState.class);
    }
}
