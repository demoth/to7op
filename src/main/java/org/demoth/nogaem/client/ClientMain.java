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
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.Network;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import org.demoth.nogaem.common.Constants;
import org.demoth.nogaem.common.MessageRegistration;
import org.demoth.nogaem.common.messages.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import static org.demoth.nogaem.common.Config.*;
import static org.demoth.nogaem.common.Constants.Actions.*;

public class ClientMain extends SimpleApplication {
    private static final Logger log = Logger.getLogger("Client");
    Client net;
    volatile long buttons;
    ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
    private boolean running = true;
    private int myId;
    private Map<Integer, Spatial> players = new HashMap<>();
    private long sentButtons = 0;
    private Vector3f sentDirection = new Vector3f();

    public static void main(String... args) {
        new ClientMain().start(JmeContext.Type.Display);
    }

    @Override
    public void simpleInitApp() {
        MessageRegistration.registerAll();
//        stateManager.detach(stateManager.getState(FlyCamAppState.class));
//        stateManager.detach(stateManager.getState(DebugKeysAppState.class));
//        stateManager.detach(stateManager.getState(StatsAppState.class));
        log.info("Messages registered");
        try {
            net = Network.connectToServer(cl_server, sv_port);
            log.info("Connected");
        } catch (IOException e) {
            log.severe(e.getMessage());
            System.exit(1);
        }
        // queue all the messages
        net.addMessageListener((source, m) -> messages.add(m));
        log.info("Added message listeners, configuring inputs...");
        configureInputs();
        log.info("Configured inputs, starting...");
        net.start();
        log.info("Client started, sending login message...");
        net.send(new LoginRequestMessage("" + System.currentTimeMillis(), cl_pass));
    }

    @Override
    public void destroy() {
        if (net.isConnected())
            net.close();
        super.destroy();
    }

    @Override
    public void update() {
        super.update();
        if (!messages.isEmpty()) {
            Message message = messages.poll();
            if (message instanceof ResponseMessage)
                processResponse((ResponseMessage) message);
            else if (message instanceof PlayerJoinedMessage)
                addPlayer((PlayerJoinedMessage) message);
            else if (message instanceof LoggedInMessage)
                connect((LoggedInMessage) message);
            else if (message instanceof TextMessage)
                log.info(((TextMessage) message).text);
            else if (message instanceof DisconnectMessage) {
                int playerId = ((DisconnectMessage) message).playerId;
                if (playerId != myId) {
                    rootNode.detachChild(players.get(playerId));
                    players.remove(playerId);
                }
            }
        }
    }

    // update
    private void addPlayer(PlayerJoinedMessage message) {
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
                log.info("i (" + myId + ") moved to " + change.pos);
                cam.setLocation(change.pos);
            }
            else {
                Spatial spatial = players.get(change.playerId);
                if (spatial != null) {
                    log.info("player " + change.playerId + " moved to " + change.pos);
                    spatial.setLocalTranslation(change.pos.x, change.pos.y, change.pos.z);
                    spatial.setLocalRotation(new Quaternion().fromAngles(change.view.x, change.view.y, change.view.z));
                }
            }
        });
    }

    // update
    private void connect(LoggedInMessage message) {
        myId = message.id;
        log.info("LoggedInMessage received: " + message);
        loadMap(message.map);
        new Thread(() -> {
            while (running) {
                sendRequests();
                try {
                    Thread.sleep(cl_sleep);
                } catch (InterruptedException e) {
                    log.severe(e.getMessage());
                }
            }
        }).start();
    }

    // init
    private void configureInputs() {
        inputManager.addMapping(WALK_FORWARD, new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping(WALK_BACKWARD, new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping(STRAFE_LEFT, new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping(STRAFE_RIGHT, new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping(JUMP, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping(FIRE_PRIMARY, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        String buttonMappings[] = {WALK_FORWARD, WALK_BACKWARD, STRAFE_LEFT, STRAFE_RIGHT, JUMP, FIRE_PRIMARY};

        inputManager.addListener((ActionListener) this::pushButton, buttonMappings);
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            running = false;
            if (net.isConnected())
                net.send(new DisconnectMessage());
        }, INPUT_MAPPING_EXIT);
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

    private void loadMap(String map) {
        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(0);
        setUpLight();

        // We load the scene from the zip file and adjust its size.
        assetManager.registerLocator("data/town.zip", ZipLocator.class);
        Spatial sceneModel = assetManager.loadModel(map);
        sceneModel.setLocalScale(g_scale);
        rootNode.attachChild(sceneModel);
    }

    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(r_ambient));
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