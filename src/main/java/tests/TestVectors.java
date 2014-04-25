package tests;

import com.jme3.math.Vector3f;

/**
 * Created by daniil on 13.04.14.
 */
public class TestVectors {
    public static void main(String... args) {
        Vector3f x = new Vector3f(1f, 0f, 0f);
        Vector3f up = new Vector3f(0f, 0f, 1f);
        System.out.println(x.cross(up));
    }
}
