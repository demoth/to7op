package server;

import java.io.IOException;
import java.util.Properties;
import java.util.TreeMap;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializer;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import common.ClientState;
import static common.Constants.Masks;
import common.entities.Player;
import common.messages.ActionMessage;
import common.messages.ClientStateMessage;
import common.messages.LoginMessage;
import common.messages.TextMessage;

public class ServerMain extends SimpleApplication {
    Server server;
    TreeMap<Integer, Player> players = new TreeMap<>();
    private ServerProperties conf;
    private Spatial          sceneModel;
    private RigidBodyControl landscapeControl;
    private BulletAppState   bulletAppState;

    public static void main(String[] args) {
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
                    ActionMessage action = (ActionMessage) message;
                    processAction(action, conn);
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

        System.out.println(msg.login + " joined!");
        server.broadcast(new TextMessage(msg.login + " joined!"));
        server.broadcast(Filters.in(conn), new TextMessage("Welcome, " + msg.login + '\n' + conf.motd));
    }

    private void processAction(ActionMessage action, HostedConnection conn) {
        ClientState oldState = players.get(conn.getId()).currentState;
        ClientState newState = applyCommands(oldState, action);
        players.get(conn.getId()).currentState = newState;
        server.broadcast(Filters.in(conn), new ClientStateMessage(oldState.diff(newState)));
    }

    private ClientState applyCommands(ClientState oldState, ActionMessage action) {
        ClientState result = new ClientState(oldState.entityId);
        result.view = action.view;
        result.speed = oldState.speed;
        if (pressed(action.buttons, Masks.WALK_FORWARD))
            result.position = oldState.position.addLocal(action.view);
        if (pressed(action.buttons, Masks.JUMP))
            ;
        return result;
    }

    private boolean pressed(long buttons, long desired) {
        return (buttons & desired) > 0;
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
}
