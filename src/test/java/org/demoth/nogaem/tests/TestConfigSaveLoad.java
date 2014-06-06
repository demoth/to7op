package org.demoth.nogaem.tests;

import org.demoth.nogaem.common.Config;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;
import java.nio.file.*;

/**
 * @author demoth
 */

@RunWith(JUnit4.class)
public class TestConfigSaveLoad {
    String fileName = "testConfig.cfg";


    @Test
    public void loadOrSave() throws IOException {
        if (Files.exists(Paths.get(fileName)))
            Files.delete(Paths.get(fileName));
        assert Files.notExists(Paths.get(fileName));
        Config.loadOrSave(fileName);
        assert Files.exists(Paths.get(fileName));
    }

    @Test
    public void saveConfig() {
        Config.save(fileName);
        assert Files.exists(Paths.get(fileName));
    }

    @After
    public void cleanup() {
        assert new File(fileName).delete();
    }
}
