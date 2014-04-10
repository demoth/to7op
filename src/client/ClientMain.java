package client;

import com.jme3.app.SimpleApplication;
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
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import static common.Constants.Actions.*;

public class ClientMain extends SimpleApplication {
    private static final Logger log = Logger.getLogger("Client");
    Client connection;
    volatile long buttons;
    volatile long lamt; // last acknowledged message time (from server)
    volatile Vector3f view = new Vector3f(0f, 0f, 0f);
    ClientStateMessage currentState;
    Integer myId;
    Spatial player;
    ConcurrentLinkedQueue<ClientStateMessage> messages = new ConcurrentLinkedQueue<>();
    boolean running = true;
    private Spatial sceneModel;

    public static void main(String... args) {
        new ClientMain().start(JmeContext.Type.Headless);
    }

    @Override
    public void update() {
        super.update();
        if (!messages.isEmpty()) {
            Message message = messages.poll();
            log.info("ClientStateMessage processed: " + message);
        }
    }

    @Override
    public void simpleInitApp() {
        MessageRegistration.registerAll();
        try {
            //assetManager.registerLocator("town.zip", ZipLocator.class);
            //sceneModel = assetManager.loadModel("main.scene");
            //sceneModel.setLocalScale(2f);
            //rootNode.attachChild(sceneModel);
            //setUpLight();

            connection = Network.connectToServer("127.0.0.1", 5555);
        } catch (IOException e) {
            log.severe(e.getMessage());
            System.exit(1);
        }
        addMessageListeners();
        configureInputs();
        connection.start();
        connection.send(new LoginMessage("demoth", "cadaver", 0, System.currentTimeMillis()));
    }

    private void addMessageListeners() {
        connection.addMessageListener((client, message) -> {
            log.info("LoginMessage received: " + message);
            new Sender().start();
        }, LoginMessage.class);
        connection.addMessageListener((client, message) -> log.info("TextMessage received: " + message), TextMessage.class);
        connection.addMessageListener((client, message) -> {
            log.info("ClientStateMessage received: " + message);
            messages.add((ClientStateMessage) message);
        }, ClientStateMessage.class);
    }

    private void configureInputs() {
        inputManager.addMapping(WALK_FORWARD, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping(LOOK_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping(LOOK_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping(LOOK_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping(LOOK_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, false));

        inputManager.addListener((ActionListener) this::pushButton
                , WALK_FORWARD, WALK_BACKWARD, STRAFE_LEFT, STRAFE_RIGHT, JUMP, FIRE_PRIMARY);
        inputManager.addListener((AnalogListener) this::updateLookAngle, LOOK_UP, LOOK_DOWN, LOOK_LEFT, LOOK_RIGHT);
    }

    private void pushButton(String actionName, boolean pressed, float tpf) {
        if (pressed)
            switch (actionName) {
                case WALK_FORWARD:
                    buttons &= Constants.Masks.WALK_FORWARD;
                    break;
                case WALK_BACKWARD:
                    buttons &= Constants.Masks.WALK_BACKWARD;
                    break;
                case STRAFE_LEFT:
                    buttons &= Constants.Masks.STRAFE_LEFT;
                    break;
                case STRAFE_RIGHT:
                    buttons &= Constants.Masks.STRAFE_RIGHT;
                    break;
                case JUMP:
                    buttons &= Constants.Masks.JUMP;
                    break;
                case FIRE_PRIMARY:
                    buttons &= Constants.Masks.FIRE_PRIMARY;
            }
    }

    private void updateLookAngle(String name, float value, float tpf) {
        switch (name) {
            case LOOK_UP:
                view.y += value * tpf;
                break;
            case LOOK_DOWN:
                view.y -= value * tpf;
                break;
            case LOOK_LEFT:
                view.x -= value * tpf;
                break;
            case LOOK_RIGHT:
                view.x += value * tpf;
                break;
        }
    }

    @Override
    public void destroy() {
        connection.close();
        super.destroy();
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

    private class Sender extends Thread {
        @Override
        public void run() {
            while (running) {
                connection.send(new ActionMessage(new Date()));
                try {
                    sleep(1000);
                } catch (InterruptedException ignored) {
                    running = false;
                }
            }
        }
    }
}