package org.demoth.nogaem.server;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.*;
import com.jme3.bullet.control.*;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.*;
import com.jme3.network.*;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import org.demoth.nogaem.common.*;
import org.demoth.nogaem.common.entities.Entity;
import org.demoth.nogaem.common.messages.TextMessage;
import org.demoth.nogaem.common.messages.fromClient.*;
import org.demoth.nogaem.common.messages.fromServer.*;
import org.slf4j.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.jme3.network.Filters.in;
import static org.demoth.nogaem.common.Config.*;

public class ServerMain extends SimpleApplication {
    static final Logger   log = LoggerFactory.getLogger(ServerMain.class);
    static final Vector3f up  = new Vector3f(0f, 1f, 0f);

    final Map<Integer, Entity> entities      = new ConcurrentHashMap<>();
    final Map<Integer, Player> players       = new ConcurrentHashMap<>();
    final Collection<Message>  requests      = new ConcurrentLinkedQueue<>();
    final Collection<Entity>   addedEntities = new ConcurrentLinkedQueue<>();
    final Collection<Integer>  removedIds    = new ConcurrentLinkedQueue<>();
    Collection<EntityState> changes = new ConcurrentLinkedQueue<>();
    Random                  random  = new Random();
    Server         server;
    BulletAppState bulletAppState;
    Thread         sender;
    long           frameIndex;
    private int lastId = 10000;

    public static void run() {
        new ServerMain().start(JmeContext.Type.Headless);
    }

