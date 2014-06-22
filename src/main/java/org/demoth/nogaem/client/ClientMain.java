package org.demoth.nogaem.client;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.font.BitmapText;
import com.jme3.input.*;
import com.jme3.input.controls.*;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.network.*;
import com.jme3.scene.*;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.*;
import com.jme3.system.JmeContext;
import org.demoth.nogaem.client.controls.ClientEntity;
import org.demoth.nogaem.client.swing.SwingConsole;
import org.demoth.nogaem.common.*;
import org.demoth.nogaem.common.entities.Entity;
import org.demoth.nogaem.common.messages.TextMessage;
import org.demoth.nogaem.common.messages.fromClient.*;
import org.demoth.nogaem.common.messages.fromServer.*;
import org.slf4j.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.demoth.nogaem.common.Config.*;
import static org.demoth.nogaem.common.Constants.Actions.*;
import static org.demoth.nogaem.common.Util.trimFirstWord;

public class ClientMain extends SimpleApplication {
    private static final Logger                         log      = LoggerFactory.getLogger(ClientMain.class);
    final                ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
    private final        Map<Integer, ClientEntity>     entities = new HashMap<>();
    Client net;
    volatile long buttons;
    private  int  myId;
    private long     sentButtons   = 0;
    private Vector3f sentDirection = new Vector3f();
    // interpolation
    private float camLerp;
    private Vector3f startPosition = new Vector3f();
    private Vector3f endPosition   = new Vector3f();


    private SwingConsole console;
    private Thread       sender;
    private long         lastReceivedMessage;

    public static void run() {
        new ClientMain().start(JmeContext.Type.Display);
    }

    @Override
    public void simpleInitApp() {
        log.info("Starting console...");
        try {
//            console = new SwingConsole(s -> messages.add(new CommandMessage(s)));
        } catch (Exception e) {
            log.error("Could not create console! " + e.getMessage());
        }
        Util.registerMessages();
        log.info("Messages registered");
        // We load the scene from the zip file and adjust its size.
        Util.scanDataFolder(assetManager);

        // todo move to state
        configureInputs();
        flyCam.setMoveSpeed(0);
        stateManager.attach(new AbstractAppState() {
            @Override
            public void update(float tpf) {
                if (camLerp >= 0) {
                    float scale = camLerp / cl_lerp;
                    cam.setLocation(FastMath.interpolateLinear(scale, startPosition, endPosition));
                    camLerp += tpf;
                    if (camLerp > cl_lerp && camLerp != 0f)
                        camLerp = -1f;
                }
            }
        });
        if (!host.isEmpty())
            connect();
    }


    @Override
    public void update() {
        super.update();
        if (!messages.isEmpty()) {
            Message message = messages.poll();
            log.trace("Received: {0}", message);
            if (message instanceof GameStateChange)
                processResponse((GameStateChange) message);
            else if (message instanceof JoinedGameMessage)
                logIn((JoinedGameMessage) message);
            else if (message instanceof ChangeMapMessage)
                loadMap(((ChangeMapMessage) message).mapName);
            else if (message instanceof TextMessage)
                log.info(((TextMessage) message).text);
            else if (message instanceof CommandMessage)
                execCommand(((CommandMessage) message).cmd);
        }
    }

    @Override
    public void destroy() {
        disconnect();
        if (console != null) {
            log.info("Closing console...");
            console.dispose();
        }
        super.destroy();
    }

