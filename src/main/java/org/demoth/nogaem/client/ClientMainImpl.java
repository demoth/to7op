package org.demoth.nogaem.client;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.audio.AudioNode;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.*;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.network.*;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import org.demoth.nogaem.client.controls.ClientEntity;
import org.demoth.nogaem.client.gui.*;
import org.demoth.nogaem.client.states.IngameState;
import org.demoth.nogaem.client.swing.SwingConsole;
import org.demoth.nogaem.common.*;
import org.demoth.nogaem.common.entities.*;
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

public class ClientMainImpl extends SimpleApplication implements ClientMain {
    private static final Logger                         log      = LoggerFactory.getLogger(ClientMainImpl.class);
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

    private SwingConsole           console;
    private Thread                 sender;
    private long                   lastReceivedMessage;
    private ClientScreenController screenController;
    private IngameState            ingameState;
    private ClientEntityFactory    entityFactory;
    private AudioNode              hitSound;

    public static void run() {
        new ClientMainImpl().start(JmeContext.Type.Display);
    }

    @Override
    public void simpleInitApp() {
        log.info("Starting console...");
        try {
            console = new SwingConsole(s -> messages.add(new CommandMessage(s)));
        } catch (Exception e) {
            log.error("Could not create console! " + e.getMessage());
        }
        Util.registerMessages();
        log.info("Messages registered");
        Util.scanDataFolder(assetManager);

        configureInputs();
        ingameState = new IngameState(inputManager, flyCam, this);
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);
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
        screenController = Screens.createController(assetManager, inputManager, audioRenderer, guiViewPort, this);
        screenController.showMainMenu();
        log.info("GUI initialized");
        entityFactory = new ClientEntityFactory(assetManager, rootNode);
        hitSound = new AudioNode(assetManager, "sounds/ekokubza123-punch.ogg");
        hitSound.setLooping(false);
        hitSound.setPositional(false);

//        if (!host.isEmpty())
//            connect();
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
                    resetClient();
                }
            });
            net.start();
            log.info("Client started, sending login message...");
            net.send(new LoginRequestMessage(cl_user, cl_pass));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        screenController.checkConnectionResumeDisconnect();
    }

    // update
    private void addEntity(Integer id, EntityInfo entityInfo, boolean playSounds) {
        log.info("Adding " + entityInfo);
        if (id == myId || entities.containsKey(id))
            return;
        entities.put(id, entityFactory.createClientEntity(entityInfo, playSounds));
    }

    private void removeEntity(int id) {
        ClientEntity entity = entities.get(id);
        if (entity == null)
            return;
        Spatial sp = entity.getSpatial();
        if (sp == null)
            return;
        rootNode.detachChild(sp);
        entities.remove(id);
        log.info("Removed info " + id);
    }

    // update
    private void processResponse(GameStateChange message) {
        if (message.index < lastReceivedMessage) {
            log.info("skipping obsolete message");
            return;
        }
        lastReceivedMessage = message.index;
        net.send(new Acknowledgement(message.index));
        screenController.setAxesQuantity(message.axeQuantity);
        if (message.hitSound)
            hitSound.playInstance();
        if (message.removedIds != null)
            message.removedIds.forEach(this::removeEntity);
        if (message.added != null) {
            boolean playSounds = entities.size() != 0;
            message.added.forEach((t, u) -> addEntity(t, u, playSounds));
        }
        if (message.changes != null) {
            message.changes.forEach(change -> {
                log.trace("Moving: {0}", change);
                if (change.id == myId) {
                    startPosition = new Vector3f(cam.getLocation());
                    endPosition = change.pos.add(0f, g_player_height / 2, 0f);
                    camLerp = 0f;
                } else {
                    ClientEntity control = entities.get(change.id);
                    if (control != null) {
                        if (!control.initialized) {
                            control.getSpatial().setLocalTranslation(change.pos);
                            control.getSpatial().setLocalRotation(change.rot);
                            control.initialized = true;
                        } else {
                            if (change.rot != null && !change.rot.equals(control.endRotation))
                                control.rotateLerp(change.rot);
                            if (change.pos != null && !change.pos.equals(control.endPosition))
                                control.moveLerp(change.pos);
                        }
                    } else {
                        log.warn("No control found for " + change.id);
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
        inputManager.addMapping(TOGGLE_CONSOLE, new KeyTrigger(KeyInput.KEY_F1));
        inputManager.deleteMapping(INPUT_MAPPING_EXIT);
        inputManager.addMapping(TOGGLE_MENU, new KeyTrigger(KeyInput.KEY_ESCAPE));

        inputManager.addListener((ActionListener) this::toggleConsole, TOGGLE_CONSOLE);
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            if (isPressed) {
                screenController.toggleMainMenu();
            }
        }, TOGGLE_MENU);
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
        messages.clear();
        if (net != null && net.isConnected())
            net.close();
        screenController.checkConnectionResumeDisconnect();
    }

    private void resetClient() {
        lastReceivedMessage = 0;
        stopSendingUpdates();
        entities.clear();
        cam.setLocation(new Vector3f());
        camLerp = -1;
        startPosition = new Vector3f();
        endPosition = new Vector3f();
        rootNode.detachAllChildren();
        rootNode.getWorldLightList().clear();
        rootNode.getLocalLightList().clear();
        viewPort.setBackgroundColor(new ColorRGBA(0f, 0f, 0f, 1f));
        stateManager.detach(ingameState);
    }

    private void toggleConsole(String actionName, boolean pressed, float tpf) {
        if (!pressed)
            console.setVisible(!console.isVisible());
    }

    @Override
    public void pushButton(String actionName, boolean pressed, float tpf) {
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
        Util.attachCoordinateAxes(rootNode, assetManager);
        stateManager.attach(ingameState);
        screenController.resume();
        AudioNode audio = new AudioNode(assetManager, "sounds/fins-teleport.wav");
        audio.setLooping(false);
        audio.setPositional(false);
        rootNode.attachChild(audio);
        audio.play();
        AudioNode ambient = new AudioNode(assetManager, "sounds/xdimebagx-ambient.ogg");
        ambient.setPositional(false);
        ambient.setLooping(true);
        rootNode.attachChild(ambient);
        ambient.play();
        startSendingUpdates();
        net.send(new Acknowledgement(-1));
    }

    private void sendRequests() {
        // send nothing if player stays idle
        if (buttons == sentButtons && cam.getDirection().equals(sentDirection))
            return;
        net.send(new ActionMessage(buttons, cam.getDirection(), cam.getRotation()));
        sentButtons = buttons;
        sentDirection = cam.getDirection();
    }


    @Override
    public void enqueue(String cmd) {
        messages.add(new CommandMessage(cmd));
    }

    @Override
    public void enableIngameState(boolean enable) {
        if (enable)
            stateManager.detach(ingameState);
        else
            stateManager.attach(ingameState);
    }

    @Override
    public boolean isConnected() {
        return net != null && net.isConnected();
    }
}