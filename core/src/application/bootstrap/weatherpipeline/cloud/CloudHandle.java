// CloudHandle.java
package application.bootstrap.weatherpipeline.cloud;

import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class CloudHandle extends HandlePackage {

    /*
     * Persistent cloud archetype record. Wraps CloudData and delegates all
     * access through it. Registered in CloudManager for the engine session;
     * referenced directly by weathers and weather patterns, never cloned.
     * cloudTypeIndex is a small, stable slot assigned by CloudManager at
     * registration — the index this archetype occupies in every
     * fixed-size, per-cloud-type array the weather map UBO carries.
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

    public float getSpreadRatio() {
        return cloudData.getSpreadRatio();
    }

    public float getSizeVarianceMin() {
        return cloudData.getSizeVarianceMin();
    }

    public float getSizeVarianceMax() {
        return cloudData.getSizeVarianceMax();
    }

    public float getElongationMin() {
        return cloudData.getElongationMin();
    }

    public float getElongationMax() {
        return cloudData.getElongationMax();
    }

    // Cloud Type Registry \\

    public void assignCloudTypeIndex(int cloudTypeIndex) {
        this.cloudTypeIndex = cloudTypeIndex;
    }

    public int getCloudTypeIndex() {
        return cloudTypeIndex;
    }
}