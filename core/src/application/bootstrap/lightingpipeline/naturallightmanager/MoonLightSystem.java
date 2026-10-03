package application.bootstrap.lightingpipeline.naturallightmanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.calendarpipeline.util.CelestialUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class MoonLightSystem extends SystemPackage {

    /*
     * Computes moon direction, color and intensity each frame from time of day
     * and the moon's place in its orbit, read live from the clock so it follows
     * world switches. The moon trails the sun by its phase — beside the sun
     * when new, opposite it when full — so it rises later every day, and rides
     * above or below the sun's path by its orbital latitude. Its light grows
     * with how much of its face is lit and with how close it passes, and a
     * calendar with no moon gives none.
     */

    // Output
    private final Vector3 direction = new Vector3();
    private final Vector3 color = new Vector3();
    private float intensity;

    // Internal
    private ClockManager clockManager;

    // Settings
    private float MOON_BRIGHTNESS_BASE;
    private float MOON_BRIGHTNESS_LUNAR_SCALE;
    private float MOON_COLOR_R;
    private float MOON_COLOR_G;
    private float MOON_COLOR_B;
    private float MOON_HORIZON_CUTOFF;
    private float MOON_PHASE_MIN;
    private float MOON_PHASE_MAX;
    private float MOON_MAX_INTENSITY;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.MOON_BRIGHTNESS_BASE = EngineSetting.MOON_BRIGHTNESS_BASE;
        this.MOON_BRIGHTNESS_LUNAR_SCALE = EngineSetting.MOON_BRIGHTNESS_LUNAR_SCALE;
        this.MOON_COLOR_R = EngineSetting.MOON_COLOR_R;
        this.MOON_COLOR_G = EngineSetting.MOON_COLOR_G;
        this.MOON_COLOR_B = EngineSetting.MOON_COLOR_B;
        this.MOON_HORIZON_CUTOFF = EngineSetting.MOON_HORIZON_CUTOFF;
        this.MOON_PHASE_MIN = EngineSetting.MOON_PHASE_MIN;
        this.MOON_PHASE_MAX = EngineSetting.MOON_PHASE_MAX;
        this.MOON_MAX_INTENSITY = EngineSetting.MOON_MAX_INTENSITY;
    }

    @Override
    protected void get() {

        // Internal
        this.clockManager = get(ClockManager.class);
    }

    // Update \\

    public void update(float visualTimeOfDay) {

        ClockHandle clockHandle = clockManager.getClockHandle();
        float illumination = clockHandle.getLunarIllumination();
        float sizeScale = clockHandle.getLunarSizeScale();
        float moonT = (float) CelestialUtility.wrapFraction(visualTimeOfDay - clockHandle.getLunarPhase());

        CelestialUtility.resolveOrbitDirection(moonT, clockHandle.getLunarLatitude(), direction);

        float brightness = MOON_BRIGHTNESS_BASE + illumination * MOON_BRIGHTNESS_LUNAR_SCALE;

        color.set(brightness * MOON_COLOR_R, brightness * MOON_COLOR_G, brightness * MOON_COLOR_B);
        intensity = computeIntensity(moonT, illumination) * sizeScale * sizeScale;
    }

    private float computeIntensity(float moonT, float illumination) {

        float distFromMoonNoon = Math.abs(moonT - 0.5f) * 2f;

        if (distFromMoonNoon > MOON_HORIZON_CUTOFF)
            return 0f;

        float blend = 1f - (distFromMoonNoon / MOON_HORIZON_CUTOFF);
        float phaseScale = MOON_PHASE_MIN + illumination * MOON_PHASE_MAX;

        return blend * blend * phaseScale * MOON_MAX_INTENSITY;
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
