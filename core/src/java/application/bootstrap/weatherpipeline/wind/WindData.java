package application.bootstrap.weatherpipeline.wind;

import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3;

public class WindData extends DataPackage {

    /*
     * Immutable-after-assignment global wind state — the planet's fixed
     * prevailing airflow, resolved once by GlobalWindBranch. Every grid's
     * own local wind lives on its own WindInstance instead.
     */

    private final Vector3 globalWindDirection;
    private float globalWindSpeed;

    public WindData() {
        this.globalWindDirection = new Vector3();
    }

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
}