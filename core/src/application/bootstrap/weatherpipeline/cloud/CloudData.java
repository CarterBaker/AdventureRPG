package application.bootstrap.weatherpipeline.cloud;

import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3;

public class CloudData extends DataPackage {

    /*
     * Immutable "Cloud Settings" for one named cloud archetype, loaded from
     * JSON. Every value the weather shader needs to draw this archetype as a
     * layer of the sky — tint, shape, noise, placement, and motion — lives
     * here and only here. Sizes and elevations are real-world kilometres,
     * converted into blocks through the active world's own scale when the
     * weather map is written. CloudHandle wraps this class and delegates
     * every accessor to it.
     */

    // Identity
    private final String cloudName;
    private final short cloudID;

    // Material Tint
    private final Vector3 cloudColor;
    private final float saturation;

    // Shape — width, height, fullness
    private final float scaleKm;
    private final float density;
    private final float verticalThicknessKm;
    private final float fullness;
    private final float elongation;

    // Density Noise
    private final float densityNoiseScale;
    private final float noiseWarpStrength;
    private final float coverageBias;

    // Silhouette
    private final float silhouetteSoftness;

    // Placement — elevation above sea level
    private final float baseAltitudeKm;

    // Motion
    private final float driftSpeedScale;

    // Constructor \\

    public CloudData(
            String cloudName,
            short cloudID,
            Vector3 cloudColor,
            float saturation,
            float scaleKm,
            float density,
            float verticalThicknessKm,
            float fullness,
            float elongation,
            float densityNoiseScale,
            float noiseWarpStrength,
            float coverageBias,
            float silhouetteSoftness,
            float baseAltitudeKm,
            float driftSpeedScale) {

        this.cloudName = cloudName;
        this.cloudID = cloudID;
        this.cloudColor = cloudColor;
        this.saturation = saturation;
        this.scaleKm = scaleKm;
        this.density = density;
        this.verticalThicknessKm = verticalThicknessKm;
        this.fullness = fullness;
        this.elongation = elongation;
        this.densityNoiseScale = densityNoiseScale;
        this.noiseWarpStrength = noiseWarpStrength;
        this.coverageBias = coverageBias;
        this.silhouetteSoftness = silhouetteSoftness;
        this.baseAltitudeKm = baseAltitudeKm;
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

    public float getScaleKm() {
        return scaleKm;
    }

    public float getDensity() {
        return density;
    }

    public float getVerticalThicknessKm() {
        return verticalThicknessKm;
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

    public float getBaseAltitudeKm() {
        return baseAltitudeKm;
    }

    public float getDriftSpeedScale() {
        return driftSpeedScale;
    }
}
