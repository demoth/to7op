package server;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.*;
import com.jme3.network.*;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import common.*;
import common.entities.Player;
import common.messages.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import static com.jme3.network.Filters.in;

public class ServerMain extends SimpleApplication {
    private static final Logger log = Logger.getLogger("Server");
    Server server;
    TreeMap<Integer, Player> players = new TreeMap<>();
    private ServerProperties conf;
    private Spatial          sceneModel;
    private RigidBodyControl landscapeControl;
    private BulletAppState   bulletAppState;
    private boolean running = true;
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
            // Setup world
            /* Set up Physics */
            //bulletAppState = new BulletAppState();
            //stateManager.attach(bulletAppState);
            //bulletAppState.getPhysicsSpace().enableDebug(assetManager);

            // We load the scene from the zip file and adjust its size.
            //assetManager.registerLocator("town.zip", ZipLocator.class);
            //sceneModel = assetManager.loadModel("main.scene");
            //sceneModel.setLocalScale(2f);
            // We set up collision detection for the scene by creating a
            // compound collision shape and a static RigidBodyControl with mass zero.
            //CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
            //landscapeControl = new RigidBodyControl(sceneShape, 0);
            //sceneModel.addControl(landscapeControl);
            // We attach the scene and the player to the rootnode and the physics space,
            // to make them appear in the game world.
            //bulletAppState.getPhysicsSpace().add(landscapeControl);
            //rootNode.attachChild(sceneModel);

            server.start();
            new Sender().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        server.addMessageListener(this::addMessage, ActionMessage.class);
    }

    private void addMessage(HostedConnection conn, Message message) {
        commands.add(message);
    }

    private void addPlayer(HostedConnection conn, Message message) {
        log.info("LoginMessage received: " + message);
        LoginMessage msg = (LoginMessage) message;
        Player player = new Player(conn.getId(), msg.login, msg.startTime);
        player.conn = conn;
        // We set up collision detection for the player by creating
        // a capsule collision shape and a CharacterControl.
        // The CharacterControl offers extra settings for
        // size, stepheight, jumping, falling, and gravity.
        // We also put the player in its starting position.
        //CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        //CharacterControl control = new CharacterControl(capsuleShape, 0.05f);
        //control.setJumpSpeed(30);
        //control.setFallSpeed(30);
        //control.setGravity(30);
        //control.setPhysicsLocation(new Vector3f(0, 10, 0));
        //player.control = control;
        //bulletAppState.getPhysicsSpace().add(control);
        players.put(conn.getId(), player);
        //activeEntities.add(player);
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

    private class ServerProperties {
        int    port;
        String motd;
    }

    private class Sender extends Thread {
        @Override
        public void run() {
            while (running) {
                players.values().forEach(this::broadcast);
                try {
                    sleep(1000);
                } catch (InterruptedException ignored) {
                    running = false;
                }
            }
        }

        private void broadcast(Player p) {
            server.broadcast(in(p.conn), p.currentState);
        }
    }
}
