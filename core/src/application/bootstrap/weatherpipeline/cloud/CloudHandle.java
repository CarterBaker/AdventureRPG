package application.bootstrap.weatherpipeline.cloud;

import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class CloudHandle extends HandlePackage {

    /*
     * Persistent cloud archetype record. Wraps CloudData and delegates all
     * access through it. Registered in CloudManager from bootstrap to
     * shutdown. Referenced directly by weathers and overhead cells — never
     * cloned or mutated at runtime.
     */

    // Internal
    private CloudData cloudData;

    // Constructor \\

    public void constructor(CloudData cloudData) {
        this.cloudData = cloudData;
    }

    // Accessible \\

    public CloudData getCloudData() {
        return cloudData;
    }

    public String getCloudName() {
        return cloudData.getCloudName();
    }

    public short getCloudID() {
        return cloudData.getCloudID();
    }

    public Vector3 getCloudColor() {
        return cloudData.getCloudColor();
    }

    public float getScale() {
        return cloudData.getScale();
    }

    public float getDensity() {
        return cloudData.getDensity();
    }

    public float getVerticalThickness() {
        return cloudData.getVerticalThickness();
    }

    public float getEdgeSoftness() {
        return cloudData.getEdgeSoftness();
    }

    public float getPuffJitter() {
        return cloudData.getPuffJitter();
    }

    public float getBaseAltitude() {
        return cloudData.getBaseAltitude();
    }

    public float getDriftSpeedScale() {
        return cloudData.getDriftSpeedScale();
    }
}