    @Override
    public void simpleInitApp() {
        Util.registerMessages();
        log.info("Registered messages");
        try {
            Util.scanDataFolder(assetManager);
            server = Network.createServer(port);
            log.info("Created server");
            addMessageListeners();
            server.start();
            server.addConnectionListener(new ConnectionListener() {
                @Override
                public void connectionAdded(Server server, HostedConnection conn) {
                    log.info("Client connecting from " + conn.getAddress());
                }

                @Override
                public void connectionRemoved(Server server, HostedConnection conn) {
                    removePlayerFromGame(conn);
                }
            });
            changeMap(map);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    private void changeMap(String mapName) {
        // todo check if mapName is valid
        log.info("Changing map to: " + mapName);
        stopSendingUpdates();
        frameIndex = 0;
        server.broadcast(new ChangeMapMessage(mapName));
        stateManager.detach(bulletAppState);
        if (mapName.isEmpty())
            return;
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        Spatial sceneModel = assetManager.loadModel(mapName);
        sceneModel.setLocalScale(g_scale);
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        RigidBodyControl landscapeControl = new RigidBodyControl(sceneShape, 0f);
        sceneModel.addControl(landscapeControl);
        bulletAppState.getPhysicsSpace().add(landscapeControl);
        players.values().forEach(p -> {
            p.physics = createPlayerPhysics();
            bulletAppState.getPhysicsSpace().add(p.physics);
        });
        startSendingUpdates();
    }

    @Override
    public void update() {
        super.update();
        players.values().forEach(pl -> pl.entity.state.pos = pl.physics.getPhysicsLocation());
        requests.forEach(this::processRequest);
        requests.clear();
        updateGameState();
    }

    private void updateGameState() {
        for (Entity e : entities.values()) {
            if (e.modelName.equals("axe")) {
                if (random.nextFloat() < 0.02f) {
                    e.state.pos = e.state.pos.add(new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())).mult(random.nextFloat());
                    log.info("New position: " + e.state.pos);
                }
                if (random.nextFloat() < 0.02f) {
                    e.state.rot = new Quaternion(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat());
                }
            }
        }
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
        changes = entities.values().stream().map(e -> e.state).collect(Collectors.toList());
        frameIndex++;
        log.trace("Sending respose to {0}. A={1}, R={2}, C={3}",players.size(), addedEntities.size(), removedIds.size(), changes.size());
        players.values().stream().filter(p -> p.isReady).forEach(pl ->
                server.broadcast(in(pl.conn), calculateChanges(pl)));
        addedEntities.clear();
        removedIds.clear();
    }

    private GameStateChange calculateChanges(Player pl) {
        //log.info("player " + pl.id + " has " + pl.notConfirmedMessages.size() + " non confirmes msgs.");
        GameStateChange msg = new GameStateChange();
        msg.index = frameIndex;
        msg.added = new HashSet<>(addedEntities);
        msg.removedIds = new HashSet<>(removedIds);
        if (pl.notConfirmedMessages.size() > sv_drop_after) {
            removePlayerFromGame(pl.conn);
            pl.conn.close("Bad connection");
        }
        pl.notConfirmedMessages.forEach(gsm -> {
            if (gsm.added != null)
                msg.added.addAll(gsm.added);
            if (gsm.removedIds != null)
                msg.removedIds.addAll(gsm.removedIds);
        });
        pl.notConfirmedMessages.add(msg);
        msg.changes = changes;
        log.trace("Changes for {0} A={1} R={2} C={3}", pl.entity.name, msg.added.size(), msg.removedIds.size(), msg.changes.size());
        return msg;
    }

    private void addMessageListeners() {
        server.addMessageListener(this::addPlayer, LoginRequestMessage.class);
        server.addMessageListener(this::queueRequest, ActionMessage.class);
        server.addMessageListener(this::execCommand, RconMessage.class);
        server.addMessageListener(this::sendChatMsg, TextMessage.class);
        server.addMessageListener(this::acknowledge, Acknowledgement.class);
    }

    private void acknowledge(HostedConnection conn, Message message) {
        Acknowledgement ack = (Acknowledgement) message;
        Player player = players.get(conn.getId());
        if (player == null)
            return;
        if (ack.index == -1) {
            player.isReady = true;
            log.info("Player " + conn.getId() + " is ready");
            return;
        }
        log.trace("Acknowledging for {0} index: {1}", conn.getId(), ack.index);
        player.notConfirmedMessages.removeAll(player.notConfirmedMessages.stream().filter(m ->
                m.index <= ack.index).collect(Collectors.toList()));
        player.lastReceivedMessageIndex = ack.index;

    }

    private void sendChatMsg(HostedConnection conn, Message message) {
        server.broadcast(new TextMessage(players.get(conn.getId()).entity.name + ':' + ((TextMessage) message).text));
    }

    private void queueRequest(HostedConnection conn, Message message) {
        ActionMessage request = (ActionMessage) message;
        request.playerId = conn.getId();
        //log.info("Queued request for " + request.playerId + ". " + message);
        requests.add(message);
    }

    private void removePlayerFromGame(HostedConnection conn) {
        Player player = players.get(conn.getId());
        if (player == null)
            return;
        log.info("disconnecting: " + player.entity.name + " id: " + conn.getId());
        bulletAppState.getPhysicsSpace().remove(player.physics);
        entities.remove(conn.getId());
        players.remove(conn.getId());
        removedIds.add(conn.getId());
        log.info("remaining players: " + players.size());
    }

    private void addPlayer(HostedConnection conn, Message message) {
        log.info("LoginRequestMessage received: " + message);
        LoginRequestMessage msg = (LoginRequestMessage) message;
        if (players.values().stream().anyMatch(p -> p.entity.name.equals(msg.login))) {
            conn.close("Player with login " + msg.login + " is already in game");
            return;
        }
        Player player = new Player(conn, msg.login, createPlayerPhysics());
        server.broadcast(in(conn), new JoinedGameMessage(player.entity.id, map));

        if (map.isEmpty())
            return;

        player.notConfirmedMessages.add(new GameStateChange(new HashSet<>(entities.values())));
        bulletAppState.getPhysicsSpace().add(player.physics);
        entities.put(conn.getId(), player.entity);
        players.put(conn.getId(), player);
        addedEntities.add(player.entity);
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
        ActionMessage request = (ActionMessage) message;
        Player player = players.get(request.playerId);
        if (player == null)
            return;
        float isWalking = 0f;
        float isStrafing = 0f;
        if (pressed(request.buttons, Constants.Masks.WALK_FORWARD))
            isWalking = 1f;
        else if (pressed(request.buttons, Constants.Masks.WALK_BACKWARD))
            isWalking = -1f;
        if (pressed(request.buttons, Constants.Masks.STRAFE_LEFT))
            isStrafing = -1f;
        else if (pressed(request.buttons, Constants.Masks.STRAFE_RIGHT))
            isStrafing = 1f;
        if (pressed(request.buttons, Constants.Masks.JUMP))
            player.physics.jump();
        if (pressed(request.buttons, Constants.Masks.FIRE_PRIMARY))
            createProjectile(player.entity.state.rot, player.entity.state.pos);
        Vector3f left = request.dir.cross(up).multLocal(isStrafing);
        Vector3f walkDirection = request.dir.multLocal(isWalking).add(left);
        player.entity.state.rot = request.rot;
        player.physics.setWalkDirection(walkDirection.normalize());
    }

    private void createProjectile(Quaternion rot, Vector3f pos) {
        int id = ++lastId;
        Entity axe = new Entity(id, "axe", "axe" + id, new EntityState(id, rot, pos));
        entities.put(id, axe);
        addedEntities.add(axe);
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
