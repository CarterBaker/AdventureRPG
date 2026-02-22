package com.internal.bootstrap.lightingpipeline.naturallightmanager;

import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class SunLightSystem extends SystemPackage {

    // Output — written each frame, read by NaturalLightManager
    private final Vector3 direction = new Vector3();
    private final Vector3 color = new Vector3(1f, 1f, 1f);
    private float intensity;

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
        intensity = computeIntensity(visualTimeOfDay);
    }

    private float computeIntensity(float t) {
        float distFromNoon = Math.abs(t - 0.5f) * 2f;
        if (distFromNoon > 0.85f)
            return 0f;
        float blend = 1f - (distFromNoon / 0.85f);
        return blend * blend;
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
}