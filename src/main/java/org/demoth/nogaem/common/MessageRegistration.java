package org.demoth.nogaem.common;

import com.jme3.network.serializing.Serializer;
import org.demoth.nogaem.common.messages.DisconnectMessage;
import org.demoth.nogaem.common.messages.TextMessage;
import org.demoth.nogaem.common.messages.client.LoginRequestMessage;
import org.demoth.nogaem.common.messages.client.RconMessage;
import org.demoth.nogaem.common.messages.client.RequestMessage;
import org.demoth.nogaem.common.messages.server.*;

/**
 * Util class to make registration the same on both client & server.
 *
 * @author demoth
 */
public class MessageRegistration {
    private MessageRegistration() {}
    public static void registerAll() {
        Serializer.registerClass(DisconnectMessage.class);
        Serializer.registerClass(JoinedGameMessage.class);
        Serializer.registerClass(ChangeMapMessage.class);
        Serializer.registerClass(LoginRequestMessage.class);
        Serializer.registerClass(TextMessage.class);
        Serializer.registerClass(RconMessage.class);
        Serializer.registerClass(RequestMessage.class);
        Serializer.registerClass(ResponseMessage.class);
        Serializer.registerClass(PlayerStateChange.class);
        Serializer.registerClass(NewPlayerJoinedMessage.class);
    }
}
