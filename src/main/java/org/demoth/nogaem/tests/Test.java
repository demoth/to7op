package org.demoth.nogaem.tests;

import org.demoth.nogaem.client.ClientMain;
import org.demoth.nogaem.server.ServerMain;

/**
 * Created by daniil on 10.04.14.
 */
public class Test {
    public static void main(String... args) throws InterruptedException {
        ServerMain.main();
        Thread.sleep(1342);
        ClientMain.main();
    }
}
