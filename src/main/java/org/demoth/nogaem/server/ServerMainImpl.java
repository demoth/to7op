package org.demoth.nogaem.server;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.shapes.*;
import com.jme3.bullet.control.*;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.*;
import com.jme3.network.*;
import com.jme3.scene.*;
import com.jme3.system.JmeContext;
import org.demoth.nogaem.common.*;
import org.demoth.nogaem.common.entities.*;
import org.demoth.nogaem.common.messages.TextMessage;
import org.demoth.nogaem.common.messages.fromClient.*;
import org.demoth.nogaem.common.messages.fromServer.*;
import org.slf4j.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.jme3.network.Filters.in;
import static org.demoth.nogaem.common.Config.*;

public class ServerMainImpl extends SimpleApplication implements ServerMain {
    static final Logger   log = LoggerFactory.getLogger(ServerMainImpl.class);
    static final Vector3f up  = new Vector3f(0f, 1f, 0f);

    final Map<Integer, ServerEntity> entities      = new ConcurrentHashMap<>();
    final Map<Integer, Player>       players       = new ConcurrentHashMap<>();
    final Collection<Message>        requests      = new ConcurrentLinkedQueue<>();
    final Collection<EntityInfo>     addedEntities = new ConcurrentLinkedQueue<>();
    final Collection<Integer>        removedIds    = new ConcurrentLinkedQueue<>();
    Collection<EntityState> changes = new ConcurrentLinkedQueue<>();
    Server            server;
    BulletAppState    bulletAppState;
    UpdatingGameState updatingState;
    Thread            sender;
    long              frameIndex;
    private int lastId = 10000;
    boolean             hit;
    ServerEntityFactory entityFactory;

    public static void run() {
        new ServerMainImpl().start(JmeContext.Type.Headless);
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
            entityFactory = new ServerEntityFactory(this);
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
        stateManager.detach(updatingState);
        entities.clear();
        if (mapName.isEmpty())
            return;
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        entityFactory.bulletAppState = bulletAppState;
        bulletAppState.getPhysicsSpace().addCollisionListener(this::collide);
        updatingState = new UpdatingGameState();
        stateManager.attach(updatingState);
        Spatial sceneModel = assetManager.loadModel("maps/" + mapName);
        sceneModel.setLocalScale(g_scale);
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        RigidBodyControl landscapeControl = new RigidBodyControl(sceneShape, 0f);
        sceneModel.addControl(landscapeControl);
        bulletAppState.getPhysicsSpace().add(landscapeControl);
        players.values().forEach(p -> {
//            p.physics = createPlayerPhysics(p.info.id);
//            bulletAppState.getPhysicsSpace().add(p.physics);
        });
        startSendingUpdates();
    }

