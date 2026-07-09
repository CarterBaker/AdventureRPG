package application.bootstrap.weatherpipeline.cloud;

import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3;

public class CloudData extends DataPackage {

    /*
     * Immutable cloud archetype definition loaded from JSON. Holds shape,
     * color, altitude, motion, and lighting tuning for one named cloud.
     * Weathers reference these by name — never cloned, since a cloud
     * archetype carries no per-instance mutable state. Owned by CloudHandle
     * for the full engine session.
     */

    // Identity
    private final String cloudName;
    private final short cloudID;

    // Color
    private final Vector3 cloudColor;

    // Shape
    private final float scale;
    private final float density;
    private final float verticalThickness;
    private final float edgeSoftness;
    private final float puffJitter;

    // Placement
    private final float baseAltitude;

    // Motion
    private final float driftSpeedScale;

    // Lighting — drives the volumetric toon shading pass. shadowColor is
    // the tint blended in on a cloud's unlit/interior side (the "shade
    // band" of the toon look); shadeStrength controls how far that blend
    // reaches. rimLightStrength brightens the silhouette facing the light
    // source. ambientOcclusionStrength darkens deep/thick regions of the
    // volume. brightnessMultiplier is a final exposure scalar applied to
    // the lit color, letting e.g. a storm cloud read as inherently darker
    // than a cumulus cloud even under identical sun conditions.
    private final Vector3 shadowColor;
    private final float shadeStrength;
    private final float rimLightStrength;
    private final float ambientOcclusionStrength;
    private final float brightnessMultiplier;

    // Constructor \\

    public CloudData(
            String cloudName,
            short cloudID,
            Vector3 cloudColor,
            float scale,
            float density,
            float verticalThickness,
            float edgeSoftness,
            float puffJitter,
            float baseAltitude,
            float driftSpeedScale,
            Vector3 shadowColor,
            float shadeStrength,
            float rimLightStrength,
            float ambientOcclusionStrength,
            float brightnessMultiplier) {

        // Identity
        this.cloudName = cloudName;
        this.cloudID = cloudID;

        // Color
        this.cloudColor = cloudColor;

        // Shape
        this.scale = scale;
        this.density = density;
        this.verticalThickness = verticalThickness;
        this.edgeSoftness = edgeSoftness;
        this.puffJitter = puffJitter;

        // Placement
        this.baseAltitude = baseAltitude;

        // Motion
        this.driftSpeedScale = driftSpeedScale;

        // Lighting
        this.shadowColor = shadowColor;
        this.shadeStrength = shadeStrength;
        this.rimLightStrength = rimLightStrength;
        this.ambientOcclusionStrength = ambientOcclusionStrength;
        this.brightnessMultiplier = brightnessMultiplier;
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

    public float getEdgeSoftness() {
        return edgeSoftness;
    }

    public float getPuffJitter() {
        return puffJitter;
    }

    public float getBaseAltitude() {
        return baseAltitude;
    }

    public float getDriftSpeedScale() {
        return driftSpeedScale;
    }

    public Vector3 getShadowColor() {
        return shadowColor;
    }

    public float getShadeStrength() {
        return shadeStrength;
    }

    public float getRimLightStrength() {
        return rimLightStrength;
    }

    public float getAmbientOcclusionStrength() {
        return ambientOcclusionStrength;
    }

    public float getBrightnessMultiplier() {
        return brightnessMultiplier;
    }
}