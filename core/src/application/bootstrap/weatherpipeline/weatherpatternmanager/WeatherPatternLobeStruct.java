package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.StructPackage;

public class WeatherPatternLobeStruct extends StructPackage {

    /*
     * One fixed lobe within a larger WeatherPatternStruct — an offset, a
     * cloud choice, and a shape-variation seed, generated once at the
     * owning pattern's stream-in and never re-rolled.
     */

    private final float offsetChunkX;
    private final float offsetChunkZ;

    private final CloudHandle cloudHandle;
    private final float effectiveAltitude;

    private final float randomSeed;
    private final float sizeVariance;
    private final float domainRotation;

    public WeatherPatternLobeStruct(
            float offsetChunkX,
            float offsetChunkZ,
            CloudHandle cloudHandle,
            float effectiveAltitude,
            float randomSeed,
            float sizeVariance,
            float domainRotation) {

        this.offsetChunkX = offsetChunkX;
        this.offsetChunkZ = offsetChunkZ;
        this.cloudHandle = cloudHandle;
        this.effectiveAltitude = effectiveAltitude;
        this.randomSeed = randomSeed;
        this.sizeVariance = sizeVariance;
        this.domainRotation = domainRotation;
    }

    public float getOffsetChunkX() {
        return offsetChunkX;
    }

    public float getOffsetChunkZ() {
        return offsetChunkZ;
    }

    public CloudHandle getCloudHandle() {
        return cloudHandle;
    }

    public boolean hasCloud() {
        return cloudHandle != null;
    }

    public float getEffectiveAltitude() {
        return effectiveAltitude;
    }

    public float getRandomSeed() {
        return randomSeed;
    }

    public float getSizeVariance() {
        return sizeVariance;
    }

    public float getDomainRotation() {
        return domainRotation;
    }
}