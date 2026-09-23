package application.bootstrap.oceanpipeline.tidemanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.worldpipeline.util.TideUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;

public class TideManager extends ManagerPackage {

    /*
     * Owns the world's one tide. Every frame the global clock is resolved into
     * a tide offset from sea level: a daily cycle that crests at
     * OCEAN_TIDE_PEAK_TIME_OF_DAY, with its reach swinging between spring and
     * neap across OCEAN_TIDE_SPRING_NEAP_PERIOD_DAYS. The whole ocean shares
     * the result, so every chunk and every window agree on where the water
     * is. The surface is published twice: as a continuous height for the
     * shader, and as quantized liquid levels for world generation and the
     * tide pass, which world-streaming threads read without a lock.
     */

    // Internal
    private ClockManager clockManager;

    // Tide
    private float tideOffsetBlocks;
    private float surfaceHeightBlocks;
    private volatile int surfaceLevels;

    // Base \\

    @Override
    protected void create() {

        // Tide
        this.tideOffsetBlocks = 0f;
        this.surfaceHeightBlocks = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS + 1f;
        this.surfaceLevels = TideUtility.BASE_SURFACE_LEVELS;
    }

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        ClockHandle clockHandle = clockManager.getClockHandle();

        double elapsedDays = clockHandle.getTotalDaysElapsed() + clockHandle.getDayProgress();

        tideOffsetBlocks = (float) (computeAmplitudeBlocks(elapsedDays)
                * computeDailyCycle(clockHandle.getRawTimeOfDay()));
        surfaceHeightBlocks = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS + 1f + tideOffsetBlocks;
        surfaceLevels = TideUtility.toSurfaceLevels(surfaceHeightBlocks);
    }

    private double computeAmplitudeBlocks(double elapsedDays) {

        double springNeapPhase = elapsedDays / EngineSetting.OCEAN_TIDE_SPRING_NEAP_PERIOD_DAYS;
        double springFactor = 0.5 * (1.0 + Math.cos(springNeapPhase * Math.PI * 2.0));
        double neapRatio = EngineSetting.OCEAN_TIDE_NEAP_AMPLITUDE_RATIO;

        return EngineSetting.OCEAN_TIDE_AMPLITUDE_BLOCKS * (neapRatio + (1.0 - neapRatio) * springFactor);
    }

    private double computeDailyCycle(double rawTimeOfDay) {
        return Math.cos((rawTimeOfDay - EngineSetting.OCEAN_TIDE_PEAK_TIME_OF_DAY) * Math.PI * 2.0);
    }

    // Accessible \\

    public float getTideOffsetBlocks() {
        return tideOffsetBlocks;
    }

    public float getSurfaceHeightBlocks() {
        return surfaceHeightBlocks;
    }

    public int getSurfaceLevels() {
        return surfaceLevels;
    }
}
