package server;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.*;
import com.jme3.bullet.control.*;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.network.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import common.ClientState;
import common.entities.Player;
import common.messages.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static com.jme3.network.Filters.*;
import static common.Constants.Masks;

public class ServerMain extends SimpleApplication {
    Server server;
    TreeMap<Integer, Player> players = new TreeMap<>();
    private ServerProperties conf;
    private Spatial          sceneModel;
    private RigidBodyControl landscapeControl;
    private BulletAppState   bulletAppState;
    private boolean running = true;

    public static void main(String... args) {
        new ServerMain().start(JmeContext.Type.Headless);
    }

    @Override
    public void simpleInitApp() {
        registerMessages();
        conf = loadConfiguration();
        try {
            server = Network.createServer(conf.port);
            addMessageListeners();
            // Setup world
            /* Set up Physics */
            bulletAppState = new BulletAppState();
            stateManager.attach(bulletAppState);
            //bulletAppState.getPhysicsSpace().enableDebug(assetManager);

            // We load the scene from the zip file and adjust its size.
            assetManager.registerLocator("town.zip", ZipLocator.class);
            sceneModel = assetManager.loadModel("main.scene");
            sceneModel.setLocalScale(2f);
            // We set up collision detection for the scene by creating a
            // compound collision shape and a static RigidBodyControl with mass zero.
            CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
            landscapeControl = new RigidBodyControl(sceneShape, 0);
            sceneModel.addControl(landscapeControl);
            // We attach the scene and the player to the rootnode and the physics space,
            // to make them appear in the game world.
            rootNode.attachChild(sceneModel);
            bulletAppState.getPhysicsSpace().add(landscapeControl);
            setUpLight();

            server.start();
            new Sender().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerMessages() {
        Serializer.registerClass(LoginMessage.class);
        Serializer.registerClass(ActionMessage.class);
        Serializer.registerClass(ClientStateMessage.class);
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
        server.addMessageListener(new MessageListener<HostedConnection>() {
            @Override
            public void messageReceived(HostedConnection hostedConnection, Message message) {
                if (message instanceof LoginMessage) {
                    addPlayer(hostedConnection, (LoginMessage) message);
                }
            }
        }, LoginMessage.class);
        server.addMessageListener(new MessageListener<HostedConnection>() {
            @Override
            public void messageReceived(HostedConnection conn, Message message) {
                if (message instanceof ActionMessage) {
                    applyCommands(players.get(conn.getId()), (ActionMessage) message);
                }
            }
        }, ActionMessage.class);
    }

    private void addPlayer(HostedConnection conn, LoginMessage msg) {
        Player player = new Player(conn.getId(), msg.login, msg.startTime);
        // We set up collision detection for the player by creating
        // a capsule collision shape and a CharacterControl.
        // The CharacterControl offers extra settings for
        // size, stepheight, jumping, falling, and gravity.
        // We also put the player in its starting position.
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        CharacterControl control = new CharacterControl(capsuleShape, 0.05f);
        control.setJumpSpeed(20);
        control.setFallSpeed(30);
        control.setGravity(30);
        control.setPhysicsLocation(new Vector3f(0, 10, 0));
        player.control = control;
        bulletAppState.getPhysicsSpace().add(control);
        players.put(conn.getId(), player);
        //activeEntities.add(player);
        System.out.println(msg.login + " joined!");
        // todo broadcast reliable message about new player
        server.broadcast(new TextMessage(msg.login + " joined!"));
        server.broadcast(in(conn), new TextMessage("Welcome, " + msg.login + '\n' + conf.motd));
    }

    private void applyCommands(Player player, ActionMessage action) {
        float isWalking = 0f;
        float isStrafing = 0f;
        if (pressed(action.buttons, Masks.WALK_FORWARD))
            isWalking = 1f;
        if (pressed(action.buttons, Masks.WALK_BACKWARD))
            isWalking = -1f;
        if (pressed(action.buttons, Masks.STRAFE_LEFT))
            isStrafing = 1f;
        if (pressed(action.buttons, Masks.STRAFE_RIGHT))
            isStrafing = -1f;
        if (pressed(action.buttons, Masks.JUMP))
            player.control.jump();
        Vector3f left = action.view.cross(0f, 1f, 0f, new Vector3f()).multLocal(isStrafing);
        player.control.setWalkDirection(action.view.multLocal(isWalking).add(left));
    }

    private boolean pressed(long buttons, long desired) {
        return (buttons & desired) > 0;
    }

    @Override
    public void update() {
        super.update();
        for (Player p : players.values())
            p.currentState = new ClientState(p.control);
    }

    @Override
    public void destroy() {
        server.close();
        super.destroy();
    }

    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
    }


    private class ServerProperties {
        int    port;
        String motd;
    }

    private class Sender extends Thread {
        @Override
        public void run() {
            while (running) {
                for (Player p : players.values())
                    server.broadcast(in(p.conn), new ClientStateMessage(p.currentState));
                try {
                    wait(50);
                } catch (InterruptedException ignored) {
                    running = false;
                }
            }
        }
    }
}
