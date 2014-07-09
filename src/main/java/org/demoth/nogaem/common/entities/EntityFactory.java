package org.demoth.nogaem.common.entities;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.font.BitmapText;
import com.jme3.scene.*;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Box;
import org.demoth.nogaem.client.controls.ClientEntity;
import org.demoth.nogaem.common.Util;
import org.slf4j.*;

import java.util.Map;

import static org.demoth.nogaem.common.Config.gamedir;

/**
 * @author demoth
 */
public class EntityFactory {
    static final Logger log = LoggerFactory.getLogger(EntityFactory.class);
    private AssetManager                     assetManager;
    private Node                             rootNode;
    private Map<Integer, EntityDetailedInfo> detailedInfoMap;

    public EntityFactory(AssetManager assetManager, Node rootNode) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.detailedInfoMap = Util.parseCsv(gamedir + "/entities.csv");
    }

    public ClientEntity createClientEntity(EntityInfo info, boolean playSounds) {
        Node node = new Node(info.name);
        EntityDetailedInfo detailedInfo = detailedInfoMap.get(info.typeId);
        if (detailedInfo == null) {
            log.error("Detailed info for typeId:" + info.typeId + " not found");
            return null;
        }
        // visual representation
        Spatial model = assetManager.loadModel("models/" + detailedInfo.modelName);
        node.attachChild(model);

        // bounding box
        Geometry bounds = new Geometry(info.name + "BB", new Box(detailedInfo.size, detailedInfo.size, detailedInfo.size));
        bounds.setMaterial(Util.createBoundBoxMaterial(assetManager));
        node.attachChild(bounds);

        // textual information
        Node textNode = new Node();
        BitmapText text = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
        text.setText(info.name);
        text.setSize(1f);
        text.move(-text.getLineWidth() / 2, detailedInfo.size + text.getLineHeight(), 0f);
        textNode.attachChild(text);
        textNode.addControl(new BillboardControl());
        node.attachChild(textNode);

        if (playSounds) {
            // audio
            AudioNode audio = new AudioNode(assetManager, "sounds/" + detailedInfo.appearSound);
            audio.setPositional(true);
            audio.setLooping(false);
            node.attachChild(audio);
            audio.play();
        }
        // coordinate axes
        Util.attachCoordinateAxes(node, assetManager);

        rootNode.attachChild(node);
        return new ClientEntity(info, node, model);
    }
}
