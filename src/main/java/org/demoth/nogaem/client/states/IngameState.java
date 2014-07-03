package org.demoth.nogaem.client.states;

import com.jme3.app.state.*;
import com.jme3.input.*;
import com.jme3.input.controls.*;
import org.demoth.nogaem.client.*;
import org.slf4j.*;

import static org.demoth.nogaem.common.Constants.Actions.*;

/**
 * @author demoth
 */
public class IngameState extends AbstractAppState {
    private static final Logger log = LoggerFactory.getLogger(IngameState.class);
    private final InputManager   inputManager;
    private final FlyByCamera    flyCam;
    private final ActionListener pushButton;
    String buttonMappings[] = {WALK_FORWARD, WALK_BACKWARD, STRAFE_LEFT, STRAFE_RIGHT, JUMP, FIRE_PRIMARY, FIRE_SECONDARY};


    public IngameState(InputManager inputManager, FlyByCamera flyCam, ClientMain clientMain) {
        log.info("Created IngameState");
        this.inputManager = inputManager;
        this.flyCam = flyCam;
        this.flyCam.setMoveSpeed(0);
        this.pushButton = clientMain::pushButton;
    }

    @Override
    public void stateAttached(AppStateManager stateManager) {
        super.stateAttached(stateManager);
        flyCam.setEnabled(true);
        inputManager.setCursorVisible(false);
        inputManager.addMapping(WALK_FORWARD, new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(WALK_BACKWARD, new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(STRAFE_LEFT, new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(STRAFE_RIGHT, new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(JUMP, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping(FIRE_PRIMARY, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(FIRE_SECONDARY, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        inputManager.addListener(pushButton, buttonMappings);
        log.info("Attached IngameState");
    }

    @Override
    public void stateDetached(AppStateManager stateManager) {
        super.stateDetached(stateManager);
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);
        inputManager.deleteMapping(WALK_FORWARD);
        inputManager.deleteMapping(WALK_BACKWARD);
        inputManager.deleteMapping(STRAFE_LEFT);
        inputManager.deleteMapping(STRAFE_RIGHT);
        inputManager.deleteMapping(JUMP);
        inputManager.deleteMapping(FIRE_PRIMARY);
        inputManager.deleteMapping(FIRE_SECONDARY);
        inputManager.removeListener(pushButton);
        log.info("Detached IngameState");
    }
}
