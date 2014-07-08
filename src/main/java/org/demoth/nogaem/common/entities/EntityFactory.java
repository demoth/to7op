package org.demoth.nogaem.common.entities;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapText;
import com.jme3.scene.*;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Box;
import org.demoth.nogaem.client.controls.ClientEntity;
import org.demoth.nogaem.common.Util;
import org.slf4j.*;

import static org.demoth.nogaem.common.Config.g_player_height;

/**
 * @author demoth
 */
public class EntityFactory {
    static final Logger log = LoggerFactory.getLogger(EntityFactory.class);
    private AssetManager assetManager;
    private Node         rootNode;

    public EntityFactory(AssetManager assetManager, Node rootNode) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
    }

    public ClientEntity createClientEntity(EntityInfo entityInfo) {
        Node node = new Node(entityInfo.name);
        Spatial model;
        float size;
        switch (entityInfo.typeId) {
            case 1:
                model = assetManager.loadModel("models/player.blend");
                model.move(0f, -g_player_height / 2, 0f);
                size = 5f;
                break;
            case 2:
            default:
                model = assetManager.loadModel("models/axe.blend");
                size = 1f;
        }
        Geometry bounds = new Geometry(entityInfo.name + "BB", new Box(size, size, size));
        bounds.setMaterial(Util.createBoundBoxMaterial(assetManager));
        node.attachChild(model);
        node.attachChild(bounds);
        Node textNode = new Node();
        BitmapText text = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
        text.setText(entityInfo.name);
        text.setSize(1f);
        text.move(-text.getLineWidth() / 2, size + text.getLineHeight(), 0f);
        textNode.attachChild(text);
        textNode.addControl(new BillboardControl());
        node.attachChild(textNode);
        Util.attachCoordinateAxes(node, assetManager);
        rootNode.attachChild(node);
        return new ClientEntity(entityInfo, node, model);
    }
}
