package org.demoth.nogaem.common;

import com.jme3.math.Vector3f;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author demoth
 */
public class Config {
    private static final Logger log = Logger.getLogger("Config");

    //////////////////////////////////////////////////////////////////////////
    /////////////////       CONFIG VARIABLES START      //////////////////////
    //////////////////////////////////////////////////////////////////////////
    public static int  sv_port  = 5555;
    public static long sv_sleep = 50;

    public static float    g_scale             = 2f;
    public static float    g_mass              = 0f;
    public static float    g_player_radius     = 1.5f;
    public static float    g_player_height     = 6f;
    public static int      g_player_axis       = 1;
    public static float    g_player_step       = 0.05f;
    public static float    g_player_jumpheight = 15;
    public static float    g_player_fallspeed  = 60;
    public static float    g_player_gravity    = 30;
    public static Vector3f g_spawn_point       = new Vector3f(0f, 10f, 0f);
    public static String   g_map               = "main.scene";

    public static String cl_server = "127.0.0.1";
    public static String cl_user   = "demoth";
    public static String cl_pass   = "cadaver";
    public static long   cl_sleep  = 50;

    public static float r_ambient = 1.3f;

    //////////////////////////////////////////////////////////////////////////
    /////////////////       CONFIG VARIABLES END         /////////////////////
    //////////////////////////////////////////////////////////////////////////
    public static Map<String, CVar> cvars;

    public static void load(String fileName) {
        try {
            Properties props = new Properties();
            props.load(new FileReader(fileName));
            props.forEach((key, value) -> cvars.get(String.valueOf(key)).set(String.valueOf(value)));
        } catch (IOException e) {
            log.severe(e.getMessage());
        }
    }

    public static void save(String fileName) {
        Properties props = new Properties();
        cvars.keySet().stream().sorted().forEach(k ->
                props.put(k, cvars.get(k).get()));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            props.store(writer, "Nogaem configuration");
            writer.flush();
        } catch (IOException e) {
            log.severe(e.getMessage());
        }
    }
    static {
        cvars = new HashMap<>();
        Properties desc = new Properties();
        try {
            desc.load(Config.class.getResourceAsStream("/cvar_descriptions.properties"));
        } catch (IOException e) {
            log.severe(e.getMessage());
        }
        Arrays.stream(Config.class.getDeclaredFields()).forEach(field -> {
            if (!field.getName().equals("cvars") && !field.getName().equals("log")) {
                cvars.put(field.getName(), new CVar(desc.getProperty(field.getName()), src -> {
                    try {
                        if (field.getType() == boolean.class) {
                            field.set(null, Boolean.parseBoolean(src));
                        } else if (field.getType() == int.class) {
                            field.set(null, Integer.parseInt(src));
                        } else if (field.getType() == long.class) {
                            field.set(null, Long.parseLong(src));
                        } else if (field.getType() == float.class) {
                            field.set(null, Float.parseFloat(src));
                        } else if (field.getType() == Vector3f.class) {
                            String coords[] = src.split(" ");
                            if (coords.length == 3)
                                field.set(null, new Vector3f(
                                                Float.parseFloat(coords[0]),
                                                Float.parseFloat(coords[1]),
                                                Float.parseFloat(coords[2]))
                                );
                        } else if (field.getType() == String.class) {
                            field.set(null, src);
                        }
                    } catch (Exception e) {
                        log.severe("Error while setting " + field.getName() + " value:" + e.getMessage());
                    }

                }, () -> {
                    try {
                        if (field.getType() == Vector3f.class) {
                            Vector3f v = (Vector3f) field.get(null);
                            return "" + v.x + ' ' + v.y + ' ' + v.z;
                        }
                        return String.valueOf(field.get(null));
                    } catch (IllegalAccessException e) {
                        log.severe("Error while getting " + field.getName() + " value:" + e.getMessage());
                        return "";
                    }
                }));
            }
        });
    }

    public static void loadOrSave(String fileName) {
        if (Files.exists(Paths.get(fileName)))
            load(fileName);
        else
            save(fileName);
    }

    static interface Getter {
        String get();
    }

    static interface Setter {
        void set(String s);
    }

    public static class CVar {
        public final String description;
        public final Setter setter;
        public final Getter getter;

        public CVar(String description, Setter setter, Getter getter) {
            this.description = description;
            this.setter = setter;
            this.getter = getter;
        }

        public void set(String src) {
            setter.set(src);
        }

        public String get() {
            return getter.get();
        }
    }
}
