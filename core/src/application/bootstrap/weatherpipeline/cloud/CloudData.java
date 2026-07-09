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
     *
     * Volumetric/toon fields (topColor, toonBands, densityNoiseScale,
     * noiseWarpStrength, coverageBias, silhouetteSoftness) back the
     * raymarched volumetric cloud shader landing in the next stage.
     * topColor lets toon shading blend from cloudColor (shadowed/base
     * tint) toward a brighter top-facing tint without a second lighting
     * pass; toonBands posterizes that blend into discrete steps for the
     * toon look. densityNoiseScale/noiseWarpStrength drive a 3D fbm
     * density field sampled in world space, so no two instances of the
     * same archetype look identical — each warps based on its own world
     * position rather than a per-instance seed alone. coverageBias shifts
     * the density threshold separating "inside the cloud" from "empty
     * sky" — live weather cloudCoverage will bias this further at render
     * time. silhouetteSoftness controls how soft the raymarched edge
     * falloff is where the density field crosses that threshold.
     *
     * edgeSoftness/puffJitter are the old card-shader tuning knobs — left
     * in place for now since CloudVolumeShader still reads them; they are
     * superseded by silhouetteSoftness/noiseWarpStrength once the shader
     * rework lands and will be removed then.
     */

    // Identity
    private final String cloudName;
    private final short cloudID;

    // Color
    private final Vector3 cloudColor;
    private final Vector3 topColor;

    // Shape
    private final float scale;
    private final float density;
    private final float verticalThickness;
    private final float edgeSoftness;
    private final float puffJitter;

    // Toon Shading
    private final int toonBands;

    // Density Noise
    private final float densityNoiseScale;
    private final float noiseWarpStrength;
    private final float coverageBias;

    // Silhouette
    private final float silhouetteSoftness;

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
            Vector3 topColor,
            float scale,
            float density,
            float verticalThickness,
            float edgeSoftness,
            float puffJitter,
            int toonBands,
            float densityNoiseScale,
            float noiseWarpStrength,
            float coverageBias,
            float silhouetteSoftness,
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
        this.topColor = topColor;

        // Shape
        this.scale = scale;
        this.density = density;
        this.verticalThickness = verticalThickness;
        this.edgeSoftness = edgeSoftness;
        this.puffJitter = puffJitter;

        // Toon Shading
        this.toonBands = toonBands;

        // Density Noise
        this.densityNoiseScale = densityNoiseScale;
        this.noiseWarpStrength = noiseWarpStrength;
        this.coverageBias = coverageBias;

        // Silhouette
        this.silhouetteSoftness = silhouetteSoftness;

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

    public Vector3 getTopColor() {
        return topColor;
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

    public int getToonBands() {
        return toonBands;
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