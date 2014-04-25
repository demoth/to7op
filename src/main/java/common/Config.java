package common;

import com.jme3.math.Vector3f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by demoth on 24.04.14.
 */
public class Config {
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

    public static Map<String, Setter> setters;

    static {
        setters = new HashMap<>();
        Arrays.stream(Config.class.getDeclaredFields()).forEach(field -> {
            if (field.getName().contains("_"))
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
                        e.printStackTrace();
                    }
                });
        });
    }
}
