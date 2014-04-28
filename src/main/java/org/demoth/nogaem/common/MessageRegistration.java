package org.demoth.nogaem.common;

import com.jme3.network.serializing.Serializer;
import org.demoth.nogaem.common.messages.DisconnectMessage;
import org.demoth.nogaem.common.messages.TextMessage;
import org.demoth.nogaem.common.messages.client.LoginRequestMessage;
import org.demoth.nogaem.common.messages.client.RequestMessage;
import org.demoth.nogaem.common.messages.server.LoggedInMessage;
import org.demoth.nogaem.common.messages.server.PlayerJoinedMessage;
import org.demoth.nogaem.common.messages.server.PlayerStateChange;
import org.demoth.nogaem.common.messages.server.ResponseMessage;

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
