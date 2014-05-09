package org.demoth.nogaem.client;

import com.jme3.app.SimpleApplication;
import com.jme3.input.*;
import com.jme3.input.controls.*;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.network.*;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
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
    private final        Map<Integer, Spatial>          entities = new HashMap<>();
    Client net;
    volatile long buttons;
    private  int  myId;
    private long     sentButtons   = 0;
    private Vector3f sentDirection = new Vector3f();

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
            console = new SwingConsole(s -> messages.add(new CommandMessage(s)));
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
        if (!host.isEmpty())
            connect();
    }


    @Override
    public void update() {
        super.update();
        if (!messages.isEmpty()) {
            Message message = messages.poll();
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
            net.start();
            log.info("Client started, sending login message...");
            net.send(new LoginRequestMessage(cl_user, cl_pass));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    // update
    private void addEntity(Entity entity) {
        if (entity.id == myId) {
//            log.info("Not adding myself");
            return;
        }
        Spatial model = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        model.scale(0.05f);
        model.setLocalTranslation(entity.state.pos);
        rootNode.attachChild(model);
        entities.put(entity.id, model);
//        log.info("Added entity " + entity.id);
    }

    private void removeEntity(int id) {
        Spatial sp = entities.get(id);
        if (sp == null)
            return;
        rootNode.detachChild(sp);
        log.info("Removed entity " + id);
    }

    // update
    private void processResponse(GameStateChange message) {
        if (message.index < lastReceivedMessage)
            return;
//        log.info("lastReceivedMessage=" + lastReceivedMessage + ". message.index=" + message.index);
        lastReceivedMessage = message.index;
        net.send(new Acknowledgement(message.index));
        if (message.removedIds != null)
            message.removedIds.forEach(this::removeEntity);
        if (message.added != null)
            message.added.forEach(this::addEntity);
        if (message.changes != null) {
            message.changes.forEach(change -> {
//                log.info("processing change " + change);
                if (change.id == myId) {
//                    log.info("moving me");
                    cam.setLocation(change.pos);
                } else {
                    Spatial spatial = entities.get(change.id);
                    if (spatial != null) {
//                        log.info("moving entity: " + change.id);
                        spatial.setLocalTranslation(change.pos.x, change.pos.y, change.pos.z);
//                        spatial.setLocalRotation(new Quaternion().fromAngles(change.view.x, change.view.y, change.view.z));
                    }
                }
            });
        }
    }

    // update
    private void logIn(JoinedGameMessage message) {
        myId = message.id;
        log.info("logged in successfuly: ");
        loadMap(message.map);
    }

    private void startSendingUpdates() {
        log.info("starting sending updates");
        sender = new Thread(() -> {
            while (true) {
                sendRequests();
                try {
                    Thread.sleep(cl_sleep);
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
        inputManager.addMapping(TOGGLE_CONSOLE, new KeyTrigger(KeyInput.KEY_F1));

        String buttonMappings[] = {WALK_FORWARD, WALK_BACKWARD, STRAFE_LEFT, STRAFE_RIGHT, JUMP, FIRE_PRIMARY};

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
        resetClient();
        if (net != null && net.isConnected())
            net.close();
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
        Spatial sceneModel = assetManager.loadModel(mapName);
        sceneModel.setLocalScale(g_scale);
        rootNode.attachChild(sceneModel);
        net.send(new Acknowledgement(-1));
        startSendingUpdates();
    }

    private void sendRequests() {
        // send nothing if player stays idle
        if (buttons == sentButtons && cam.getDirection().equals(sentDirection))
            return;
        net.send(new ActionMessage(buttons, cam.getDirection()));
        sentButtons = buttons;
        sentDirection = cam.getDirection();
    }
}