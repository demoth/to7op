package client;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.serializing.Serializer;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.JmeContext;
import common.ClientState;
import common.Constants;
import common.messages.ActionMessage;
import common.messages.ClientStateMessage;
import common.messages.LoginMessage;
import common.messages.TextMessage;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.soap.Node;

import static common.Constants.Actions.*;

public class ClientMain extends SimpleApplication {

    Client client;
    volatile long     buttons;
    volatile long     lamt; // last acknowledged message time (from server)
    volatile Vector3f view;

    ClientState currentState;
    Integer     myId;
    Spatial     player;
    ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
    boolean                        running  = true;


    @Override
    public void simpleInitApp() {
        registerMessages();
        try {
            client = Network.connectToServer("127.0.0.1", 5555);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            running = false;
            System.exit(1);
        }
        addMessageListeners();
        configureInputs();
        client.start();
        // set up message sender thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    client.send(new ActionMessage(view, buttons, lamt));
                    buttons = 0l;
                    try {
                        wait(50);
                    } catch (InterruptedException ignored) {
                        running = false;
                    }
                }
            }
        }).start();
        client.send(new LoginMessage("demoth", "cadaver", 0));
    }

    private void addMessageListeners() {
        client.addMessageListener(new MessageListener<Client>() {
            @Override
            public void messageReceived(Client client, Message message) {
                if (message instanceof TextMessage) {
                    System.out.println(((TextMessage) message).text);
                }
            }
        }, LoginMessage.class);
        client.addMessageListener(new MessageListener<Client>() {
            @Override
            public void messageReceived(Client client, Message message) {
                if (message instanceof ClientStateMessage) {
                    messages.add(message);
                }
            }
        }, ClientStateMessage.class);
    }

    private void configureInputs() {
        inputManager.addMapping(WALK_FORWARD, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping(LOOK_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping(LOOK_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping(LOOK_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping(LOOK_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, false));

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean pressed, float tpf) {
                pushButton(name, pressed);
            }
        }, WALK_FORWARD, WALK_BACKWARD, STRAFE_LEFT, STRAFE_RIGHT, JUMP, FIRE_PRIMARY);
        inputManager.addListener(new AnalogListener() {
            @Override
            public void onAnalog(String name, float value, float tpf) {
                updateLookAngle(name, value, tpf);
            }
        }, LOOK_UP, LOOK_DOWN, LOOK_LEFT, LOOK_RIGHT);
    }

    private void registerMessages() {
        Serializer.registerClass(LoginMessage.class);
        Serializer.registerClass(ActionMessage.class);
        Serializer.registerClass(TextMessage.class);
        Serializer.registerClass(ClientStateMessage.class);
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

    private void pushButton(String actionName, boolean pressed) {
        if (pressed)
            switch (actionName) {
                case WALK_FORWARD:
                    buttons &= Constants.Masks.WALK_FORWARD;
                    break;
                case WALK_BACKWARD:
                    buttons &= Constants.Masks.WALK_BACKWARD;
                case STRAFE_LEFT:
                    buttons &= Constants.Masks.STRAFE_LEFT;
                case STRAFE_RIGHT:
                    buttons &= Constants.Masks.STRAFE_RIGHT;
                case JUMP:
                    buttons &= Constants.Masks.JUMP;
                case FIRE_PRIMARY:
                    buttons &= Constants.Masks.FIRE_PRIMARY;
            }
    }

    @Override
    public void update() {
        if (!messages.isEmpty()) {
            Message message = messages.poll();
            if (message instanceof ClientStateMessage) {
                ClientStateMessage m = (ClientStateMessage) message;
                ClientState diff = m.diff;
                lamt = diff.time;
                player.move(diff.position);
                player.rotate(diff.view.x, diff.view.y, diff.view.z);
            }
        }
        super.update();
    }

    @Override
    public void destroy() {
        client.close();
        super.destroy();
    }

    public static void main(String[] args) {
        new ClientMain().start(JmeContext.Type.Display);
    }
}
//Box b = new Box(Vector3f.ZERO, 1, 1, 1);
//geom = new Geometry("Box", b);
//        Material mat = new Material(assetManager,
//        "Common/MatDefs/Misc/Unshaded.j3md");
//        mat.setColor("Color", ColorRGBA.Blue);
//        geom.setMaterial(mat);
//        rootNode.attachChild(geom);
