package org.demoth.nogaem;

import org.apache.commons.cli.*;
import org.demoth.nogaem.client.ClientMain;
import org.demoth.nogaem.common.Config;
import org.demoth.nogaem.server.ServerMain;
import org.slf4j.*;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.LogManager;

/**
 * @author demoth
 */
public class App {
    public static void main(String... args) throws ParseException {
        Logger log = LoggerFactory.getLogger(App.class);
        log.info("Starting nogaem application");
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        Options options = new Options();
        options.addOption("server", false, "Start nogaem server");
        options.addOption("client", false, "Start nogaem client");
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
