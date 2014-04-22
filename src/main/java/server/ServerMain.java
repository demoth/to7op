package server;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.*;
import com.jme3.bullet.control.*;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Vector3f;
import com.jme3.network.*;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import common.*;
import common.entities.Player;
import common.messages.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.jme3.network.Filters.in;

public class ServerMain extends SimpleApplication {
    private static final Logger log = Logger.getLogger("Server");
    private static final Vector3f up = new Vector3f(0f, 1f, 0f);
    Server server;
    Map<Integer, Player> players = new ConcurrentHashMap<>();
    ConcurrentLinkedQueue<Message> requests = new ConcurrentLinkedQueue<>();
    private BulletAppState bulletAppState;
    private boolean running = true;

    public static void main(String... args) {
        new ServerMain().start(JmeContext.Type.Headless);
    }

    @Override
    public void simpleInitApp() {
        MessageRegistration.registerAll();
        ServerProperties conf = loadConfiguration();
        try {
            server = Network.createServer(conf.port);
            addMessageListeners();
            initWorld();
            server.start();
            new Thread(() -> {
                while (running) {
                    sendResponses();
                    try {
                        Thread.sleep(Constants.updateRate);
                    } catch (InterruptedException e) {
                        log.severe(e.getMessage());
                    }
                }
            }).start();
        } catch (IOException e) {
            log.severe(e.getMessage());
        }
    }

    private void initWorld() {
        // Setup world
            /* Set up Physics */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.getPhysicsSpace().enableDebug(assetManager);

        // We load the scene from the zip file and adjust its size.
        assetManager.registerLocator("town.zip", ZipLocator.class);
        Spatial sceneModel = assetManager.loadModel("main.scene");
        sceneModel.setLocalScale(2f);
        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        RigidBodyControl landscapeControl = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscapeControl);
        // We attach the scene and the player to the rootnode and the physics space,
        // to make them appear in the game world.
        bulletAppState.getPhysicsSpace().add(landscapeControl);
        rootNode.attachChild(sceneModel);
    }

    @Override
    public void update() {
        super.update();
        requests.forEach(this::processRequest);
        requests.clear();
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
        server.broadcast(new ResponseMessage(players.values().stream()
                .map(p -> new PlayerStateChange(p.id, p.control.getPhysicsLocation()))
                .collect(Collectors.toList())));
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
        server.addMessageListener(this::queueRequest, RequestMessage.class);
        server.addMessageListener(this::disconnect, DisconnectMessage.class);
    }

    private void queueRequest(HostedConnection conn, Message message) {
        RequestMessage request = (RequestMessage) message;
        request.playerId = conn.getId();
        requests.add(message);
    }

    private void disconnect(HostedConnection conn, Message message) {
        log.info("disconnecting: " + players.get(conn.getId()).login);
        players.remove(conn.getId());
        log.info("remaining players: " + players.size());
    }

    private void addPlayer(HostedConnection conn, Message message) {
        log.info("LoginMessage received: " + message);
        LoginMessage msg = (LoginMessage) message;
        Player player = new Player(conn.getId(), msg.login, msg.startTime);
        player.conn = conn;

        player.control = createPlayerPhysics();
        bulletAppState.getPhysicsSpace().add(player.control);

        players.put(conn.getId(), player);
        // todo broadcast reliable message about new player
        server.broadcast(new TextMessage(msg.login + " joined!"));
        server.broadcast(in(conn), new LoginMessage(msg.login, "", player.id, 0));
    }

    private CharacterControl createPlayerPhysics() {
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        CharacterControl control = new CharacterControl(capsuleShape, 0.05f);
        control.setJumpSpeed(30);
        control.setFallSpeed(30);
        control.setGravity(30);
        control.setPhysicsLocation(new Vector3f(0, 10, 0));
        return control;
    }

    private void processRequest(Message message) {
        RequestMessage request = (RequestMessage) message;
        Player player = players.get(request.playerId);
        if (player == null)
            return;
        Vector3f forward = new Vector3f(request.view.x, 0f, request.view.z);
        float isWalking = 0f;
        float isStrafing = 0f;
        if (pressed(request.buttons, Constants.Masks.WALK_FORWARD))
            isWalking = 1f;
        if (pressed(request.buttons, Constants.Masks.WALK_BACKWARD))
            isWalking = -1f;
        if (pressed(request.buttons, Constants.Masks.STRAFE_LEFT))
            isStrafing = -1f;
        if (pressed(request.buttons, Constants.Masks.STRAFE_RIGHT))
            isStrafing = 1f;
        if (pressed(request.buttons, Constants.Masks.JUMP))
            player.control.jump();
        Vector3f left = forward.cross(up).multLocal(isStrafing);
        Vector3f walkDirection = forward.multLocal(isWalking).add(left);
        player.control.setWalkDirection(walkDirection.normalize());
    }

    private boolean pressed(long buttons, long desired) {
        return (buttons & desired) > 0;
    }

    private class ServerProperties {
        int port;
        String motd;
    }

}
