package org.demoth.nogaem.tests;

import com.jme3.app.SimpleApplication;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.Network;
import com.jme3.system.JmeContext;
import org.demoth.nogaem.common.RconCommand;
import org.demoth.nogaem.common.Util;
import org.demoth.nogaem.common.messages.fromClient.LoginRequestMessage;
import org.demoth.nogaem.common.messages.fromClient.RconMessage;
import org.demoth.nogaem.common.messages.fromServer.JoinedGameMessage;

import java.io.IOException;

import static org.demoth.nogaem.common.Config.*;

/**
 * @author demoth
 */
public class HeadlessClient extends SimpleApplication{
    Client net;
    boolean waitingForJoinedGameMessage;

    public void simpleInitApp() {
        Util.registerMessages();
    }

    public void tryToConnect() {
        System.out.println("Connecting to " + host + ':' + port);
        try {
            net = Network.connectToServer(host, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        net.start();
        System.out.println("Connected, adding message listener");
        net.addMessageListener(this::receiveJoinedMessageAndShutdown);
        waitingForJoinedGameMessage = true;
        System.out.println("Sending login request");
        net.send(new LoginRequestMessage("demoth", "empty"));
        // here we are waiting JoinedMessage to come
    }

    public void tryToConnectAndReceiveUpdates() {

    }

    public void receiveJoinedMessageAndShutdown(Client client, Message message) {
        System.out.println("Received message: " + message);
        if (message instanceof JoinedGameMessage) {
            if (waitingForJoinedGameMessage) {
                // all ok! exiting
                shutdown();
            } else {
                throw new RuntimeException("JoinedGameMessage received when not expected!");
            }
        } else {
            throw new RuntimeException("Unexpected message!");
        }
    }

    private void shutdown() {
        System.out.println("Sending rcon stop");
        net.send(new RconMessage(RconCommand.stop, "", rcon_pass));
        try {
            Thread.sleep(1000);
            net.close();
            stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        HeadlessClient client = new HeadlessClient();
        client.start(JmeContext.Type.Headless);
        client.tryToConnect();
    }
}
