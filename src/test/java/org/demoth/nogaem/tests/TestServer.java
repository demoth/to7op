package org.demoth.nogaem.tests;

import com.jme3.system.JmeContext;
import org.demoth.nogaem.server.ServerMain;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * @author demoth
 */
@RunWith(JUnit4.class)
public class TestServer {

    @Test(timeout = 7000)
    public void testConnection() throws IOException, InterruptedException {
        prepareTest(HeadlessClient::tryToConnect);
    }

    @Test
    public void testUpdates() throws IOException, InterruptedException {
        prepareTest(HeadlessClient::tryToConnectAndReceiveUpdates);
    }

    private void prepareTest(Consumer<HeadlessClient> c) throws InterruptedException, IOException {
        new Thread(ServerMain::run).start();
        Thread.sleep(3000);
        HeadlessClient client = new HeadlessClient();
        client.start(JmeContext.Type.Headless);
        c.accept(client);
        // join to client or server
        Thread.getAllStackTraces().keySet().stream().filter(o -> o.getName().equals("Headless Application Thread"))
                .collect(Collectors.toList()).get(0).join();
    }
}
