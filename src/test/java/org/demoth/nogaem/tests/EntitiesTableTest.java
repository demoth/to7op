package org.demoth.nogaem.tests;

import org.demoth.nogaem.common.Util;
import org.demoth.nogaem.common.entities.EntityDetailedInfo;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Map;

import static org.demoth.nogaem.common.Config.gamedir;
import static org.junit.Assert.assertTrue;

public class EntitiesTableTest {
    @Test
    public void testCsvConsistency() {
        Map<Integer, EntityDetailedInfo> entities = Util.parseCsv(gamedir + "/entities.csv");
        entities.forEach((integer, entityDetailedInfo) -> {
            assertTrue(entityDetailedInfo.typeId > 0);
            assertTrue(entityDetailedInfo.size > 0);
            assertTrue(entityDetailedInfo.mass >= 0);
            assertTrue(new File(gamedir + "/models/" + entityDetailedInfo.modelName).exists());
            assertTrue(new File(gamedir + "/sounds/" + entityDetailedInfo.appearSound).exists());
        });
    }
}
