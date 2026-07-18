package application.bootstrap.weatherpipeline.cloud;

import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3;

public class CloudData extends DataPackage {

    /*
     * Immutable cloud archetype definition loaded from JSON — pure shape and
     * motion data for one named cloud (cumulus, nimbus, stratus, etc.).
     * Lighting is never per-archetype: every cloud is lit by the same real
     * sun/moon-driven shading in the shader, so two archetypes with identical
     * shape numbers always look identically lit and differ only in
     * silhouette, density, and placement. cloudColor is the one exception —
     * the cloud's base material tint — not a lighting response.
     */

    // Identity
    private final String cloudName;
    private final short cloudID;

    // Material Tint
    private final Vector3 cloudColor;

    // Shape
    private final float scale;
    private final float density;
    private final float verticalThickness;

    // Density Noise — silhouette detail and turbulence
    private final float densityNoiseScale;
    private final float noiseWarpStrength;
    private final float coverageBias;

    // Silhouette
    private final float silhouetteSoftness;

    // Placement
    private final float baseAltitude;

    // Motion
    private final float driftSpeedScale;

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
            float driftSpeedScale) {

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
}