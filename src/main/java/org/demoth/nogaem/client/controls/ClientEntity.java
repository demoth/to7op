package org.demoth.nogaem.client.controls;

import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import org.demoth.nogaem.common.*;
import org.demoth.nogaem.common.entities.Entity;

import static org.demoth.nogaem.common.Config.cl_float_offset;
import static org.demoth.nogaem.common.Config.cl_lerp;

/**
 * @author demoth
 */
public class ClientEntity extends AbstractControl {
    public Entity     entity;
    public Spatial    visible;
    public float      posLerp;
    public float      rotLerp;
    public Vector3f   startPosition;
    public Vector3f   endPosition;
    public Quaternion startRotation;
    public Quaternion endRotation;
    public Quaternion currentRotation;

    // floating effect
    public float floatTime = 0f;

    public ClientEntity(Entity entity, Spatial node, Spatial visible) {
        super();
        this.entity = entity;
        this.visible = visible;
        endPosition = new Vector3f(node.getLocalTranslation());
        endRotation = new Quaternion(node.getLocalRotation());
        currentRotation = new Quaternion(node.getLocalRotation());
        posLerp = -1f;
        rotLerp = -1f;
        setSpatial(node);
        node.addControl(this);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (posLerp >= 0) {
            getSpatial().setLocalTranslation(FastMath.interpolateLinear(posLerp / cl_lerp, startPosition, endPosition));
            posLerp += tpf;
            if (posLerp > cl_lerp)
                posLerp = -1f;
        }
        if (rotLerp >= 0) {
            getSpatial().setLocalRotation(currentRotation.slerp(startRotation, endRotation, rotLerp / cl_lerp));
            rotLerp += tpf;
            if (rotLerp > cl_lerp)
                rotLerp = -1f;
        }
        if ((entity.effects & Constants.Effects.ROTATE_Z) > 0)
            visible.rotate(tpf * 10, 0f, 0f);
        if ((entity.effects & Constants.Effects.FLOATING) > 0) {
            floatTime += tpf;
            // assuming visible.getLocalTranslation = 0
            Vector3f newLocation = new Vector3f(0f, cl_float_offset * FastMath.sin(10f * floatTime), 0f);
            visible.setLocalTranslation(newLocation);
            System.out.println("floating:\nnewLocation = " + newLocation + "\nfloatTime = " + floatTime);

        }

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    public void rotateLerp(Quaternion rot) {
        if (rot != null) {
            startRotation = new Quaternion(getSpatial().getLocalRotation());
            endRotation = rot;
            currentRotation = new Quaternion(startRotation);
            rotLerp = 0f;
        }
    }

    public void moveLerp(Vector3f pos) {
        if (pos != null) {
            startPosition = new Vector3f(getSpatial().getLocalTranslation());
            endPosition = pos;
            posLerp = 0f;
        }
    }
}
