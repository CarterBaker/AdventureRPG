package program.bootstrap.lightingpipeline.naturallightmanager;

import program.bootstrap.calendarpipeline.clockmanager.ClockManager;
import program.core.engine.SystemPackage;
import program.core.settings.EngineSetting;
import program.core.util.mathematics.vectors.Vector3;

public class MoonLightSystem extends SystemPackage {

    /*
     * Computes moon direction, color, and intensity each frame from the current
     * visual time of day and lunar phase. Moon is offset half a cycle from the
     * sun. Intensity and color tint scale with the current lunar phase.
     */

    // Output
    private final Vector3 direction = new Vector3();
    private final Vector3 color = new Vector3();
    private float intensity;

    // Internal
    private ClockManager clockManager;

    // Settings
    private int LUNAR_CYCLE_DAYS;
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
        this.LUNAR_CYCLE_DAYS = EngineSetting.LUNAR_CYCLE_DAYS;
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

        float moonT = (visualTimeOfDay + 0.5f) % 1.0f;
        float angle = moonT * (float) Math.PI * 2f;
        float dirX = -(float) Math.sin(angle);
        float dirY = -(float) Math.cos(angle);
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);

        if (len > 0f) {
            dirX /= len;
            dirY /= len;
        }

        float lunarPhase = computeLunarPhase();
        float brightness = MOON_BRIGHTNESS_BASE + lunarPhase * MOON_BRIGHTNESS_LUNAR_SCALE;

        direction.set(dirX, dirY, 0f);
        color.set(brightness * MOON_COLOR_R, brightness * MOON_COLOR_G, brightness * MOON_COLOR_B);
        intensity = computeIntensity(moonT, lunarPhase);
    }

    private float computeLunarPhase() {

        long totalDays = clockManager.getClockHandle().getTotalDaysElapsed();
        float cycleProgress = (totalDays % LUNAR_CYCLE_DAYS) / (float) LUNAR_CYCLE_DAYS;

        return (1f + (float) Math.sin(cycleProgress * Math.PI * 2f - Math.PI / 2f)) / 2f;
    }

    private float computeIntensity(float moonT, float lunarPhase) {

        float distFromMoonNoon = Math.abs(moonT - 0.5f) * 2f;

        if (distFromMoonNoon > MOON_HORIZON_CUTOFF)
            return 0f;

        float blend = 1f - (distFromMoonNoon / MOON_HORIZON_CUTOFF);
        float phaseScale = MOON_PHASE_MIN + lunarPhase * MOON_PHASE_MAX;

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