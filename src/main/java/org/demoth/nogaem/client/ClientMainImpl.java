package org.demoth.nogaem.client;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Message;
import com.jme3.network.Network;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.demoth.nogaem.client.controls.ClientEntity;
import org.demoth.nogaem.client.gui.ClientScreenController;
import org.demoth.nogaem.client.gui.Screens;
import org.demoth.nogaem.client.states.IngameState;
import org.demoth.nogaem.client.swing.SwingConsole;
import org.demoth.nogaem.common.*;
import org.demoth.nogaem.common.entities.EntityInfo;
import org.demoth.nogaem.common.messages.TextMessage;
import org.demoth.nogaem.common.messages.fromClient.*;
import org.demoth.nogaem.common.messages.fromServer.ChangeMapMessage;
import org.demoth.nogaem.common.messages.fromServer.GameStateChange;
import org.demoth.nogaem.common.messages.fromServer.JoinedGameMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.demoth.nogaem.common.Config.*;
import static org.demoth.nogaem.common.Constants.Actions.*;
import static org.demoth.nogaem.common.Util.trimFirstWord;

public class ClientMainImpl extends SimpleApplication implements ClientMain {
    private static final Logger log = LoggerFactory.getLogger(ClientMainImpl.class);
    final ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
    private final Map<Integer, ClientEntity> entities = new HashMap<>();
    Client net;
    volatile long buttons;
    private int myId;
    private long sentButtons = 0;
    private Vector3f sentDirection = new Vector3f();
    // interpolation
    private float camLerp;
    private Vector3f startPosition = new Vector3f();
    private Vector3f endPosition = new Vector3f();

    private SwingConsole console;
    private Thread sender;
    private long lastReceivedMessage;
    private ClientScreenController screenController;
    private IngameState ingameState;
    private ClientEntityFactory entityFactory;
    private AudioNode hitSound;
    private AudioNode explosion;
    private Node explosionEffect;
    private ParticleEmitter flash;
    private float explosionTime = 5;

    public static void run() {
        ClientMainImpl clientMain = new ClientMainImpl();
        clientMain.setShowSettings(true);
        clientMain.start();
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
        hitSound = new AudioNode(assetManager, "sounds/ekokubza123-punch.ogg", AudioData.DataType.Buffer);
        hitSound.setLooping(false);
        hitSound.setPositional(false);

        explosion = new AudioNode(assetManager, "sounds/hl-explode5.wav", AudioData.DataType.Buffer);
        explosion.setLooping(false);
        explosion.setPositional(false);

//        if (!host.isEmpty())
//            connect();
    }


    @Override
    public void update() {
        if (flash != null)
            if (explosionTime < 0) {
                flash.killAllParticles();
            } else {
                explosionTime -= 0.01f;
            }
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
        if (entity.info.typeId == 3) {
            // playSound
            explosion.playInstance();
            // play effect
            explosionEffect.setLocalTranslation(entity.endPosition);
            explosionTime = 5;
            flash.emitAllParticles();
        }
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
        screenController.setStats(message.stats);
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
                    endPosition = change.getPos().add(0f, g_player_height / 2, 0f);
                    camLerp = 0f;
                } else {
                    ClientEntity control = entities.get(change.id);
                    if (control != null) {
                        if (!control.initialized) {
                            control.getSpatial().setLocalTranslation(change.getPos());
                            control.getSpatial().setLocalRotation(change.getRot());
                            control.initialized = true;
                        } else {
                            if (change.getRot() != null && !change.getRot().equals(control.endRotation))
                                control.rotateLerp(change.getRot());
                            if (change.getPos() != null && !change.getPos().equals(control.endPosition))
                                control.moveLerp(change.getPos());
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
        log.info("logged in successfully: id=" + message.id);
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
        createFlash();
        renderManager.preloadScene(explosionEffect);
        rootNode.attachChild(explosionEffect);
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
        AudioNode audio = new AudioNode(assetManager, "sounds/fins-teleport.wav", AudioData.DataType.Buffer);
        audio.setLooping(false);
        audio.setPositional(false);
        rootNode.attachChild(audio);
        audio.play();
        AudioNode ambient = new AudioNode(assetManager, "sounds/xdimebagx-ambient.ogg", AudioData.DataType.Stream);
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

    private void createFlash() {
        explosionEffect = new Node("explosionFX");
        int COUNT_FACTOR = 1;
        float COUNT_FACTOR_F = 1f;

        boolean POINT_SPRITE = true;
        ParticleMesh.Type EMITTER_TYPE = POINT_SPRITE ? ParticleMesh.Type.Point : ParticleMesh.Type.Triangle;

        flash = new ParticleEmitter("Flash", EMITTER_TYPE, 24 * COUNT_FACTOR);
        flash.setSelectRandomImage(true);
        flash.setStartColor(new ColorRGBA(1f, 0.8f, 0.36f, (float) (1f / COUNT_FACTOR_F)));
        flash.setEndColor(new ColorRGBA(1f, 0.8f, 0.36f, 0f));
        flash.setStartSize(.1f);
        flash.setEndSize(3.0f);
        flash.setShape(new EmitterSphereShape(Vector3f.ZERO, .05f));
        flash.setParticlesPerSec(0);
        flash.setGravity(0, 0, 0);
        flash.setLowLife(.2f);
        flash.setHighLife(.2f);
        flash.setInitialVelocity(new Vector3f(0, 5f, 0));
        flash.setVelocityVariation(1);
        flash.setImagesX(5);
        flash.setImagesY(3);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture("textures/explosion-sprite.png"));
        mat.setBoolean("PointSprite", POINT_SPRITE);
        flash.setMaterial(mat);
        explosionEffect.attachChild(flash);
    }

}