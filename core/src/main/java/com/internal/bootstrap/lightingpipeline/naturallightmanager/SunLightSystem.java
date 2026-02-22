package com.internal.bootstrap.lightingpipeline.naturallightmanager;

import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class SunLightSystem extends SystemPackage {

    // Permanent fields — updated each frame, no allocation
    private final Vector3 direction = new Vector3();
    private final Vector3 color = new Vector3(1f, 1f, 1f);
    private float intensity;

    private UBOHandle ubo;

    // Called by NaturalLightManager after UBO is ready
    public void constructor(UBOHandle ubo) {
        this.ubo = ubo;
    }

    // Update \\

    public void update(float visualTimeOfDay) {

        float angle = visualTimeOfDay * (float) Math.PI * 2f;

        float dirX = -(float) Math.sin(angle);
        float dirY = -(float) Math.cos(angle);
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len > 0f) {
            dirX /= len;
            dirY /= len;
        }

        direction.set(dirX, dirY, 0f);
        color.set(1f, 1f, 1f);
        intensity = getIntensity(visualTimeOfDay);

        ubo.updateUniform("u_sunDirection", direction);
        ubo.updateUniform("u_sunColor", color);
        ubo.updateUniform("u_sunIntensity", intensity);
    }

    private float getIntensity(float t) {
        float distFromNoon = Math.abs(t - 0.5f) * 2f;
        if (distFromNoon > 0.85f)
            return 0f;
        float blend = 1f - (distFromNoon / 0.85f);
        return blend * blend;
    }
}