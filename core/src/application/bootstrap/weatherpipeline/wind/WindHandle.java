package application.bootstrap.weatherpipeline.wind;

import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class WindHandle extends HandlePackage {

    /*
     * Runtime handle wrapping WindData. Owned by WindManager and passed to
     * GlobalWindBranch and LocalWindBranch for reading and writing wind
     * state each frame. Delegates all accessors and mutators through
     * WindData.
     */

    // Internal
    private WindData windData;

    // Constructor \\

    public void constructor(WindData windData) {

        // Internal
        this.windData = windData;
    }

    // Accessible \\

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

    public Vector3 getLocalWindDirection() {
        return windData.getLocalWindDirection();
    }

    public void setLocalWindDirection(float x, float y, float z) {
        windData.setLocalWindDirection(x, y, z);
    }

    public float getLocalWindSpeed() {
        return windData.getLocalWindSpeed();
    }

    public void setLocalWindSpeed(float localWindSpeed) {
        windData.setLocalWindSpeed(localWindSpeed);
    }
}