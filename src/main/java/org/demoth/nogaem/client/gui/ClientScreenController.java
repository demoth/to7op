package org.demoth.nogaem.client.gui;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.button.ButtonControl;
import de.lessvoid.nifty.controls.textfield.TextFieldControl;
import de.lessvoid.nifty.screen.*;
import org.demoth.nogaem.client.ClientMain;
import org.demoth.nogaem.common.Config;

/**
 * @author demoth
 */
public class ClientScreenController implements ScreenController {

    private ClientMain client;
    private Nifty      nifty;

    ClientScreenController(ClientMain client) {
        this.client = client;
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {

        this.nifty = nifty;
    }

    @Override
    public void onStartScreen() {

    }

    @Override
    public void onEndScreen() {

    }

    public String getUser() {
        return Config.cl_user;
    }

    public String getPass() {
        return Config.cl_pass;
    }

    public String getHost() {
        return Config.host;
    }

    public Integer getPort() {
        return Config.port;
    }

    public void gotoScreen(String name) {
        nifty.gotoScreen(name);
    }


    public void quit() {
        client.enqueue("quit");
    }

    public void disconnect() {
        client.enqueue("disconnect");
    }

    public void setConnectParamsAndConnect() {
        TextFieldControl hostField = nifty.getScreen("loginScreen").findControl("hostField", TextFieldControl.class);
        Config.host = hostField.getRealText();
        TextFieldControl portField = nifty.getScreen("loginScreen").findControl("portField", TextFieldControl.class);
        Config.port = Integer.parseInt(portField.getRealText());
        TextFieldControl usernameField = nifty.getScreen("loginScreen").findControl("usernameField", TextFieldControl.class);
        Config.cl_user = usernameField.getRealText();
        TextFieldControl passwordField = nifty.getScreen("loginScreen").findControl("passwordField", TextFieldControl.class);
        Config.cl_pass = passwordField.getRealText();
        Config.save("client.cfg");
        client.enqueue("connect");
    }

    public void resume() {
        if (client.isConnected()) {
            nifty.gotoScreen("hud");
            client.enableIngameState(false);
        }
    }

    public void toggleMainMenu() {
        if ("hud".equals(nifty.getCurrentScreen().getScreenId())) {
            showMainMenu();
        } else {
            resume();
        }
    }

    public void showMainMenu() {
        nifty.gotoScreen("mainmenuScreen");
        client.enableIngameState(true);
        checkConnectionResumeDisconnect();
    }

    public void checkConnectionResumeDisconnect() {
        ButtonControl resumeButton = nifty.getScreen("mainmenuScreen").findControl("resumeButton", ButtonControl.class);
        resumeButton.setEnabled(client.isConnected());
        ButtonControl disconnectButton = nifty.getScreen("mainmenuScreen").findControl("disconnectButton", ButtonControl.class);
        disconnectButton.setEnabled(client.isConnected());
    }
}
