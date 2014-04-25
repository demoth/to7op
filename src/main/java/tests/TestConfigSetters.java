package tests;

import com.jme3.math.Vector3f;
import common.Config;

import java.util.logging.Logger;

/**
 * Created by demoth on 25.04.14.
 */
public class TestConfigSetters {
    public static void main(String[] args) {
        Logger log = Logger.getLogger("TestConfigSetters");
        log.info("---INTEGER TEST---");
        log.info("sv_port:" + Config.sv_port);
        Config.setters.get("sv_port").set("3214");
        log.info("new sv_port: " + Config.sv_port);
        assert Config.sv_port == 3214;

        log.info("---   LONG TEST---");
        log.info("sv_sleep:" + Config.sv_sleep);
        Config.setters.get("sv_sleep").set("1234125");
        log.info("new sv_sleep: " + Config.sv_sleep);
        assert Config.sv_sleep == 1234125;

        log.info("--- VECTOR TEST---");
        log.info("g_spawn_point: " + Config.g_spawn_point);
        Config.setters.get("g_spawn_point").set("1 2 3");
        log.info("new g_spawn_point: " + Config.g_spawn_point);
        assert new Vector3f(1f, 2f, 3f).equals(Config.g_spawn_point);

    }
}
