package application.bootstrap.weatherpipeline.cloud;

import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3;

public class CloudData extends DataPackage {

    /*
     * Immutable cloud archetype definition loaded from JSON. Holds shape,
     * color, altitude, and motion tuning for one named cloud. Weathers
     * reference these by name — never cloned, since a cloud archetype
     * carries no per-instance mutable state. Owned by CloudHandle for the
     * full engine session.
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
            float driftSpeedScale) {

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
}