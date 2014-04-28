package org.demoth.nogaem.tests;

import com.jme3.math.Vector3f;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Random;

import static org.demoth.nogaem.common.Config.*;

/**
 * @author demoth
 */
@RunWith(JUnit4.class)
public class TestConfigSetters {

    private Logger log;
    private Random random;

    @Before
    public void setUp() {
        log = LoggerFactory.getLogger("TestConfigSetters");
        random = new Random();
        log.info("Config cvars size: " + cvars.size());
    }

    @Test
    public void testIntegers() {
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
    }

    @Test
    public void testFloat() {
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


    }

    @Test
    public void testVector3f() {
        log.info("--- VECTOR TEST---");
        log.info("g_spawn_point: " + g_spawn_point);
        cvars.get("g_spawn_point").set("1 2 3");
        log.info("new g_spawn_point: " + g_spawn_point);
        assert new Vector3f(1f, 2f, 3f).equals(g_spawn_point);
    }
}
