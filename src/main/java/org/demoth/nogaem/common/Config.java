package org.demoth.nogaem.common;

import com.jme3.math.Vector3f;
import org.slf4j.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * @author demoth
 */
public class Config {
    public static final Map<String, CVar> cvars;
    private static final Logger   log                 = LoggerFactory.getLogger(Config.class);
    //////////////////////////////////////////////////////////////////////////
    /////////////////       CONFIG VARIABLES START      //////////////////////
    //////////////////////////////////////////////////////////////////////////
    public static        int      port                = 5555;
    public static        long     sv_sleep            = 100;
    public static        int      sv_drop_after       = 50;
    public static        String   rcon_pass           = "asdf";
    public static        float    g_scale             = 1f;
    public static        float    g_player_radius     = 1.5f;
    public static        float    g_player_height     = 6f;
    public static        int      g_player_axis       = 1;
    public static        float    g_player_step       = 0.05f;
    public static        float    g_player_jumpheight = 15;
    public static        float    g_player_fallspeed  = 60;
    public static        float    g_player_gravity    = 30;
    public static        Vector3f g_spawn_point       = new Vector3f(0f, 10f, 0f);
    public static        String   map                 = "box.blend";
    public static        String   host                = "127.0.0.1";
    public static        String   cl_user             = "demoth";
    public static        String   cl_pass             = "cadaver";
    public static        long     cl_sleep            = 100;
    public static        float    cl_float_offset     = 0.5f;
    public static        float    cl_lerp             = sv_sleep / 1000f;
    public static        float    r_ambient           = 1.3f;

    //////////////////////////////////////////////////////////////////////////
    /////////////////       CONFIG VARIABLES END         /////////////////////
    //////////////////////////////////////////////////////////////////////////
    private static void load(String fileName) {
        try {
            Properties props = new Properties();
            props.load(new FileReader(fileName));
            props.forEach((key, value) -> cvars.get(String.valueOf(key)).set(String.valueOf(value)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
            log.error(e.getMessage(), e);
        }
    }

    static {
        cvars = new HashMap<>();
        Properties desc = new Properties();
        try {
            desc.load(Config.class.getResourceAsStream("/cvar_descriptions.properties"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
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
                        log.error("Error while setting " + field.getName() + '.' + e.getMessage());
                    }

                }, () -> {
                    try {
                        if (field.getType() == Vector3f.class) {
                            Vector3f v = (Vector3f) field.get(null);
                            return "" + v.x + ' ' + v.y + ' ' + v.z;
                        }
                        return String.valueOf(field.get(null));
                    } catch (IllegalAccessException e) {
                        log.error("Error while getting " + field.getName() + '.' + e.getMessage());
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
