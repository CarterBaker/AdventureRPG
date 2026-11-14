package application.bootstrap.weatherpipeline.wind;

import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class WindHandle extends HandlePackage {

    /*
     * Runtime handle wrapping the session-wide global wind. Owned by
     * WindManager and passed to GlobalWindBranch/LocalWindBranch.
     */

    private WindData windData;

    public void constructor(WindData windData) {
        this.windData = windData;
    }

    public WindData getWindData() {
        return windData;
    }

    public Vector3 getGlobalWindDirection() {
        return windData.getGlobalWindDirection();
    }

    public void setGlobalWindDirection(float x, float y, float z) {
        windData.setGlobalWindDirection(x, y, z);
    }

    public float getGlobalWindSpeed() {
        return windData.getGlobalWindSpeed();
    }

    public void setGlobalWindSpeed(float globalWindSpeed) {
        windData.setGlobalWindSpeed(globalWindSpeed);
    }
}