package com.internal.bootstrap.lightingpipeline.directionallight;

import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class DirectionalLightHandle extends HandlePackage {

    /*
     * Runtime directional light state pushed to the GPU each frame. Holds
     * direction, color, and intensity for the active dominant light source.
     * Owned by NaturalLightManager — push routes through UBOManager.
     */

    // Internal
    private UBOManager uboManager;

    // Light Properties
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
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    // Constructor \\

    public void constructor(UBOHandle uboHandle) {
        this.uboHandle = uboHandle;
    }

    // Push to GPU \\

    public void push() {
        uboHandle.updateUniform("u_lightDirection", direction);
        uboHandle.updateUniform("u_lightColor", color);
        uboHandle.updateUniform("u_lightIntensity", intensity);
        uboManager.push(uboHandle);
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

    public UBOHandle getUBOHandle() {
        return uboHandle;
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
}