package application.bootstrap.lightingpipeline.naturallightmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class MoonLightSystem extends SystemPackage {

    /*
     * Computes moon direction, color, and intensity each frame from the current
     * visual time of day and lunar phase. Moon is offset half a cycle from the
     * sun. Intensity and color tint scale with the current lunar phase.
     *
     * Lunar cycle length is calendar data now (it can differ per world), so it
     * is read live off the active CalendarHandle via ClockManager each time the
     * phase is computed, rather than cached once at create() — this keeps it
     * correct across a switchWorld() calendar swap too.
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

        int lunarCycleDays = clockManager.getClockHandle().getCalendarHandle().getLunarCycleDays();

        if (lunarCycleDays <= 0)
            return 0f;

        long totalDays = clockManager.getClockHandle().getTotalDaysElapsed();
        float cycleProgress = (totalDays % lunarCycleDays) / (float) lunarCycleDays;

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