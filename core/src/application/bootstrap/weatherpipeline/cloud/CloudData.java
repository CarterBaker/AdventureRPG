package application.bootstrap.weatherpipeline.cloud;

import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3;

public class CloudData extends DataPackage {

    /*
     * Immutable "Cloud Settings" for one named cloud archetype, loaded from
     * JSON. Every value the weather shader needs to draw this archetype as a
     * layer of the sky — tint, shape, noise, placement, and motion — lives
     * here and only here. CloudHandle wraps this class and delegates every
     * accessor to it.
     */

    // Identity
    private final String cloudName;
    private final short cloudID;

    // Material Tint
    private final Vector3 cloudColor;
    private final float saturation;

    // Shape — width, height, fullness
    private final float scale;
    private final float density;
    private final float verticalThickness;
    private final float fullness;
    private final float elongation;

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

    // Constructor \\

    public CloudData(
            String cloudName,
            short cloudID,
            Vector3 cloudColor,
            float saturation,
            float scale,
            float density,
            float verticalThickness,
            float fullness,
            float elongation,
            float densityNoiseScale,
            float noiseWarpStrength,
            float coverageBias,
            float silhouetteSoftness,
            float baseAltitude,
            float driftSpeedScale) {

        this.cloudName = cloudName;
        this.cloudID = cloudID;
        this.cloudColor = cloudColor;
        this.saturation = saturation;
        this.scale = scale;
        this.density = density;
        this.verticalThickness = verticalThickness;
        this.fullness = fullness;
        this.elongation = elongation;
        this.densityNoiseScale = densityNoiseScale;
        this.noiseWarpStrength = noiseWarpStrength;
        this.coverageBias = coverageBias;
        this.silhouetteSoftness = silhouetteSoftness;
        this.baseAltitude = baseAltitude;
        this.driftSpeedScale = driftSpeedScale;
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

    public float getSaturation() {
        return saturation;
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

    public float getFullness() {
        return fullness;
    }

    public float getElongation() {
        return elongation;
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
}