    private void collide(PhysicsCollisionEvent e) {
        Spatial nodeA = e.getNodeA();
        Spatial nodeB = e.getNodeB();
        if (nodeA != null && nodeB != null) {
            ServerEntity entityA = nodeA.getUserData("entity");
            ServerEntity entityB = nodeB.getUserData("entity");
            if (entityA != null && entityB != null) {
                if (entityA.touch != null)
                    entityA.touch.accept(entityB);
                else if (entityB.touch != null)
                    entityB.touch.accept(entityA);
            }
        }
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
                long started = System.currentTimeMillis();
                sendResponses();
                long toSleep = sv_sleep + started - System.currentTimeMillis();
                if (toSleep > 0)
                    try {
                        Thread.sleep(toSleep);
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
        log.trace("Sending respose to {0}. A={1}, R={2}, C={3}", players.size(), addedEntities.size(), removedIds.size(), changes.size());
        players.values().stream().filter(p -> p.isReady).forEach(pl ->
                server.broadcast(in(pl.conn), calculateChanges(pl)));
        addedEntities.clear();
        removedIds.clear();
        hit = false;
    }

    private GameStateChange calculateChanges(Player pl) {
        //log.info("player " + pl.id + " has " + pl.notConfirmedMessages.size() + " non confirmes msgs.");
        GameStateChange msg = new GameStateChange(pl.axeQuantity);
        msg.hitSound = hit;
        msg.index = frameIndex;
        msg.added = new HashMap<>();
        addedEntities.forEach(e -> msg.added.put(e.id, e));
        msg.removedIds = new HashSet<>(removedIds);
        if (pl.notConfirmedMessages.size() > sv_drop_after) {
            removePlayerFromGame(pl.conn);
            pl.conn.close("Bad connection");
        }
        pl.notConfirmedMessages.forEach(gsm -> {
            if (gsm.added != null)
                msg.added.putAll(gsm.added);
            if (gsm.removedIds != null)
                msg.removedIds.addAll(gsm.removedIds);
        });
        pl.notConfirmedMessages.add(msg);
        msg.changes = changes;
        log.trace("Changes for {0} A={1} R={2} C={3}", pl.info.name, msg.added.size(), msg.removedIds.size(), msg.changes.size());
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
        server.broadcast(new TextMessage(players.get(conn.getId()).info.name + ':' + ((TextMessage) message).text));
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
        log.info("disconnecting: " + player.info.name + " id: " + conn.getId());
        bulletAppState.getPhysicsSpace().remove(player.physics);
        entities.remove(conn.getId());
        players.remove(conn.getId());
        removedIds.add(conn.getId());
        log.info("remaining players: " + players.size());
    }

    private void addPlayer(HostedConnection conn, Message message) {
        log.info("LoginRequestMessage received: " + message);
        LoginRequestMessage msg = (LoginRequestMessage) message;
        if (players.values().stream().anyMatch(p -> p.info.name.equals(msg.login))) {
            conn.close("Player with login " + msg.login + " is already in game");
            return;
        }
        Player player = new Player(conn, msg.login);
        players.put(conn.getId(), player);
        server.broadcast(in(conn), new JoinedGameMessage(player.info.id, map));
        if (map.isEmpty())
            return;

        Map<Integer, EntityInfo> newEntities = new HashMap<>();
        Collection<EntityState> changes = new HashSet<>();
        entities.forEach((i, e) -> {
            newEntities.put(i, e.info);
            changes.add(e.state);
        });
        player.notConfirmedMessages.add(new GameStateChange(newEntities, changes));
        ServerEntity playerEntity = entityFactory.createPlayerEntity(player);
        player.physics = createPlayerPhysics(playerEntity);
        bulletAppState.getPhysicsSpace().add(player.physics);
        entities.put(conn.getId(), playerEntity);
        addedEntities.add(player.info);
    }

    private CharacterControl createPlayerPhysics(ServerEntity entity) {
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(g_player_radius, g_player_height, g_player_axis);
        CharacterControl control = new CharacterControl(capsuleShape, g_player_step);
        control.setJumpSpeed(g_player_jumpheight);
        control.setFallSpeed(g_player_fallspeed);
        control.setGravity(g_player_gravity);
        control.setPhysicsLocation(g_spawn_point);
        Node node = new Node("player");
        node.setUserData("entity", entity);
        control.setSpatial(node);
        return control;
    }

    private void processRequest(Message message) {
        ActionMessage request = (ActionMessage) message;
        Player player = players.get(request.playerId);
        if (player == null || !player.isReady)
            return;
        if (player.hp > 0f) {
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
            if (pressed(request.buttons, Constants.Masks.FIRE_PRIMARY)) {
                if (player.axeCooldown > 0 && player.axeQuantity > 0) {
                    createProjectile(player.state.rot, player.state.pos, request.dir);
                    player.axeCooldown = -1f;
                    player.axeQuantity--;
                }
            }
            request.dir = new Vector3f(request.dir.x, 0f, request.dir.z);
            Vector3f left = request.dir.cross(up).multLocal(isStrafing);
            Vector3f walkDirection = request.dir.multLocal(isWalking).add(left);
            player.state.rot = request.rot;
            player.physics.setWalkDirection(walkDirection.normalize());
        }
    }

    private void createProjectile(Quaternion rot, Vector3f pos, Vector3f dir) {
        int id = ++lastId;
        ServerEntity axe = entityFactory.create(id, 2, rot, pos, dir);
        entities.put(id, axe);
        addedEntities.add(axe.info);

    }

    @Override
    public void removeEntity(int id, RigidBodyControl control) {
        entities.remove(id);
        players.remove(id);
        removedIds.add(id);
        bulletAppState.getPhysicsSpace().remove(control);
    }

    @Override
    public Player getPlayer(int id) {
        return players.get(id);
    }

    @Override
    public void sendHitSound() {
        hit = true;
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

    class UpdatingGameState extends AbstractAppState {
        @Override
        public void update(float tpf) {
            entities.values().forEach(e -> e.update.accept(tpf));
        }
    }
}
