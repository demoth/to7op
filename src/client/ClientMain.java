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

import static common.Constants.Actions.*;

public class ClientMain extends SimpleApplication {

    Client client;
    volatile Long     buttons;
    volatile Vector3f view;

    ClientState currentState;
    Integer     myId;
    Geometry    geom;
    ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
    boolean running = true;


    @Override
    public void simpleInitApp() {
        Serializer.registerClass(LoginMessage.class);
        Serializer.registerClass(ActionMessage.class);
        Serializer.registerClass(TextMessage.class);
        Serializer.registerClass(ClientStateMessage.class);
        try {
            client = Network.connectToServer("127.0.0.1", 5555);
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
            inputManager.addMapping(WALK_FORWARD, new KeyTrigger(KeyInput.KEY_SPACE));
            inputManager.addMapping(LOOK_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
            inputManager.addMapping(LOOK_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
            inputManager.addMapping(LOOK_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
            inputManager.addMapping(LOOK_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, false));

            inputManager.addListener(new ActionListener() {
                @Override
                public void onAction(String s, boolean b, float v) {
                    pushButton(s, b);
                }
            }, WALK_FORWARD);
            inputManager.addListener(new AnalogListener() {
                @Override
                public void onAnalog(String name, float value, float tpf) {
                    updateLookAngle(name, value, tpf);
                }
            }, LOOK_UP, LOOK_DOWN, LOOK_LEFT, LOOK_RIGHT);
            Box b = new Box(Vector3f.ZERO, 1, 1, 1);
            geom = new Geometry("Box", b);
            Material mat = new Material(assetManager,
                    "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Blue);
            geom.setMaterial(mat);
            rootNode.attachChild(geom);
            client.start();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (running) {
                            client.send(new ActionMessage(view, buttons));
                            buttons = 0l;
                            wait(1000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            client.send(new LoginMessage("demoth", "cadaver", 0));
        } catch (IOException e) {
            e.printStackTrace();
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

    private void pushButton(String actionName, boolean pressed) {
        if (pressed)
            switch (actionName) {
                case WALK_FORWARD:
                    buttons &= Constants.Masks.WALK_FORWARD;
                    break;
            }
    }

    @Override
    public void update() {
        if (!messages.isEmpty()) {
            Message message = messages.poll();
            if (message instanceof ClientStateMessage) {
                ClientStateMessage m = (ClientStateMessage) message;
                ClientState diff = m.diff;
                geom.move(diff.position);
                geom.rotate(diff.view.x, diff.view.y, diff.view.z);
//                geom.move(m.delta.x, m.delta.y, 0);
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
