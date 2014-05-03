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
import com.jme3.network.*;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import org.demoth.nogaem.common.Constants;
import org.demoth.nogaem.common.MessageRegistration;
import org.demoth.nogaem.common.entities.Player;
import org.demoth.nogaem.common.messages.DisconnectMessage;
import org.demoth.nogaem.common.messages.TextMessage;
import org.demoth.nogaem.common.messages.client.LoginRequestMessage;
import org.demoth.nogaem.common.messages.client.RconMessage;
import org.demoth.nogaem.common.messages.client.RequestMessage;
import org.demoth.nogaem.common.messages.server.*;
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
    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);
    private static final Vector3f up = new Vector3f(0f, 1f, 0f);
    Server server;
    Map<Integer, Player> players = new ConcurrentHashMap<>();
    ConcurrentLinkedQueue<Message> requests = new ConcurrentLinkedQueue<>();
    private BulletAppState bulletAppState;
    private Thread sender;

    public static void run() {
        new ServerMain().start(JmeContext.Type.Headless);
    }

    @Override
    public void simpleInitApp() {
        MessageRegistration.registerAll();
        log.info("Registered messages");
        try {
            assetManager.registerLocator("data/town.zip", ZipLocator.class);
            server = Network.createServer(port);
            log.info("Created server");
            addMessageListeners();
            server.start();
            server.addConnectionListener(new ConnectionListener() {
                @Override
                public void connectionAdded(Server server, HostedConnection conn) {
                    log.info("Client connected from " + conn.getAddress());
                }

                @Override
                public void connectionRemoved(Server server, HostedConnection conn) {
                    removePlayerFromGame(conn);
                    log.info("Broadcasting disconnectMessage for id:" + conn.getId());
                    server.broadcast(new DisconnectMessage(conn.getId()));
                }
            });
            if (!map.isEmpty())
                changeMap(map);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }


    private void changeMap(String mapName) {
        // todo check if mapName is valid
        log.info("Changing map to: " + mapName);
        stopSendingUpdates();
        server.broadcast(new ChangeMapMessage(mapName));
        stateManager.detach(bulletAppState);
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        Spatial sceneModel = assetManager.loadModel(mapName);
        sceneModel.setLocalScale(g_scale);
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        RigidBodyControl landscapeControl = new RigidBodyControl(sceneShape, 0f);
        sceneModel.addControl(landscapeControl);
        bulletAppState.getPhysicsSpace().add(landscapeControl);
        players.values().forEach(p -> {
            p.control = createPlayerPhysics();
            bulletAppState.getPhysicsSpace().add(p.control);
        });
        startSendingUpdates();
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

    private void startSendingUpdates() {
        log.info("starting sending updates");
        sender = new Thread(() -> {
            while (true) {
                sendResponses();
                try {
                    Thread.sleep(sv_sleep);
                } catch (InterruptedException e) {
                    log.info("stopped sending updates");
                    break;
                }
            }
        });
        sender.start();
    }

    private void stopSendingUpdates() {
        if (sender != null)
            sender.interrupt();
    }

    private void sendResponses() {
        if (players.size() > 0)
            server.broadcast(new ResponseMessage(players.values().stream()
                    .map(p -> new PlayerStateChange(p.id, p.view, p.control.getPhysicsLocation()))
                    .collect(Collectors.toList())));
    }

    private void addMessageListeners() {
        server.addMessageListener(this::addPlayer, LoginRequestMessage.class);
        server.addMessageListener(this::queueRequest, RequestMessage.class);
        server.addMessageListener(this::execCommand, RconMessage.class);
        server.addMessageListener(this::sendChatMsg, TextMessage.class);
    }

    private void sendChatMsg(HostedConnection conn, Message message) {
        server.broadcast(new TextMessage(players.get(conn.getId()).login + ':' + ((TextMessage) message).text));
    }

    private void queueRequest(HostedConnection conn, Message message) {
        RequestMessage request = (RequestMessage) message;
        request.playerId = conn.getId();
        requests.add(message);
    }

    private void removePlayerFromGame(HostedConnection conn) {
        log.info("disconnecting: " + players.get(conn.getId()).login);
        bulletAppState.getPhysicsSpace().remove(players.get(conn.getId()).control);
        players.remove(conn.getId());
        log.info("remaining players: " + players.size());
    }

    private void addPlayer(HostedConnection conn, Message message) {
        log.info("LoginRequestMessage received", message);
        LoginRequestMessage msg = (LoginRequestMessage) message;
        if (players.values().stream().anyMatch(p -> p.login.equals(msg.login)))
            conn.close("Player with login " + msg.login + " is already in game");
        Player player = new Player(conn.getId(), msg.login);
        player.conn = conn;

        player.control = createPlayerPhysics();
        bulletAppState.getPhysicsSpace().add(player.control);

        players.put(conn.getId(), player);
        server.broadcast(new NewPlayerJoinedMessage(conn.getId(), msg.login, g_spawn_point));
        server.broadcast(in(conn), new JoinedGameMessage(msg.login, player.id, map));
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

    private void execCommand(HostedConnection conn, Message message) {
        log.info("Rcon command: " + message);
        RconMessage msg = (RconMessage) message;
        if (!rcon_pass.equals(msg.password))
            return;
        switch (msg.command) {
            case stop:
                stop();
                break;
            case map:
                changeMap(msg.args);
                break;
            case set:
                break;
            case addbot:
                break;
            case kick:
                break;
        }
    }

    private boolean pressed(long buttons, long desired) {
        return (buttons & desired) > 0;
    }

}
