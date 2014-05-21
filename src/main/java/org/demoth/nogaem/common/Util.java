package org.demoth.nogaem.common;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.*;
import com.jme3.network.serializing.*;
import com.jme3.scene.plugins.blender.BlenderModelLoader;
import org.demoth.nogaem.common.entities.Entity;
import org.demoth.nogaem.common.messages.*;
import org.demoth.nogaem.common.messages.fromClient.*;
import org.demoth.nogaem.common.messages.fromServer.*;
import org.demoth.nogaem.server.Player;
import org.slf4j.*;

import java.io.File;
import java.util.Arrays;

/**
 * @author demoth
 */
public class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    public static String trimFirstWord(String str) {
        return str.substring(str.indexOf(' '), str.length()).trim();
    }

    public static void scanDataFolder(AssetManager assetManager) {
        assetManager.registerLocator("data/", FileLocator.class);
        assetManager.registerLoader(BlenderModelLoader.class, "blend");
        Arrays.stream(new File("data").listFiles(file -> file.getName().endsWith("zip"))).forEach(f -> {
            log.info("Registering data file: " + f.getName());
            assetManager.registerLocator("data/" + f.getName(), ZipLocator.class);
        });
    }

    public static void registerMessages() {
        Serializer.registerClass(Acknowledgement.class);
        Serializer.registerClass(ActionMessage.class);
        Serializer.registerClass(LoginRequestMessage.class);
        Serializer.registerClass(Entity.class);
        Serializer.registerClass(RconMessage.class);

        Serializer.registerClass(AddedEntities.class);
        Serializer.registerClass(ChangeMapMessage.class);
        Serializer.registerClass(EntityState.class);
        Serializer.registerClass(GameStateChange.class);
        Serializer.registerClass(RemovedEnitites.class);
        Serializer.registerClass(JoinedGameMessage.class);

        Serializer.registerClass(TextMessage.class);
    }
}
