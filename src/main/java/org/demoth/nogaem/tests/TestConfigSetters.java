package org.demoth.nogaem.tests;

import com.jme3.math.Vector3f;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

import static org.demoth.nogaem.common.Config.*;

/**
 * @author demoth
 */
public class TestConfigSetters {
    public static void main(String[] args) {
        Logger log = Logger.getLogger("TestConfigSetters");
        Random random = new Random();
        log.info("Config getters size: " + getters.size());
        log.info("Config setters size: " + setters.size());

        log.info("---INTEGER & LONG TEST---");
        String intLongVars[] = {"sv_port", "sv_sleep", "cl_sleep", "g_player_axis"};
        Arrays.stream(intLongVars).forEach(var -> {
            assert getters.containsKey(var);
            assert setters.containsKey(var);
            log.info("Old value: " + getters.get(var).get());
            int newValue = random.nextInt();
            log.info("Change to: " + newValue);
            setters.get(var).set("" + newValue);
            assert Integer.valueOf(getters.get(var).get()).compareTo(newValue) == 0;
        });

        log.info("--- FLOAT TEST---");
        float eps = 0.000_001f;
        String floatVars[] = {"g_scale", "g_mass", "g_player_radius", "g_player_height"};
        Arrays.stream(floatVars).forEach(var -> {
            assert getters.containsKey(var);
            assert setters.containsKey(var);
            log.info("Old value: " + getters.get(var).get());
            float newValue = random.nextFloat();
            log.info("Change to: " + newValue);
            setters.get(var).set("" + newValue);
            assert newValue - Float.valueOf(getters.get(var).get()) < eps;
        });

        log.info("--- VECTOR TEST---");
        log.info("g_spawn_point: " + g_spawn_point);
        setters.get("g_spawn_point").set("1 2 3");
        log.info("new g_spawn_point: " + g_spawn_point);
        assert new Vector3f(1f, 2f, 3f).equals(g_spawn_point);

    }
}
