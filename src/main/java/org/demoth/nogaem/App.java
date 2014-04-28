package org.demoth.nogaem;

import org.apache.commons.cli.*;
import org.demoth.nogaem.client.ClientMain;
import org.demoth.nogaem.common.Config;
import org.demoth.nogaem.server.ServerMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author demoth
 */
public class App {
    private static final Logger log = LoggerFactory.getLogger("main");

    public static void main(String... args) throws ParseException {
        log.info("Welcome to Nogaem!");
        Options options = new Options();
        options.addOption("server", false, "Start nogaem server");
        options.addOption("client", false, "Start client server");
        Config.cvars.forEach((k, o) -> options.addOption(k, true, o.description));

        try {
            CommandLine cmd = new GnuParser().parse(options, args);

            if (cmd.hasOption("server")) {
                Config.loadOrSave("server.cfg");
                setCvarsFromCmdline(cmd);
                ServerMain.run();
            } else if (cmd.hasOption("client")) {
                Config.loadOrSave("client.cfg");
                setCvarsFromCmdline(cmd);
                ClientMain.run();
            } else
                printUsage(options);

        } catch (ParseException e) {
            printUsage(options);
        }
    }

    private static void setCvarsFromCmdline(CommandLine cmd) {
        Config.cvars.forEach((k, cvar) -> {
            if (cmd.hasOption(k))
                cvar.set(cmd.getOptionValue(k));
        });
    }

    private static void printUsage(Options options) {
        new HelpFormatter().printHelp("nogaem [client|server]", options);
    }
}
