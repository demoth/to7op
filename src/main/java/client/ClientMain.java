package client;

import com.jme3.app.*;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.input.*;
import com.jme3.input.controls.*;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.network.*;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;
import common.*;
import common.messages.*;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static common.Constants.Actions.*;

public class ClientMain extends SimpleApplication {
    private static final Logger log = Logger.getLogger("Client");
    Client net;
    volatile long buttons;
    volatile Vector3f view = new Vector3f(0f, 0f, 0f);
    ConcurrentLinkedQueue<ResponseMessage> messages = new ConcurrentLinkedQueue<>();
    private boolean running = true;

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
            net = Network.connectToServer("127.0.0.1", 5555);
            log.info("Connected");
        } catch (IOException e) {
            log.severe(e.getMessage());
            System.exit(1);
        }
        addMessageListeners();
        log.info("Added message listeners, configuring inputs...");
        configureInputs();
        log.info("Configured inputs, starting...");
        net.start();
        // todo should be moved to update loop
        initWorld();
        log.info("Client started, sending login message...");
        net.send(new LoginMessage("demoth", "cadaver", 0, System.currentTimeMillis()));
    }

    @Override
    public void update() {
        super.update();
        if (!messages.isEmpty())
            processMessage(messages.poll());
    }

    @Override
    public void destroy() {
        net.close();
        super.destroy();
    }

    private void addMessageListeners() {
        net.addMessageListener(this::connect, LoginMessage.class);
        net.addMessageListener(this::printTextMessage, TextMessage.class);
        net.addMessageListener(this::addResponseMessage, ResponseMessage.class);
    }

    private void addResponseMessage(Client client, Message message) {
        log.info("ResponseMessage received: " + message);
        messages.add((ResponseMessage) message);
    }

    private void processMessage(ResponseMessage message) {
        log.info("ResponseMessage processed: " + message);
        cam.setLocation(message.position);
    }

    private void configureInputs() {
        inputManager.addMapping(WALK_FORWARD, new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping(WALK_BACKWARD, new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping(STRAFE_LEFT, new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping(STRAFE_RIGHT, new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping(JUMP, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping(FIRE_PRIMARY, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        String buttonMappings[] = {WALK_FORWARD, WALK_BACKWARD, STRAFE_LEFT, STRAFE_RIGHT, JUMP, FIRE_PRIMARY};

        inputManager.addListener((ActionListener) this::pushButton, buttonMappings);
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

    private void connect(Client client, Message message) {
        log.info("LoginMessage received: " + message);
//        initWorld();
        new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(Constants.updateRate);
                } catch (InterruptedException e) {
                    log.severe(e.getMessage());
                }
                sendRequests();
            }
        }).start();
    }

    private void initWorld() {
        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(100);
        setUpLight();

        // We load the scene from the zip file and adjust its size.
        assetManager.registerLocator("town.zip", ZipLocator.class);
        Spatial sceneModel = assetManager.loadModel("main.scene");
        sceneModel.setLocalScale(2f);
        rootNode.attachChild(sceneModel);
    }

    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
    }

    private void sendRequests() {
        net.send(new RequestMessage(buttons, cam.getDirection()));
    }

    private void printTextMessage(Client client, Message message) {
        log.info("TextMessage received: " + message);
    }
}