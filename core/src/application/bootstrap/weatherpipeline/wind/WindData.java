package application.bootstrap.weatherpipeline.wind;

import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3;

public class WindData extends DataPackage {

    /*
     * Mutable runtime wind state. Holds the planet's fixed global airflow
     * plus the current locally-blended wind, updated each frame by
     * LocalWindBranch. Global fields are written once at assignment and
     * never change again; local fields are recomputed every frame.
     */

    // Global
    private final Vector3 globalWindDirection;
    private float globalWindSpeed;

    // Local
    private final Vector3 localWindDirection;
    private float localWindSpeed;

    // Constructor \\

    public WindData() {

        // Global
        this.globalWindDirection = new Vector3();

        // Local
        this.localWindDirection = new Vector3();
    }

    // Accessible \\

    public Vector3 getGlobalWindDirection() {
        return globalWindDirection;
    }

    public void setGlobalWindDirection(float x, float y, float z) {
        globalWindDirection.set(x, y, z);
    }

    public float getGlobalWindSpeed() {
        return globalWindSpeed;
    }

    public void setGlobalWindSpeed(float globalWindSpeed) {
        this.globalWindSpeed = globalWindSpeed;
    }

    public Vector3 getLocalWindDirection() {
        return localWindDirection;
    }

    public void setLocalWindDirection(float x, float y, float z) {
        localWindDirection.set(x, y, z);
    }

    public float getLocalWindSpeed() {
        return localWindSpeed;
    }

    public void setLocalWindSpeed(float localWindSpeed) {
        this.localWindSpeed = localWindSpeed;
    }
}