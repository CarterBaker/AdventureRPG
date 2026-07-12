package application.bootstrap.weatherpipeline.overheadmanager;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.StructPackage;

class WeatherPatternLobeStruct extends StructPackage {

    /*
     * One fixed physical approximation-lobe within a larger
     * WeatherPatternStruct — see that class's own doc comment. Represents
     * exactly one cloud-volume mesh instance's worth of placement and shape
     * data, generated once at the owning pattern's stream-in and never
     * re-rolled for its lifetime.
     *
     * offsetChunkX/Z is this lobe's own fixed position relative to the
     * pattern's home center, in chunk units — never re-rolled, and never
     * itself wrapped or re-centered; only the pattern's own home position
     * and accumulated drift are (see WeatherPatternStruct). A lobe's
     * offset radius is allowed to exceed half the pattern's own streaming
     * cell size — real weather systems blend into their neighbors at the
     * edges rather than stopping at a hard grid line, so a small amount of
     * overlap between two neighboring patterns' outer lobes is expected
     * and intentional, not a bug.
     *
     * cloudHandle may be null — a lobe whose own per-lobe coverage roll
     * missed (see OverheadManager.streamInPattern()) draws nothing,
     * mirroring exactly how a whole cell could previously resolve to "no
     * cloud" under a Clear weather or a coverage-roll miss. A null-cloud
     * lobe costs nothing at render time since CloudRenderSystem skips it
     * outright — see OverheadCellStruct.hasCloud().
     *
     * effectiveAltitude, randomSeed, sizeVariance, and domainRotation
     * mirror the exact same fields the previous per-cell design already
     * carried — see OverheadCellStruct's own doc comment for the full
     * rationale behind each.
     */

    // Position — fixed relative to the owning pattern's own home center
    private final float offsetChunkX;
    private final float offsetChunkZ;

    // Cloud Choice — may be null; see class comment
    private final CloudHandle cloudHandle;
    private final float effectiveAltitude;

    // Shape Diversity — stable for this lobe's entire lifetime
    private final float randomSeed;
    private final float sizeVariance;
    private final float domainRotation;

    // Constructor \\

    WeatherPatternLobeStruct(
            float offsetChunkX,
            float offsetChunkZ,
            CloudHandle cloudHandle,
            float effectiveAltitude,
            float randomSeed,
            float sizeVariance,
            float domainRotation) {

        // Position
        this.offsetChunkX = offsetChunkX;
        this.offsetChunkZ = offsetChunkZ;

        // Cloud Choice
        this.cloudHandle = cloudHandle;
        this.effectiveAltitude = effectiveAltitude;

        // Shape Diversity
        this.randomSeed = randomSeed;
        this.sizeVariance = sizeVariance;
        this.domainRotation = domainRotation;
    }

    // Accessible \\

    float getOffsetChunkX() {
        return offsetChunkX;
    }

    float getOffsetChunkZ() {
        return offsetChunkZ;
    }

    CloudHandle getCloudHandle() {
        return cloudHandle;
    }

    boolean hasCloud() {
        return cloudHandle != null;
    }

    float getEffectiveAltitude() {
        return effectiveAltitude;
    }

    float getRandomSeed() {
        return randomSeed;
    }

    float getSizeVariance() {
        return sizeVariance;
    }

    float getDomainRotation() {
        return domainRotation;
    }
}