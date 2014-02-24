package server;

import com.jme3.app.SimpleApplication;
import com.jme3.network.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.system.JmeContext;
import common.messages.LoginMessage;

import java.io.IOException;

public class ServerMain extends SimpleApplication {
    Server server;
    @Override
    public void simpleInitApp() {
        Serializer.registerClass(LoginMessage.class);
        try {
            server = Network.createServer(5555);
            server.addMessageListener(new MessageListener<HostedConnection>() {
                @Override
                public void messageReceived(HostedConnection hostedConnection, Message message) {
                    if (message instanceof LoginMessage){
                        LoginMessage m = (LoginMessage) message;
                        System.out.println( m.login + " joined!");
                        server.broadcast(new LoginMessage(m.login, m.password, true));
                    }
                }
            }, LoginMessage.class);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ServerMain().start(JmeContext.Type.Headless);
    }

    @Override
    public void destroy() {
        server.close();
        super.destroy();
    }
}
