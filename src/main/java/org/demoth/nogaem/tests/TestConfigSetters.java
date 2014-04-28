package org.demoth.nogaem.tests;

import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Random;

import static org.demoth.nogaem.common.Config.*;

/**
 * @author demoth
 */
public class TestConfigSetters {
    public static void main(String[] args) {
        Logger log = LoggerFactory.getLogger("TestConfigSetters");
        Random random = new Random();
        log.info("Config cvars size: " + cvars.size());

        log.info("---INTEGER & LONG TEST---");
        String intLongVars[] = {"sv_port", "sv_sleep", "cl_sleep", "g_player_axis"};
        Arrays.stream(intLongVars).forEach(var -> {
            assert cvars.containsKey(var);
            log.info("Old value: " + cvars.get(var).get());
            int newValue = random.nextInt();
            log.info("Change to: " + newValue);
            cvars.get(var).set("" + newValue);
            assert Integer.valueOf(cvars.get(var).get()).compareTo(newValue) == 0;
        });

        log.info("--- FLOAT TEST---");
        float eps = 0.000_001f;
        String floatVars[] = {"g_scale", "g_mass", "g_player_radius", "g_player_height"};
        Arrays.stream(floatVars).forEach(var -> {
            assert cvars.containsKey(var);
            log.info("Old value: " + cvars.get(var).get());
            float newValue = random.nextFloat();
            log.info("Change to: " + newValue);
            cvars.get(var).set("" + newValue);
            assert newValue - Float.valueOf(cvars.get(var).get()) < eps;
        });

        log.info("--- VECTOR TEST---");
        log.info("g_spawn_point: " + g_spawn_point);
        cvars.get("g_spawn_point").set("1 2 3");
        log.info("new g_spawn_point: " + g_spawn_point);
        assert new Vector3f(1f, 2f, 3f).equals(g_spawn_point);

    }
}