    private void connect() {
        resetClient();
        try {
            log.info("Connecting to " + host + ':' + port);
            net = Network.connectToServer(host, port);
            net.addMessageListener((source, m) -> messages.add(m));
            net.addClientStateListener(new ClientStateListener() {
                @Override
                public void clientConnected(Client c) {
                    log.info("Connection initialized");
                }

                @Override
                public void clientDisconnected(Client c, DisconnectInfo info) {
                    stopSendingUpdates();
                    stop();
                }
            });
            net.start();
            log.info("Client started, sending login message...");
            net.send(new LoginRequestMessage(cl_user, cl_pass));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    // update
    private void addEntity(Integer id, Entity entity) {
        log.info("Adding " + entity);
        if (id == myId || entities.containsKey(id))
            return;
        Node node = new Node(entity.name);
        Spatial model;
        switch (entity.modelName) {
            case "player":
                model = assetManager.loadModel("models/player.blend");
                model.move(0f, -g_player_height / 2, 0f);
                break;
            case "axe":
            default:
                model = assetManager.loadModel("models/axe.blend");
        }
        float size = entity.size;
        Geometry bounds = new Geometry(entity.name + "BB", new Box(size, size, size));
        bounds.setMaterial(createBoundBoxMaterial());
        node.attachChild(model);
        node.attachChild(bounds);
        Node textNode = new Node();
        BitmapText text = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
        text.setText(entity.name);
        text.setSize(1f);
        text.move(-text.getLineWidth() / 2, size + text.getLineHeight(), 0f);
        textNode.attachChild(text);
        textNode.addControl(new BillboardControl());
        node.attachChild(textNode);
        attachCoordinateAxes(node);

        if (entity.state != null) {
            node.setLocalTranslation(entity.state.pos.x, entity.state.pos.y - 5f, entity.state.pos.z);
            node.setLocalRotation(entity.state.rot);
        }
        entities.put(id, new ClientEntity(entity, node, model));
        rootNode.attachChild(node);
    }

    private Material createBoundBoxMaterial() {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Blue);
        return mat;
    }

    private void removeEntity(int id) {
        Spatial sp = entities.get(id).getSpatial();
        if (sp == null)
            return;
        rootNode.detachChild(sp);
        entities.remove(id);
        log.info("Removed entity " + id);
    }

    // update
    private void processResponse(GameStateChange message) {
        if (message.index < lastReceivedMessage) {
            log.info("skipping obsolete message");
            return;
        }
        lastReceivedMessage = message.index;
        net.send(new Acknowledgement(message.index));
        if (message.removedIds != null)
            message.removedIds.forEach(this::removeEntity);
        if (message.added != null)
            message.added.forEach(this::addEntity);
        if (message.changes != null) {
            message.changes.forEach(change -> {
                if (change.id == myId) {
                    startPosition = new Vector3f(cam.getLocation());
                    endPosition = change.pos.add(0f, g_player_height/2, 0f);
                    camLerp = 0f;
                } else {
                    ClientEntity contol = entities.get(change.id);
                    if (contol != null) {
                        if (change.rot != null && !change.rot.equals(contol.endRotation)) {
                            contol.rotateLerp(change.rot);
                        }
                        if (change.pos != null && !change.pos.equals(contol.endPosition)) {
                            contol.moveLerp(change.pos);
                        }
                    }
                }
            });
        }
    }

    // update
    private void logIn(JoinedGameMessage message) {
        myId = message.id;
        log.info("logged in successfuly: id=" + message.id);
        if (!message.map.isEmpty())
            loadMap(message.map);
    }

    private void startSendingUpdates() {
        log.info("starting sending updates");
        sender = new Thread(() -> {
            while (true) {
                long started = System.currentTimeMillis();
                sendRequests();
                long toSleep = cl_sleep + started - System.currentTimeMillis();
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

    // init
    private void configureInputs() {
        inputManager.addMapping(WALK_FORWARD, new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping(WALK_BACKWARD, new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping(STRAFE_LEFT, new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping(STRAFE_RIGHT, new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping(JUMP, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping(FIRE_PRIMARY, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(FIRE_SECONDARY, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping(TOGGLE_CONSOLE, new KeyTrigger(KeyInput.KEY_F1));

        String buttonMappings[] = {WALK_FORWARD, WALK_BACKWARD, STRAFE_LEFT, STRAFE_RIGHT, JUMP, FIRE_PRIMARY, FIRE_SECONDARY};

        inputManager.addListener((ActionListener) this::toggleConsole, TOGGLE_CONSOLE);
        inputManager.addListener((ActionListener) this::pushButton, buttonMappings);
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> stop(), INPUT_MAPPING_EXIT);
    }

    private void execCommand(String cmdStr) {
        try {
            String trimmed = cmdStr.trim();
            Command cmd;
            String[] words = {""};
            if (trimmed.contains(" ")) {
                words = trimmed.split(" ");
                cmd = Command.valueOf(words[0]);
            } else {
                cmd = Command.valueOf(trimmed);
            }
            switch (cmd) {
                case quit:
                    stop();
                    break;
                case disconnect:
                    disconnect();
                    break;
                case connect:
                    switch (words.length) {
                        case 3:
                            port = Integer.valueOf(words[2]);
                        case 2:
                            host = words[1];
                        case 1:
                            disconnect();
                            connect();
                    }
                    break;
                case rcon:
                    if (words.length < 2)
                        break;
                    String args;
                    if (words.length > 2)
                        args = trimFirstWord(trimFirstWord(trimmed));
                    else
                        args = "";
                    RconMessage message = new RconMessage(RconCommand.valueOf(words[1]), args, rcon_pass);
                    log.info("Sending rcon message: " + message);
                    stopSendingUpdates();
                    net.send(message);
                    break;
                case set:
                    Config.cvars.get(words[0]).set(trimFirstWord(trimmed));
                    break;
                case say:
                    net.send(new TextMessage(trimFirstWord(trimmed)));
                    break;
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    private void disconnect() {
        if (net != null && net.isConnected())
            net.close();
        resetClient();
    }

    private void resetClient() {
        lastReceivedMessage = 0;
        stopSendingUpdates();
        rootNode.detachAllChildren();
        rootNode.getWorldLightList().clear();
        rootNode.getLocalLightList().clear();
        viewPort.setBackgroundColor(new ColorRGBA(0f, 0f, 0f, 1f));
    }

    private void toggleConsole(String actionName, boolean pressed, float tpf) {
        if (!pressed)
            console.setVisible(!console.isVisible());
    }

    private void pushButton(String actionName, boolean pressed, float tpf) {
        switch (actionName) {
            case WALK_FORWARD:
                buttons = pressOrRelease(buttons, pressed, Constants.Masks.WALK_FORWARD);
                break;
            case WALK_BACKWARD:
                buttons = pressOrRelease(buttons, pressed, Constants.Masks.WALK_BACKWARD);
                break;
            case STRAFE_LEFT:
                buttons = pressOrRelease(buttons, pressed, Constants.Masks.STRAFE_LEFT);
                break;
            case STRAFE_RIGHT:
                buttons = pressOrRelease(buttons, pressed, Constants.Masks.STRAFE_RIGHT);
                break;
            case JUMP:
                buttons = pressOrRelease(buttons, pressed, Constants.Masks.JUMP);
                break;
            case FIRE_PRIMARY:
                buttons = pressOrRelease(buttons, pressed, Constants.Masks.FIRE_PRIMARY);
                break;
            case FIRE_SECONDARY:
                buttons = pressOrRelease(buttons, pressed, Constants.Masks.FIRE_SECONDARY);
        }
    }

    private long pressOrRelease(long buttons, boolean pressed, long button) {
        if (pressed)
            return buttons | button;
        else
            return buttons & ~button;
    }

    private void loadMap(String mapName) {
        resetClient();
        log.info("Changing map:" + mapName);
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
        Spatial sceneModel = assetManager.loadModel("maps/" + mapName);
        sceneModel.setLocalScale(g_scale);
        rootNode.attachChild(sceneModel);
        net.send(new Acknowledgement(-1));
        attachCoordinateAxes(rootNode);
        startSendingUpdates();
    }

    private void sendRequests() {
        // send nothing if player stays idle
        if (buttons == sentButtons && cam.getDirection().equals(sentDirection))
            return;
        net.send(new ActionMessage(buttons, cam.getDirection(), cam.getRotation()));
        sentButtons = buttons;
        sentDirection = cam.getDirection();
    }

    private void attachCoordinateAxes(Node node){
        Arrow arrow = new Arrow(Vector3f.UNIT_X);
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, node, ColorRGBA.Red);

        arrow = new Arrow(Vector3f.UNIT_Y);
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, node, ColorRGBA.Green);

        arrow = new Arrow(Vector3f.UNIT_Z);
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, node, ColorRGBA.Blue);
    }

    private Geometry putShape(Mesh shape, Node node, ColorRGBA color){
        Geometry g = new Geometry("coordinate axis", shape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", color);
        g.setMaterial(mat);
        node.attachChild(g);
        return g;
    }

}