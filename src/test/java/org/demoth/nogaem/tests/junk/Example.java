package org.demoth.nogaem.tests.junk;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.*;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.*;
import com.jme3.bullet.control.*;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.*;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;

/**
 * Example 9 - How to make walls and floors solid.
 * This collision code uses Physics and a custom Action Listener.
 *
 * @author normen, with edits by Zathras
 */
public class Example extends SimpleApplication
        implements ActionListener {

    private CharacterControl player;
    private Vector3f walkDirection = new Vector3f();
    private boolean  left          = false, right = false, up = false, down = false;

    //Temporary vectors used on each frame.
    //They here to avoid instantiating new vectors on each frame
    private Vector3f camDir  = new Vector3f();
    private Vector3f camLeft = new Vector3f();

    public Example() {
        setShowSettings(false);
        AppSettings settings = new AppSettings(true);
        settings.setWidth(1000);
        settings.setHeight(700);
        setSettings(settings);
    }

    public void simpleInitApp() {
        /* Set up Physics */
        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.getPhysicsSpace().enableDebug(assetManager);

        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(0);
        setUpKeys();
        setUpLight();

        // We load the scene from the zip file and adjust its size.
//        assetManager.registerLocator("data/town.zip", ZipLocator.class);
        assetManager.registerLocator("data/maps", FileLocator.class);
        Spatial sceneModel = assetManager.loadModel("box.blend");

        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape =
                CollisionShapeFactory.createMeshShape(sceneModel);
        RigidBodyControl landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);

        // We set up collision detection for the player by creating
        // a capsule collision shape and a CharacterControl.
        // The CharacterControl offers extra settings for
        // size, stepheight, jumping, falling, and gravity.
        // We also put the player in its starting pos.
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);
        player.setGravity(30);
        player.setPhysicsLocation(new Vector3f(0, 10, 0));

        // We attach the scene and the player to the rootnode and the physics space,
        // to make them appear in the game world.
        rootNode.attachChild(sceneModel);
        bulletAppState.getPhysicsSpace().add(landscape);
        bulletAppState.getPhysicsSpace().add(player);
    }

    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(5f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);

        PointLight pl = new PointLight();
        pl.setPosition(new Vector3f());
        pl.setRadius(10f);
        rootNode.addLight(pl);
    }

    /**
     * We over-write some navigational key mappings here, so we can
     * add physics-controlled walking and jumping:
     */
    private void setUpKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
    }

    /**
     * These are our custom actions triggered by key presses.
     * We do not walk yet, we just keep track of the direction the user pressed.
     */
    public void onAction(String binding, boolean isPressed, float tpf) {
        switch (binding) {
            case "Left":
                left = isPressed;
                break;
            case "Right":
                right = isPressed;
                break;
            case "Up":
                up = isPressed;
                break;
            case "Down":
                down = isPressed;
                break;
            case "Jump":
                if (isPressed) player.jump();
                break;
        }
    }

    /**
     * This is the main event loop--walking happens here.
     * We check in which direction the player is walking by interpreting
     * the camera direction forward (camDir) and to the side (camLeft).
     * The setWalkDirection() command is what lets a physics-controlled player walk.
     * We also make sure here that the camera moves with player.
     */
    @Override
    public void simpleUpdate(float tpf) {
        camDir.set(cam.getDirection()).multLocal(0.6f);
        camLeft.set(cam.getLeft()).multLocal(0.4f);
        walkDirection.set(0, 0, 0);
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }
        player.setWalkDirection(walkDirection);
        cam.setLocation(player.getPhysicsLocation());
    }

    public static void main(String... args) {
        new Example().start();
    }
}