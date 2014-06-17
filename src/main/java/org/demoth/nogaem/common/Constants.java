package org.demoth.nogaem.common;

/**
 * @author demoth
 */
public interface Constants {
    interface Actions {
        String WALK_FORWARD   = "forward";
        String WALK_BACKWARD  = "backward";
        String STRAFE_LEFT    = "left";
        String STRAFE_RIGHT   = "right";
        String FIRE_PRIMARY   = "fire";
        String FIRE_SECONDARY = "switch";
        String JUMP           = "jump";
        String CROUCH         = "crouch";
        String USE            = "use";
        String LOOK_UP        = "lookUp";
        String LOOK_DOWN      = "lookDown";
        String LOOK_LEFT      = "lookLeft";
        String LOOK_RIGHT     = "lookRight";
        String TOGGLE_CONSOLE = "toggleConsole";
    }

    interface Masks {
        long WALK_FORWARD   = 1l;
        long WALK_BACKWARD  = 1l << 1;
        long STRAFE_LEFT    = 1l << 2;
        long STRAFE_RIGHT   = 1l << 3;
        long FIRE_SECONDARY = 1l << 4;
        long FIRE_PRIMARY   = 1l << 5;
        long JUMP           = 1l << 6;
        long CROUCH         = 1l << 7;
        long USE            = 1l << 8;
    }

    interface Effects {
        long NONE     = 0;
        long FLOATING = 1;
        long ROTATE_X = 2;
        long ROTATE_Y = 4;
        long ROTATE_Z = 8;
    }
}
