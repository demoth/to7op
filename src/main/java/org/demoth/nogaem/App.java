package org.demoth.nogaem;

import org.apache.commons.cli.*;
import org.demoth.nogaem.client.ClientMain;
import org.demoth.nogaem.common.Config;
import org.demoth.nogaem.server.ServerMain;

/**
 * @author demoth
 */
public class App {
    public static void main(String... args) throws ParseException {
        Options options = new Options();
        options.addOption("server", false, "Start nogaem server");
        options.addOption("client", false, "Start client server");
        Config.getters.keySet().forEach(o -> options.addOption(o, true, ""));
        CommandLine cmd = new GnuParser().parse(options, args);
        if (cmd.hasOption("server"))
            ServerMain.run(cmd);
        else if (cmd.hasOption("client"))
            ClientMain.run(cmd);
    }
}
