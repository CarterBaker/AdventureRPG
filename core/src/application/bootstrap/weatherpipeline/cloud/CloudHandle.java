package application.bootstrap.weatherpipeline.cloud;

import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class CloudHandle extends HandlePackage {

    /*
     * Persistent reference to one loaded cloud archetype. Every accessor
     * below delegates straight through to the wrapped CloudData — this
     * class holds no Cloud Setting of its own. cloudTypeIndex is the one
     * exception: it's a registry slot assigned by CloudManager at
     * registration time, not an authored setting, so it lives here instead.
     */

    private CloudData cloudData;
    private int cloudTypeIndex = -1;

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

    public float getSaturation() {
        return cloudData.getSaturation();
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

    public float getFullness() {
        return cloudData.getFullness();
    }

    public float getElongation() {
        return cloudData.getElongation();
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

    // Cloud Type Registry \\

    public void assignCloudTypeIndex(int cloudTypeIndex) {
        this.cloudTypeIndex = cloudTypeIndex;
    }

    public int getCloudTypeIndex() {
        return cloudTypeIndex;
    }
}
