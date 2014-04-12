package server;

import com.jme3.app.SimpleApplication;
import com.jme3.network.*;
import com.jme3.system.JmeContext;
import common.MessageRegistration;
import common.entities.Player;
import common.messages.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static com.jme3.network.Filters.in;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ServerMain extends SimpleApplication {
    private static final Logger log = Logger.getLogger("Server");
    Server server;
    Map<Integer, Player> players = new ConcurrentHashMap<>();
    private ServerProperties conf;
    ConcurrentLinkedQueue<Message> commands = new ConcurrentLinkedQueue<>();

    public static void main(String... args) {
        new ServerMain().start(JmeContext.Type.Headless);
    }

    @Override
    public void simpleInitApp() {
        MessageRegistration.registerAll();
        conf = loadConfiguration();
        try {
            server = Network.createServer(conf.port);
            addMessageListeners();
            server.start();
            Executors.newSingleThreadScheduledExecutor()
                    .scheduleAtFixedRate(this::sendResponses, 0, 1, SECONDS).get();
        } catch (InterruptedException | ExecutionException | IOException e) {
            log.severe(e.getMessage());
        }
    }

    @Override
    public void update() {
        super.update();
        commands.forEach(this::applyCommand);
    }

    @Override
    public void destroy() {
        server.close();
        super.destroy();
    }

    private void sendResponses(){
        // todo: for each world cell
        players.values().forEach(this::broadcastState);
    }

    private void broadcastState(Player player) {
        // todo: each client belong to certain world cell,
        // calculate updates for the whole cell,
        // then send response to all clients in this cell
        server.broadcast(in(player.conn), new ResponseMessage(new Date()));
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

    private void addMessageListeners() {
        server.addMessageListener(this::addPlayer, LoginMessage.class);
        server.addMessageListener(this::addActionMessage, RequestMessage.class);
    }

    private void addActionMessage(HostedConnection conn, Message message) {
        log.info("RequestMessage received: " + message);
        commands.add(message);
    }

    private void addPlayer(HostedConnection conn, Message message) {
        log.info("LoginMessage received: " + message);
        LoginMessage msg = (LoginMessage) message;
        Player player = new Player(conn.getId(), msg.login, msg.startTime);
        player.conn = conn;
        players.put(conn.getId(), player);
        // todo broadcast reliable message about new player
        server.broadcast(new TextMessage(msg.login + " joined!"));
        server.broadcast(in(conn), new LoginMessage(msg.login, "", player.id, 0));
    }

    private void applyCommand(Message action) {
//        float isWalking = 0f;
//        float isStrafing = 0f;
//        if (pressed(action.buttons, Masks.WALK_FORWARD))
//            isWalking = 1f;
//        if (pressed(action.buttons, Masks.WALK_BACKWARD))
//            isWalking = -1f;
//        if (pressed(action.buttons, Masks.STRAFE_LEFT))
//            isStrafing = 1f;
//        if (pressed(action.buttons, Masks.STRAFE_RIGHT))
//            isStrafing = -1f;
//        if (pressed(action.buttons, Masks.JUMP))
//            player.control.jump();
//        Vector3f left = action.view.cross(0f, 1f, 0f, new Vector3f()).multLocal(isStrafing);
//        player.control.setWalkDirection(action.view.multLocal(isWalking).add(left));
    }

    private boolean pressed(long buttons, long desired) {
        return (buttons & desired) > 0;
    }

    private class ServerProperties {
        int port;
        String motd;
    }

}
