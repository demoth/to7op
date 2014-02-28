package server;

import com.jme3.app.SimpleApplication;
import com.jme3.network.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.system.JmeContext;
import common.ClientState;
import common.entities.Player;
import common.messages.ActionMessage;
import common.messages.ClientStateMessage;
import common.messages.LoginMessage;
import common.messages.TextMessage;

import java.io.IOException;
import java.util.Properties;
import java.util.TreeMap;

import static common.Constants.*;

public class ServerMain extends SimpleApplication {
    Server server;
    TreeMap<Integer, Player> players = new TreeMap<>();

    public static void main(String[] args) {
        new ServerMain().start(JmeContext.Type.Headless);
    }

    @Override
    public void simpleInitApp() {
        registerMessages();
        final ServerProperties conf = loadConfiguration();
        try {
            server = Network.createServer(conf.port);
            server.addMessageListener(new MessageListener<HostedConnection>() {
                @Override
                public void messageReceived(HostedConnection hostedConnection, Message message) {
                    if (message instanceof LoginMessage){
                        LoginMessage m = (LoginMessage) message;
                        System.out.println( m.login + " joined!");
                        players.put(hostedConnection.getId(), new Player(hostedConnection.getId(), m.login));
                        server.broadcast(new TextMessage(m.login + " joined!"));
                        server.broadcast(Filters.in(hostedConnection), new TextMessage("Welcome, " + m.login + '\n'
                                + conf.motd));
                    }
                }
            }, LoginMessage.class);
            server.addMessageListener(new MessageListener<HostedConnection>() {
                @Override
                public void messageReceived(HostedConnection conn, Message message) {
                    if (message instanceof ActionMessage) {
                        ActionMessage action = (ActionMessage) message;
                        ClientState oldState = players.get(conn.getId()).currentState;
                        ClientState newState = applyCommands(oldState, action);
                        players.get(conn.getId()).currentState = newState;
                        server.broadcast(Filters.in(conn), new ClientStateMessage(oldState.diff(newState)));
                    }
                }
            }, ActionMessage.class);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ClientState applyCommands(ClientState oldState, ActionMessage action) {
        ClientState result = new ClientState(oldState.entityId);
        result.view = action.view;
        result.speed = oldState.speed;
        if (pressed(action.buttons, Masks.WALK_FORWARD))
            result.position = oldState.position.add(
                    action.view);
        return result;
    }

    private boolean pressed(long buttons, long desired) {
        return (buttons & desired) == 1l;
    }

    @Override
    public void destroy() {
        server.close();
        super.destroy();
    }

    private ServerProperties loadConfiguration() {
        // todo move to file
        Properties properties = new Properties();
        properties.setProperty("port", "5555");
        properties.setProperty("motd", "welcome to the gaem");

        ServerProperties props = new ServerProperties();
        props.motd = properties.getProperty("motd");
        props.port = Integer.parseInt(properties.getProperty("port"));
        return props;
    }

    private void registerMessages() {
        Serializer.registerClass(LoginMessage.class);
        Serializer.registerClass(ActionMessage.class);
        Serializer.registerClass(ClientStateMessage.class);
    }

    private class ServerProperties {
        int port;
        String motd;
    }
}
