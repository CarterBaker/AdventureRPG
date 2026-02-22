package com.internal.bootstrap.lightingpipeline.naturallightmanager;

import com.internal.bootstrap.calendarpipeline.clockmanager.ClockManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.vectors.Vector3;

public class MoonLightSystem extends SystemPackage {

    // Permanent fields — updated each frame, no allocation
    private final Vector3 direction = new Vector3();
    private final Vector3 color = new Vector3();
    private float intensity;

    private ClockManager clockManager;
    private UBOHandle ubo;

    // Internal \\

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
    }

    // Called by NaturalLightManager after UBO is ready
    public void constructor(UBOHandle ubo) {
        this.ubo = ubo;
    }

    // Update \\

    public void update(float visualTimeOfDay) {

        float moonT = (visualTimeOfDay + 0.5f) % 1.0f;
        float angle = moonT * (float) Math.PI * 2f;

        float dirX = -(float) Math.sin(angle);
        float dirY = -(float) Math.cos(angle);
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len > 0f) {
            dirX /= len;
            dirY /= len;
        }

        float lunarPhase = getLunarPhase();
        float brightness = 0.7f + lunarPhase * 0.3f;

        direction.set(dirX, dirY, 0f);
        color.set(brightness * 0.75f, brightness * 0.85f, brightness * 1.0f);
        intensity = getIntensity(moonT, lunarPhase);

        ubo.updateUniform("u_moonDirection", direction);
        ubo.updateUniform("u_moonColor", color);
        ubo.updateUniform("u_moonIntensity", intensity);
    }

    // 0 = new moon, 0.5 = full moon, 1 = new moon
    private float getLunarPhase() {
        long totalDays = clockManager.getClockHandle().getTotalDaysElapsed();
        float cycleProgress = (totalDays % EngineSetting.LUNAR_CYCLE_DAYS) / (float) EngineSetting.LUNAR_CYCLE_DAYS;
        return (1f + (float) Math.sin(cycleProgress * Math.PI * 2f - Math.PI / 2f)) / 2f;
    }

    private float getIntensity(float moonT, float lunarPhase) {
        float distFromMoonNoon = Math.abs(moonT - 0.5f) * 2f;
        if (distFromMoonNoon > 0.85f)
            return 0f;
        float blend = 1f - (distFromMoonNoon / 0.85f);
        float phaseScale = 0.05f + lunarPhase * 0.95f;
        return blend * blend * phaseScale * 0.25f;
    }

    // Accessible \\

    public float getLunarPhaseNormalized() {
        return getLunarPhase();
    }
}