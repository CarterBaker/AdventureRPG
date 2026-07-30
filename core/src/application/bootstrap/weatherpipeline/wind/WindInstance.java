package application.bootstrap.weatherpipeline.wind;

import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.vectors.Vector3;

public class WindInstance extends InstancePackage {

    /*
     * One grid's own local wind state — direction, speed, and the
     * continuously accumulated sky-dome drift offset that direction/speed
     * integrate into every frame. Owned directly by GridInstance so every
     * window's location tracks its own wind and drift independently.
     */

    private final Vector3 localWindDirection = new Vector3();
    private float localWindSpeed;

    private double skyDriftX;
    private double skyDriftZ;

    // Constructor \\

    public void constructor() {
        this.localWindDirection.set(0f, 0f, 0f);
        this.localWindSpeed = 0f;
        this.skyDriftX = 0.0;
        this.skyDriftZ = 0.0;
    }

    // Local Wind \\

    public void setLocalWindDirection(float x, float y, float z) {
        localWindDirection.set(x, y, z);
    }

    public Vector3 getLocalWindDirection() {
        return localWindDirection;
    }

    public void setLocalWindSpeed(float localWindSpeed) {
        this.localWindSpeed = localWindSpeed;
    }

    public float getLocalWindSpeed() {
        return localWindSpeed;
    }

    // Sky Drift \\

    public void advanceSkyDrift(float deltaTime) {

        skyDriftX += localWindDirection.x * localWindSpeed * EngineSetting.SKY_WIND_DRIFT_SCALE * deltaTime;
        skyDriftZ += localWindDirection.z * localWindSpeed * EngineSetting.SKY_WIND_DRIFT_SCALE * deltaTime;

        skyDriftX %= EngineSetting.SKY_WIND_DRIFT_WRAP;
        skyDriftZ %= EngineSetting.SKY_WIND_DRIFT_WRAP;
    }

    public double getSkyDriftX() {
        return skyDriftX;
    }

    public double getSkyDriftZ() {
        return skyDriftZ;
    }
}