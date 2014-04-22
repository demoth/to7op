package common;

/**
 * Created by daniil on 2/28/14.
 */
public interface Constants {
    interface Actions {
        String WALK_FORWARD  = "forward";
        String WALK_BACKWARD = "backward";
        String STRAFE_LEFT   = "left";
        String STRAFE_RIGHT  = "right";
        String FIRE_PRIMARY  = "fire";
        String JUMP          = "jump";
        String CROUCH        = "crouch";
        String USE           = "use";
        String LOOK_UP       = "lookUp";
        String LOOK_DOWN     = "lookDown";
        String LOOK_LEFT     = "lookLeft";
        String LOOK_RIGHT    = "lookRight";
    }

    int updateRate = 50;

    interface Masks {
        long WALK_FORWARD  = 1l;
        long WALK_BACKWARD = 1l << 1;
        long STRAFE_LEFT   = 1l << 2;
        long STRAFE_RIGHT  = 1l << 3;
        long FIRE_PRIMARY  = 1l << 4;
        long JUMP          = 1l << 5;
        long CROUCH        = 1l << 6;
        long USE           = 1l << 7;
    }
}
