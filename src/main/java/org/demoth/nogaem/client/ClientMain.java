package org.demoth.nogaem.client;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.Network;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import org.demoth.nogaem.common.*;
import org.demoth.nogaem.common.messages.DisconnectMessage;
import org.demoth.nogaem.common.messages.TextMessage;
import org.demoth.nogaem.common.messages.client.LoginRequestMessage;
import org.demoth.nogaem.common.messages.client.RconMessage;
import org.demoth.nogaem.common.messages.client.RequestMessage;
import org.demoth.nogaem.common.messages.server.ChangeMapMessage;
import org.demoth.nogaem.common.messages.server.JoinedGameMessage;
import org.demoth.nogaem.common.messages.server.NewPlayerJoinedMessage;
import org.demoth.nogaem.common.messages.server.ResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.demoth.nogaem.common.Config.*;
import static org.demoth.nogaem.common.Constants.Actions.*;
import static org.demoth.nogaem.common.Util.*;

public class ClientMain extends SimpleApplication {
    private static final Logger log = LoggerFactory.getLogger(ClientMain.class);
    Client net;
    volatile long buttons;
    ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
    private boolean running = true;
    private int myId;
    private Map<Integer, Spatial> players       = new HashMap<>();
    private long                  sentButtons   = 0;
    private Vector3f              sentDirection = new Vector3f();

    private SwingConsole console;
    private Thread       sender;

    public static void run() {
        new ClientMain().start(JmeContext.Type.Display);
    }

    @Override
    public void simpleInitApp() {
        log.info("Starting console...");
        try {
            console = new SwingConsole(this::execCommand);
        } catch (Exception e) {
            log.error("Could not create console!");
        }
        MessageRegistration.registerAll();
//        stateManager.detach(stateManager.getState(FlyCamAppState.class));
//        stateManager.detach(stateManager.getState(DebugKeysAppState.class));
//        stateManager.detach(stateManager.getState(StatsAppState.class));
        log.info("Messages registered");
        try {
            net = Network.connectToServer(cl_server, sv_port);
            log.info("Connected to " + cl_server + ':' + sv_port);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            System.exit(1);
        }
        // We load the scene from the zip file and adjust its size.
        assetManager.registerLocator("data/town.zip", ZipLocator.class);
        // queue all the messages
        net.addMessageListener((source, m) -> messages.add(m));
        log.info("Added message listeners, configuring inputs...");
        configureInputs();
        log.info("Configured inputs, starting...");
        net.start();
        log.info("Client started, sending login message...");
        net.send(new LoginRequestMessage(cl_user, cl_pass));
    }

    @Override
    public void destroy() {
        stopSendingUpdates();
        if (net.isConnected()) {
            log.info("Closing connection...");
            net.close();
        }
        if (console != null) {
            log.info("Closing console...");
            console.dispose();
        }
        super.destroy();
    }

    @Override
    public void update() {
        super.update();
        if (!messages.isEmpty()) {
            Message message = messages.poll();
            if (message instanceof ResponseMessage)
                processResponse((ResponseMessage) message);
            else if (message instanceof NewPlayerJoinedMessage)
                addPlayer((NewPlayerJoinedMessage) message);
            else if (message instanceof JoinedGameMessage)
                connect((JoinedGameMessage) message);
            else if (message instanceof ChangeMapMessage)
                loadMap(((ChangeMapMessage) message).mapName);
            else if (message instanceof TextMessage) {
                log.info(((TextMessage) message).text);
                console.print(((TextMessage) message).text);
            } else if (message instanceof DisconnectMessage) {
                int playerId = ((DisconnectMessage) message).playerId;
                if (playerId != myId) {
                    rootNode.detachChild(players.get(playerId));
                    players.remove(playerId);
                }
            }
        }
    }

    // update
    private void addPlayer(NewPlayerJoinedMessage message) {
        if (message.id == myId)
            return;
        Spatial model = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        model.scale(0.1f);
        model.setLocalTranslation(message.location);
        rootNode.attachChild(model);
        players.put(message.id, model);
    }

    // update
    private void processResponse(ResponseMessage message) {
        message.changes.forEach(change -> {
            if (change.playerId == myId) {
                cam.setLocation(change.pos);
            } else {
                Spatial spatial = players.get(change.playerId);
                if (spatial != null) {
                    spatial.setLocalTranslation(change.pos.x, change.pos.y, change.pos.z);
                    spatial.setLocalRotation(new Quaternion().fromAngles(change.view.x, change.view.y, change.view.z));
                }
            }
        });
    }

    // update
    private void connect(JoinedGameMessage message) {
        myId = message.id;
        log.info("JoinedGameMessage received: " + message);
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
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            running = false;
            if (net.isConnected())
                net.send(new DisconnectMessage());
        }, INPUT_MAPPING_EXIT);
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
                    break;
                case disconnect:
                    break;
                case rcon:
                    if (words.length < 2)
                        break;
                    String args;
                    if (words.length > 2)
                        args = trimFirstWord(trimFirstWord(trimmed));
                    else
                        args = "";
                    net.send(new RconMessage(RconCommand.valueOf(words[1]), args, rcon_pass));
                    break;
                case set:
                    Config.cvars.get(words[0]).set(trimFirstWord(trimmed));
                    break;
                case say:
                    net.send(new TextMessage(trimFirstWord(trimmed)));
                    break;
            }
        } catch (Exception e) {
            log.info("Wrong command: ", cmdStr);
            console.print("Wrong command: " + cmdStr + '(' + e.getMessage() + ')');
        }
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
        log.info("Changing map:" + mapName);
        stopSendingUpdates();
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(0);
        rootNode.detachAllChildren();
        setUpLight();
        Spatial sceneModel = assetManager.loadModel(mapName);
        sceneModel.setLocalScale(g_scale);
        rootNode.attachChild(sceneModel);
        startSendingUpdates();
    }

    private void stopSendingUpdates() {
        if (sender != null)
            sender.interrupt();
    }

    private void setUpLight() {
        rootNode.getLocalLightList().clear();
        rootNode.getWorldLightList().clear();
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White);
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
    }

    private void sendRequests() {
        // send nothing if player stays idle
        if (buttons == sentButtons && cam.getDirection().equals(sentDirection))
            return;
        net.send(new RequestMessage(buttons, cam.getDirection()));
        sentButtons = buttons;
        sentDirection = cam.getDirection();
    }
}