package application.bootstrap.weatherpipeline.cloud;

import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3;

public class CloudData extends DataPackage {

    /*
     * Immutable cloud archetype definition loaded from JSON — shape, motion,
     * and placement-variance data for one named cloud. Lighting is never
     * per-archetype; only cloudColor tints the shared real-time shading.
     */

    // Identity
    private final String cloudName;
    private final short cloudID;

    // Material Tint
    private final Vector3 cloudColor;

    // Shape — width, height
    private final float scale;
    private final float density;
    private final float verticalThickness;

    // Density Noise
    private final float densityNoiseScale;
    private final float noiseWarpStrength;
    private final float coverageBias;

    // Silhouette
    private final float silhouetteSoftness;

    // Placement — elevation
    private final float baseAltitude;

    // Motion
    private final float driftSpeedScale;

    // Instance Variation — spread, and per-instance size/shape jitter range
    private final float spreadRatio;
    private final float sizeVarianceMin;
    private final float sizeVarianceMax;
    private final float elongationMin;
    private final float elongationMax;

    // Constructor \\

    public CloudData(
            String cloudName,
            short cloudID,
            Vector3 cloudColor,
            float scale,
            float density,
            float verticalThickness,
            float densityNoiseScale,
            float noiseWarpStrength,
            float coverageBias,
            float silhouetteSoftness,
            float baseAltitude,
            float driftSpeedScale,
            float spreadRatio,
            float sizeVarianceMin,
            float sizeVarianceMax,
            float elongationMin,
            float elongationMax) {

        this.cloudName = cloudName;
        this.cloudID = cloudID;
        this.cloudColor = cloudColor;
        this.scale = scale;
        this.density = density;
        this.verticalThickness = verticalThickness;
        this.densityNoiseScale = densityNoiseScale;
        this.noiseWarpStrength = noiseWarpStrength;
        this.coverageBias = coverageBias;
        this.silhouetteSoftness = silhouetteSoftness;
        this.baseAltitude = baseAltitude;
        this.driftSpeedScale = driftSpeedScale;
        this.spreadRatio = spreadRatio;
        this.sizeVarianceMin = sizeVarianceMin;
        this.sizeVarianceMax = sizeVarianceMax;
        this.elongationMin = elongationMin;
        this.elongationMax = elongationMax;
    }

    // Accessible \\

    public String getCloudName() {
        return cloudName;
    }

    public short getCloudID() {
        return cloudID;
    }

    public Vector3 getCloudColor() {
        return cloudColor;
    }

    public float getScale() {
        return scale;
    }

    public float getDensity() {
        return density;
    }

    public float getVerticalThickness() {
        return verticalThickness;
    }

    public float getDensityNoiseScale() {
        return densityNoiseScale;
    }

    public float getNoiseWarpStrength() {
        return noiseWarpStrength;
    }

    public float getCoverageBias() {
        return coverageBias;
    }

    public float getSilhouetteSoftness() {
        return silhouetteSoftness;
    }

    public float getBaseAltitude() {
        return baseAltitude;
    }

    public float getDriftSpeedScale() {
        return driftSpeedScale;
    }

    public float getSpreadRatio() {
        return spreadRatio;
    }

    public float getSizeVarianceMin() {
        return sizeVarianceMin;
    }

    public float getSizeVarianceMax() {
        return sizeVarianceMax;
    }

    public float getElongationMin() {
        return elongationMin;
    }

    public float getElongationMax() {
        return elongationMax;
    }
}