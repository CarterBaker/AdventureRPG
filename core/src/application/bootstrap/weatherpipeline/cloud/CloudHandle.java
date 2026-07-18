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

    private CloudData cloudData;

    public void constructor(CloudData cloudData) {
        this.cloudData = cloudData;
    }

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

    public float getDensityNoiseScale() {
        return cloudData.getDensityNoiseScale();
    }

    public float getNoiseWarpStrength() {
        return cloudData.getNoiseWarpStrength();
    }

    public float getCoverageBias() {
        return cloudData.getCoverageBias();
    }

    public float getSilhouetteSoftness() {
        return cloudData.getSilhouetteSoftness();
    }

    public float getBaseAltitude() {
        return cloudData.getBaseAltitude();
    }

    public float getDriftSpeedScale() {
        return cloudData.getDriftSpeedScale();
    }
}