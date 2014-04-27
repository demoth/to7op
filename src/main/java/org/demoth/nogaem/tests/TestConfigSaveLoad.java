package org.demoth.nogaem.tests;

import org.demoth.nogaem.common.Config;

/**
 * Created by demoth on 26.04.14.
 */
public class TestConfigSaveLoad {
    public static void main(String[] args) {
        String fileName = "testConfig.cfg";
        Config.save(fileName);
        Config.load(fileName);
    }
}
