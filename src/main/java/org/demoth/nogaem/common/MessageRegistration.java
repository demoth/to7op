package org.demoth.nogaem.common;

import com.jme3.network.serializing.Serializer;
import org.demoth.nogaem.common.messages.*;

/**
 * Util class to keep the make registration the same on both client & server.
 *
 * @author demoth
 */
public class MessageRegistration {
    private MessageRegistration() {}
    public static void registerAll() {
        Serializer.registerClass(DisconnectMessage.class);
        Serializer.registerClass(LoggedInMessage.class);
        Serializer.registerClass(LoginRequestMessage.class);
        Serializer.registerClass(TextMessage.class);
        Serializer.registerClass(RequestMessage.class);
        Serializer.registerClass(ResponseMessage.class);
        Serializer.registerClass(PlayerStateChange.class);
        Serializer.registerClass(PlayerJoinedMessage.class);
    }
}
