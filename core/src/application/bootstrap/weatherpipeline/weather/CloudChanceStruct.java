package application.bootstrap.weatherpipeline.weather;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.StructPackage;
import engine.util.random.ChanceWeighted;

public class CloudChanceStruct extends StructPackage implements ChanceWeighted {

    /*
     * One cloud archetype a weather may spawn, paired with its relative
     * chance weight and an optional altitude override. A negative
     * altitudeOverride means "no override" — the cloud's own baseAltitude
     * applies. Overriding altitude per weather lets, for example, a storm
     * push its nimbus layer lower than that same cloud would sit under a
     * calmer condition, without needing a second cloud archetype.
     */

    // Sentinel — no per-weather altitude override, use the cloud's own
    // baseAltitude.
    public static final float NO_ALTITUDE_OVERRIDE = -1f;

    // Internal
    private final CloudHandle cloudHandle;
    private final float chance;
    private final float altitudeOverride;

    // Constructor \\

    public CloudChanceStruct(CloudHandle cloudHandle, float chance, float altitudeOverride) {

        // Internal
        this.cloudHandle = cloudHandle;
        this.chance = chance;
        this.altitudeOverride = altitudeOverride;
    }

    // Accessible \\

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
}