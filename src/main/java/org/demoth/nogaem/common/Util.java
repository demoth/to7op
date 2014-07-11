package org.demoth.nogaem.common;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.scene.*;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.plugins.blender.BlenderModelLoader;
import org.demoth.nogaem.common.entities.*;
import org.demoth.nogaem.common.messages.TextMessage;
import org.demoth.nogaem.common.messages.fromClient.*;
import org.demoth.nogaem.common.messages.fromServer.*;
import org.slf4j.*;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

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
            } else if (gamedir.startsWith("../") && Files.exists(Paths.get(gamedir.replaceFirst("\\.\\./", "")))) {
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


    public static Map<Integer, EntityDetailedInfo> parseCsv(String pathname) {
        HashMap<Integer, EntityDetailedInfo> result = new HashMap<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(pathname)));
            String[] properties = reader.readLine().split(",");
            List<Field> methods = new ArrayList<>(properties.length);
            for (String name : properties)
                methods.add(EntityDetailedInfo.class.getDeclaredField(name));
            reader.lines().forEach(s -> {
                EntityDetailedInfo info = new EntityDetailedInfo();
                String[] values = s.split(",");
                if (values.length != properties.length)
                    log.error("Line has wrong amount of values: " + s);
                for (int i = 0; i < values.length; i++)
                    setValue(values[i], methods.get(i), info);
                result.put(info.typeId, info);
            });
        } catch (IOException | NoSuchFieldException e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    private static void setValue(String value, Field field, EntityDetailedInfo info) {
        try {
            switch (field.getType().getSimpleName()) {
                case "int":
                    field.set(info, Integer.parseInt(value));
                    break;
                case "float":
                    field.set(info, Float.parseFloat(value));
                    break;
                case "String":
                    field.set(info, value);
                    break;
                default:
                    log.error("Unknown value type " + field.getType().getSimpleName() + " for " + field.getName());
            }
        } catch (NumberFormatException | IllegalAccessException e) {
            log.error("Error while setting value: " + value + " field " + field.getName());
        }
    }
}
