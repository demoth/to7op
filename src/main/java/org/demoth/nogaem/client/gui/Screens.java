package org.demoth.nogaem.client.gui;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.InputManager;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.ViewPort;
import de.lessvoid.nifty.Nifty;
import org.demoth.nogaem.client.*;

/**
 * @author demoth
 */
public class Screens {

    public static ClientScreenController createController(AssetManager assetManager, InputManager inputManager,
                                                          AudioRenderer audioRenderer, ViewPort guiViewPort,
                                                          ClientMain main) {
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(
                assetManager, inputManager, audioRenderer, guiViewPort);
        Nifty nifty = niftyDisplay.getNifty();
        ClientScreenController screenController = new ClientScreenController(main);
        nifty.fromXml("ui/screens.xml", "mainmenuScreen", screenController);
//        nifty.setDebugOptionPanelColors(true);
        guiViewPort.addProcessor(niftyDisplay);
        return screenController;
    }

}
