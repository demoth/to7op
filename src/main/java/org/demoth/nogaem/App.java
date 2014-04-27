package org.demoth.nogaem;

import org.demoth.nogaem.client.ClientMain;
import org.demoth.nogaem.server.ServerMain;

/**
 * @author demoth
 */
public class App {
    public static void main(String... args) {
        if (args.length > 0) {
            if ("server".equals(args[0]))
                ServerMain.main(args);
            else if ("client".equals(args[0]))
                ClientMain.main(args);
        } else
            System.out.println("Usage: nogaem [client|server]");
    }
}
