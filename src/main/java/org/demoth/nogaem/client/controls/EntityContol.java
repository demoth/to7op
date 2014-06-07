package org.demoth.nogaem.client.controls;

import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

import static org.demoth.nogaem.common.Config.cl_lerp;

/**
 * @author demoth
 */
public class EntityContol extends AbstractControl {
    public float      posLerp;
    public float      rotLerp;
    public Vector3f   startPosition;
    public Vector3f   endPosition;
    public Quaternion startRotation;
    public Quaternion endRotation;
    public Quaternion currentRotation;

    public EntityContol(Spatial model) {
        super();
        endPosition = new Vector3f(model.getLocalTranslation());
        endRotation = new Quaternion(model.getLocalRotation());
        currentRotation = new Quaternion(model.getLocalRotation());
        posLerp = -1f;
        rotLerp = -1f;
        setSpatial(model);
        model.addControl(this);
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
