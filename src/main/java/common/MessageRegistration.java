package common;

import com.jme3.network.serializing.*;
import common.messages.*;

/**
 * Created by daniil on 4/6/14.
 */
public class MessageRegistration {
    private MessageRegistration() {}
    public static void registerAll() {
        Serializer.registerClass(DisconnectMessage.class);
        Serializer.registerClass(LoginMessage.class);
        Serializer.registerClass(TextMessage.class);
        Serializer.registerClass(RequestMessage.class);
        Serializer.registerClass(ResponseMessage.class);
        Serializer.registerClass(PlayerStateChange.class);
    }
}
