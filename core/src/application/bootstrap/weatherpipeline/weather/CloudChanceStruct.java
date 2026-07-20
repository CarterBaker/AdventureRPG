package application.bootstrap.weatherpipeline.weather;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.StructPackage;
import engine.util.random.ChanceWeighted;

public class CloudChanceStruct extends StructPackage implements ChanceWeighted {

    /*
     * One cloud archetype a weather may spawn, paired with its relative
     * chance weight, an optional altitude override, and a per-weather
     * density multiplier applied on top of the cloud's own base density —
     * lets one weather run the same archetype thinner or thicker than
     * another without needing a second archetype.
     */

    public static final float NO_ALTITUDE_OVERRIDE = -1f;
    public static final float DEFAULT_DENSITY_MULTIPLIER = 1.0f;

    private final CloudHandle cloudHandle;
    private final float chance;
    private final float altitudeOverride;
    private final float densityMultiplier;

    public CloudChanceStruct(CloudHandle cloudHandle, float chance, float altitudeOverride, float densityMultiplier) {
        this.cloudHandle = cloudHandle;
        this.chance = chance;
        this.altitudeOverride = altitudeOverride;
        this.densityMultiplier = densityMultiplier;
    }

    public CloudHandle getCloudHandle() {
        return cloudHandle;
    }

    @Override
    public float getChance() {
        return chance;
    }

    public float getAltitudeOverride() {
        return altitudeOverride;
    }

    public float getEffectiveAltitude() {
        return altitudeOverride >= 0f ? altitudeOverride : cloudHandle.getBaseAltitude();
    }

    public float getDensityMultiplier() {
        return densityMultiplier;
    }
}