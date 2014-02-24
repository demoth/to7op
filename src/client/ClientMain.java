package client;

import com.jme3.app.SimpleApplication;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.serializing.Serializer;
import com.jme3.system.JmeContext;
import common.messages.LoginMessage;

import java.io.IOException;

public class ClientMain extends SimpleApplication {
    Client client;
    @Override
    public void simpleInitApp() {
        Serializer.registerClass(LoginMessage.class);
        try {
            client = Network.connectToServer("127.0.0.1", 5555);
            client.addMessageListener(new MessageListener<Client>() {
                @Override
                public void messageReceived(Client client, Message message) {
                    if (message instanceof LoginMessage){
                        if (((LoginMessage) message).loggedIn){
                            System.out.println("Joined!");
                        }
                    }
                }
            }, LoginMessage.class);
            client.start();
            client.send(new LoginMessage("demoth","cadaver", false));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        client.close();
        super.destroy();
    }

    public static void main(String[] args) {
        new ClientMain().start(JmeContext.Type.Display);
    }
}
