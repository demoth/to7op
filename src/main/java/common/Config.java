package common;

import com.jme3.math.Vector3f;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by demoth on 24.04.14.
 */
public class Config {
    private static final Logger log = Logger.getLogger("Config");

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

    public static interface Setter {
        void set(String src);
    }

    public static interface Getter {
        String get();
    }

    public static Map<String, Setter> setters;
    public static Map<String, Getter> getters;

    public static void load(String fileName) {
        try {
            new BufferedReader(new FileReader(fileName)).lines().forEach(line -> {
                String parts[] = line.split(" ");
                if (setters.containsKey(parts[0]))
                    setters.get(parts[0]).set(parts[1]);
            });
        } catch (FileNotFoundException e) {
            log.severe(e.getMessage());
        }
    }

    public static void save(String fileName) {
        StringBuilder sb = new StringBuilder();
        getters.keySet().stream().sorted().forEach(k -> sb.append(k).append(" ").append(getters.get(k).get()).append('\n'));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(sb.toString());
            writer.flush();
        } catch (IOException e) {
            log.severe(e.getMessage());
        }
    }

    static {
        setters = new HashMap<>();
        getters = new HashMap<>();
        Arrays.stream(Config.class.getDeclaredFields()).forEach(field -> {
            if (field.getName().contains("_")) {
                getters.put(field.getName(), () -> {
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
                });
                setters.put(field.getName(), src -> {
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
                });
            }
        });
    }
}
