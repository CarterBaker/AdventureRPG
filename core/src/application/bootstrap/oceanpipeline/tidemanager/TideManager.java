package application.bootstrap.oceanpipeline.tidemanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.calendarpipeline.util.CelestialUtility;
import application.bootstrap.worldpipeline.util.TideUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;

public class TideManager extends ManagerPackage {

    /*
     * Owns the world's one tide, raised by the moon itself. Every frame the
     * global clock places the moon and the sun on their paths exactly as the
     * sky draws them, and each lifts the sea into two bulges, one beneath it
     * and one opposite, so high water follows the moon overhead and underfoot
     * and arrives later every day. The moon pulls harder the closer it passes
     * and not at all in a calendar without one; the weaker sun adds springs at
     * new and full moon and neaps at the quarters. The surface is published
     * twice: as a continuous height every renderer and physics read, and as
     * quantized liquid levels held a lead above it, so the voxel sea already
     * covers wherever the live surface is about to be on the way up and
     * still covers it on the way down, and no chunk is ever caught short of
     * the water drawn over it. World-streaming threads read the levels
     * without a lock.
     */

    // Internal
    private ClockManager clockManager;

    // Tide
    private float tideOffsetBlocks;
    private float surfaceHeightBlocks;
    private volatile int surfaceLevels;

    // Scratch
    private final Vector3 bodyDirection = new Vector3();

    // Base \\

    @Override
    protected void create() {

        // Tide
        this.tideOffsetBlocks = 0f;
        this.surfaceHeightBlocks = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS + 1f;
        this.surfaceLevels = TideUtility.toVoxelSurfaceLevels(surfaceHeightBlocks);
    }

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        ClockHandle clockHandle = clockManager.getClockHandle();
        double timeOfDay = clockHandle.getRawTimeOfDay();
        float sizeScale = clockHandle.getLunarSizeScale();

        CelestialUtility.resolveOrbitDirection(
                CelestialUtility.wrapFraction(timeOfDay - clockHandle.getLunarPhase()),
                clockHandle.getLunarLatitude(),
                bodyDirection);
        double lunarPull = EngineSetting.OCEAN_TIDE_LUNAR_SHARE * sizeScale * sizeScale * sizeScale
                * computeBulge(bodyDirection);

        CelestialUtility.resolveSunDirection(timeOfDay, bodyDirection);
        double solarPull = EngineSetting.OCEAN_TIDE_SOLAR_SHARE * computeBulge(bodyDirection);

        tideOffsetBlocks = (float) (EngineSetting.OCEAN_TIDE_AMPLITUDE_BLOCKS
                * Math.max(-1.0, Math.min(1.0, lunarPull + solarPull)));
        surfaceHeightBlocks = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS + 1f + tideOffsetBlocks;
        surfaceLevels = TideUtility.toVoxelSurfaceLevels(surfaceHeightBlocks);
    }

    // Height of the two tidal bulges a body raises under a direction toward it: 1 with the body overhead or
    // underfoot, -1 with it on the horizon
    private double computeBulge(Vector3 direction) {
        return 2.0 * direction.y * direction.y - 1.0;
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
