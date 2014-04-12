package client;

import com.jme3.app.SimpleApplication;
import com.jme3.input.*;
import com.jme3.input.controls.*;
import com.jme3.math.*;
import com.jme3.network.*;
import com.jme3.system.JmeContext;
import common.*;
import common.messages.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static common.Constants.Actions.*;

public class ClientMain extends SimpleApplication {
    private static final Logger log = Logger.getLogger("Client");
    Client connection;
    volatile long buttons;
    volatile Vector3f view = new Vector3f(0f, 0f, 0f);
    ConcurrentLinkedQueue<ResponseMessage> messages = new ConcurrentLinkedQueue<>();
    private boolean running = true;

    public static void main(String... args) {
        new ClientMain().start(JmeContext.Type.Headless);
    }

    @Override
    public void simpleInitApp() {
        MessageRegistration.registerAll();
        log.info("Messages registered");
        try {
            connection = Network.connectToServer("127.0.0.1", 5555);
            log.info("Connected");
        } catch (IOException e) {
            log.severe(e.getMessage());
            System.exit(1);
        }
        addMessageListeners();
        log.info("Added message listeners, configuring inputs...");
        configureInputs();
        log.info("Configured inputs, starting...");
        connection.start();
        log.info("Client started, sending login message...");
        connection.send(new LoginMessage("demoth", "cadaver", 0, System.currentTimeMillis()));
    }

    @Override
    public void update() {
        super.update();
        if (!messages.isEmpty())
            processMessage(messages.poll());
    }

    @Override
    public void destroy() {
        connection.close();
        super.destroy();
    }

    private void addMessageListeners() {
        connection.addMessageListener(this::connect, LoginMessage.class);
        connection.addMessageListener(this::printTextMessage, TextMessage.class);
        connection.addMessageListener(this::addResponseMessage, ResponseMessage.class);
    }

    private void addResponseMessage(Client client, Message message) {
        log.info("ResponseMessage received: " + message);
        messages.add((ResponseMessage) message);
    }

    private void processMessage(ResponseMessage message) {
        log.info("ResponseMessage processed: " + message);
    }

    private void configureInputs() {
        inputManager.addMapping(WALK_FORWARD, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping(LOOK_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping(LOOK_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping(LOOK_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping(LOOK_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, false));

        String buttonMappings[] = {WALK_FORWARD, WALK_BACKWARD, STRAFE_LEFT, STRAFE_RIGHT, JUMP, FIRE_PRIMARY};
        String mouseMappings[] = {LOOK_UP, LOOK_DOWN, LOOK_LEFT, LOOK_RIGHT};

        inputManager.addListener((ActionListener) this::pushButton, buttonMappings);
        inputManager.addListener((AnalogListener) this::updateLookAngle, mouseMappings);
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

    private void connect(Client client, Message message) {
        log.info("LoginMessage received: " + message);
        new Thread(() -> {
            while (running) {
                sendCommands();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void sendCommands() {
        connection.send(new RequestMessage(new Date()));
    }

    private void printTextMessage(Client client, Message message) {
        log.info("TextMessage received: " + message);
    }
}