package client;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.serializing.Serializer;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.JmeContext;
import common.messages.LoginMessage;
import common.messages.MoveEntity;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientMain extends SimpleApplication {

    public static final String MOVE_ACTION = "move";
    Client client;
    Integer myId;
    Geometry geom;
    ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();


    @Override
    public void simpleInitApp() {
        Serializer.registerClass(LoginMessage.class);
        Serializer.registerClass(MoveEntity.class);
        try {
            client = Network.connectToServer("127.0.0.1", 5555);
            client.addMessageListener(new MessageListener<Client>() {
                @Override
                public void messageReceived(Client client, Message message) {
                    if (message instanceof LoginMessage) {
                        if (((LoginMessage) message).id > 0) {
                            myId = ((LoginMessage) message).id;
                            System.out.println("Joined!");
                        }
                    }
                }
            }, LoginMessage.class);
            client.addMessageListener(new MessageListener<Client>() {
                @Override
                public void messageReceived(Client client, Message message) {
                    if (message instanceof MoveEntity) {
                        messages.add(message);
                    }
                }
            }, MoveEntity.class);
            inputManager.addMapping(MOVE_ACTION, new KeyTrigger(KeyInput.KEY_SPACE));
            inputManager.addListener(new ActionListener() {
                @Override
                public void onAction(String s, boolean b, float v) {
                    if (MOVE_ACTION.equals(s)) {
                        client.send(new MoveEntity(myId, new Vector2f(2f, 2f)));
                    }
                }
            }, MOVE_ACTION);
            Box b = new Box(Vector3f.ZERO, 1, 1, 1);
            geom = new Geometry("Box", b);
            Material mat = new Material(assetManager,
                    "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Blue);
            geom.setMaterial(mat);
            rootNode.attachChild(geom);
            client.start();
            client.send(new LoginMessage("demoth", "cadaver", 0));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
        if (!messages.isEmpty()){
            Message message = messages.poll();
            if (message instanceof MoveEntity){
                MoveEntity m = (MoveEntity) message;
                geom.move(m.delta.x, m.delta.y, 0);
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
