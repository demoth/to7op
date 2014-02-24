package server;

import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector2f;
import com.jme3.network.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.system.JmeContext;
import common.entities.Player;
import common.messages.LoginMessage;
import common.messages.MoveEntity;

import java.io.IOException;
import java.util.TreeMap;

public class ServerMain extends SimpleApplication {
    Server server;
    TreeMap<Integer, Player> players = new TreeMap<>();
    @Override
    public void simpleInitApp() {
        Serializer.registerClass(LoginMessage.class);
        Serializer.registerClass(MoveEntity.class);
        try {
            server = Network.createServer(5555);
            server.addMessageListener(new MessageListener<HostedConnection>() {
                @Override
                public void messageReceived(HostedConnection hostedConnection, Message message) {
                    if (message instanceof LoginMessage){
                        LoginMessage m = (LoginMessage) message;
                        System.out.println( m.login + " joined!");
                        players.put(hostedConnection.getId(), new Player(hostedConnection.getId(), m.login, Vector2f.ZERO));
                        server.broadcast(new LoginMessage(m.login, m.password, 20));
                    }
                }
            }, LoginMessage.class);
            server.addMessageListener(new MessageListener<HostedConnection>() {
                @Override
                public void messageReceived(HostedConnection hostedConnection, Message message) {
                    if (message instanceof MoveEntity) {
                        System.out.println("moving");
                        MoveEntity m = (MoveEntity) message;
                        server.broadcast(new MoveEntity(m.id, m.delta));
                    }
                }
            }, MoveEntity.class);
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
