package org.demoth.nogaem.server;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Vector3f;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import org.demoth.nogaem.common.Constants;
import org.demoth.nogaem.common.MessageRegistration;
import org.demoth.nogaem.common.entities.Player;
import org.demoth.nogaem.common.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static com.jme3.network.Filters.in;
import static org.demoth.nogaem.common.Config.*;

public class ServerMain extends SimpleApplication {
    private static final Logger   log = LoggerFactory.getLogger("Server");
    private static final Vector3f up  = new Vector3f(0f, 1f, 0f);
    Server server;
    Map<Integer, Player>           players  = new ConcurrentHashMap<>();
    ConcurrentLinkedQueue<Message> requests = new ConcurrentLinkedQueue<>();
    private BulletAppState bulletAppState;
    private boolean running = true;

    public static void run() {
        new ServerMain().start(JmeContext.Type.Headless);
    }

    @Override
    public void simpleInitApp() {
        MessageRegistration.registerAll();
        try {
            server = Network.createServer(sv_port);
            addMessageListeners();
            initWorld();
            server.start();
            new Thread(() -> {
                while (running) {
                    sendResponses();
                    try {
                        Thread.sleep(sv_sleep);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }).start();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void initWorld() {
        // Setup world
            /* Set up Physics */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.getPhysicsSpace().enableDebug(assetManager);

        // We load the scene from the zip file and adjust its size.
        assetManager.registerLocator("data/town.zip", ZipLocator.class);
        Spatial sceneModel = assetManager.loadModel("main.scene");
        sceneModel.setLocalScale(g_scale);
        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        RigidBodyControl landscapeControl = new RigidBodyControl(sceneShape, g_mass);
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
        log.info("Shutting down server...");
        server.close();
        super.destroy();
    }

    private void sendResponses() {
        server.broadcast(new ResponseMessage(players.values().stream()
                .map(p -> new PlayerStateChange(p.id, p.view, p.control.getPhysicsLocation()))
                .collect(Collectors.toList())));
    }

    private void addMessageListeners() {
        server.addMessageListener(this::addPlayer, LoginRequestMessage.class);
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
        bulletAppState.getPhysicsSpace().remove(players.get(conn.getId()).control);
        players.remove(conn.getId());
        log.info("remaining players: " + players.size());
        server.broadcast(new DisconnectMessage(conn.getId()));
        conn.close("Goodbye");
    }

    private void addPlayer(HostedConnection conn, Message message) {
        log.info("LoggedInMessage received", message);
        LoginRequestMessage msg = (LoginRequestMessage) message;
        if (players.values().stream().anyMatch(p -> p.login.equals(msg.login)))
            conn.close("Player with login " + msg.login + " is already in game");
        Player player = new Player(conn.getId(), msg.login);
        player.conn = conn;

        player.control = createPlayerPhysics();
        bulletAppState.getPhysicsSpace().add(player.control);

        players.put(conn.getId(), player);
        server.broadcast(new PlayerJoinedMessage(conn.getId(), msg.login, g_spawn_point));
        server.broadcast(in(conn), new LoggedInMessage(msg.login, "", player.id, 0, g_map));
    }

    private CharacterControl createPlayerPhysics() {
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(g_player_radius, g_player_height, g_player_axis);
        CharacterControl control = new CharacterControl(capsuleShape, g_player_step);
        control.setJumpSpeed(g_player_jumpheight);
        control.setFallSpeed(g_player_fallspeed);
        control.setGravity(g_player_gravity);
        control.setPhysicsLocation(g_spawn_point);
        return control;
    }

    private void processRequest(Message message) {
        RequestMessage request = (RequestMessage) message;
        Player player = players.get(request.playerId);
        if (player == null)
            return;
        player.view = new Vector3f(request.view.x, 0f, request.view.z);
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
        Vector3f left = player.view.cross(up).multLocal(isStrafing);
        Vector3f walkDirection = player.view.multLocal(isWalking).add(left);
        player.control.setWalkDirection(walkDirection.normalize());
    }

    private boolean pressed(long buttons, long desired) {
        return (buttons & desired) > 0;
    }

}
