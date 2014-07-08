package org.demoth.nogaem.common;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.network.serializing.*;
import com.jme3.scene.*;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.plugins.blender.BlenderModelLoader;
import org.demoth.nogaem.common.entities.EntityInfo;
import org.demoth.nogaem.common.messages.*;
import org.demoth.nogaem.common.messages.fromClient.*;
import org.demoth.nogaem.common.messages.fromServer.*;
import org.slf4j.*;

import java.nio.file.*;

import static org.demoth.nogaem.common.Config.gamedir;

/**
 * @author demoth
 */
public class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    public static String trimFirstWord(String str) {
        return str.substring(str.indexOf(' '), str.length()).trim();
    }

    public static void scanDataFolder(AssetManager assetManager) {
        if (!Files.exists(Paths.get(gamedir))) {
            log.warn("gamedir: " + gamedir + " not found in current directory. Trying to find it above...");
            if (Files.exists(Paths.get("../" + gamedir))) {
                log.info("Found in ../");
                gamedir = "../" + gamedir;
            } else if (gamedir.startsWith("../") && Files.exists(Paths.get(gamedir.replaceFirst("\\.\\./","")))) {
                log.info("Found in current");
                gamedir = gamedir.replaceFirst("\\.\\./", "");
            }
        }
        assetManager.registerLocator(gamedir + '/', FileLocator.class);
        assetManager.registerLoader(BlenderModelLoader.class, "blend");
    }

    public static void registerMessages() {
        Serializer.registerClass(Acknowledgement.class);
        Serializer.registerClass(ActionMessage.class);
        Serializer.registerClass(LoginRequestMessage.class);
        Serializer.registerClass(EntityInfo.class);
        Serializer.registerClass(RconMessage.class);

        Serializer.registerClass(AddedEntities.class);
        Serializer.registerClass(ChangeMapMessage.class);
        Serializer.registerClass(EntityState.class);
        Serializer.registerClass(GameStateChange.class);
        Serializer.registerClass(RemovedEnitites.class);
        Serializer.registerClass(JoinedGameMessage.class);

        Serializer.registerClass(TextMessage.class);
    }

    public static Material createBoundBoxMaterial(AssetManager assetManager) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Blue);
        return mat;
    }
    public static void attachCoordinateAxes(Node node, AssetManager assetManager) {
        Arrow arrow = new Arrow(Vector3f.UNIT_X);
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, node, ColorRGBA.Red, assetManager);

        arrow = new Arrow(Vector3f.UNIT_Y);
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, node, ColorRGBA.Green, assetManager);

        arrow = new Arrow(Vector3f.UNIT_Z);
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, node, ColorRGBA.Blue, assetManager);
    }

    private static Geometry putShape(Mesh shape, Node node, ColorRGBA color, AssetManager assetManager) {
        Geometry g = new Geometry("coordinate axis", shape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", color);
        g.setMaterial(mat);
        node.attachChild(g);
        return g;
    }


}
