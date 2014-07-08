package org.demoth.nogaem.common.entities;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapText;
import com.jme3.scene.*;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Box;
import org.demoth.nogaem.client.controls.ClientEntity;
import org.demoth.nogaem.common.Util;
import org.slf4j.*;

import java.io.File;
import java.util.Map;

import static org.demoth.nogaem.common.Config.g_player_height;
import static org.demoth.nogaem.common.Config.gamedir;

/**
 * @author demoth
 */
public class EntityFactory {
    static final Logger log = LoggerFactory.getLogger(EntityFactory.class);
    private AssetManager assetManager;
    private Node         rootNode;
    private Map<Integer, EntityDetailedInfo> detailedInfoMap;

    public EntityFactory(AssetManager assetManager, Node rootNode) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.detailedInfoMap = Util.parseCsv(gamedir + "/entities.csv");
    }

    public ClientEntity createClientEntity(EntityInfo info) {
        Node node = new Node(info.name);
        EntityDetailedInfo detailedInfo = detailedInfoMap.get(info.typeId);
        if (detailedInfo == null) {
            log.error("Detailed info for typeId:" + info.typeId + " not found");
            return null;
        }
        Spatial model = assetManager.loadModel("models/" + detailedInfo.modelName);
        Geometry bounds = new Geometry(info.name + "BB", new Box(detailedInfo.size, detailedInfo.size, detailedInfo.size));
        bounds.setMaterial(Util.createBoundBoxMaterial(assetManager));
        node.attachChild(model);
        node.attachChild(bounds);
        Node textNode = new Node();
        BitmapText text = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
        text.setText(info.name);
        text.setSize(1f);
        text.move(-text.getLineWidth() / 2, detailedInfo.size + text.getLineHeight(), 0f);
        textNode.attachChild(text);
        textNode.addControl(new BillboardControl());
        node.attachChild(textNode);
        Util.attachCoordinateAxes(node, assetManager);
        rootNode.attachChild(node);
        return new ClientEntity(info, node, model);
    }
}
