package com.internal.bootstrap.lightingpipeline.directionallight;

import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class DirectionalLightHandle extends HandlePackage {

    // Light properties — readable for future entity light level queries
    private Vector3 direction;
    private Vector3 color;
    private float intensity;

    // UBO
    private UBOHandle uboHandle;

    // Internal \\

    @Override
    protected void create() {
        this.direction = new Vector3();
        this.color = new Vector3();
        this.intensity = 0f;
    }

    public void constructor(UBOHandle uboHandle) {
        this.uboHandle = uboHandle;
    }

    // Push to GPU \\

    public void push() {
        uboHandle.updateUniform("u_lightDirection", direction);
        uboHandle.updateUniform("u_lightColor", color);
        uboHandle.updateUniform("u_lightIntensity", intensity);
        uboHandle.push();
    }

    // Accessible \\

    public Vector3 getDirection() {
        return direction;
    }

    public Vector3 getColor() {
        return color;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setDirection(float x, float y, float z) {
        direction.set(x, y, z);
    }

    public void setColor(float r, float g, float b) {
        color.set(r, g, b);
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public UBOHandle getUBOHandle() {
        return uboHandle;
    }
}