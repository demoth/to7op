package tests;

import client.ClientMain;
import server.ServerMain;

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
