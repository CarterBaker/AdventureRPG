package application.bootstrap.weatherpipeline.wind;

import engine.root.InstancePackage;
import engine.util.mathematics.vectors.Vector3;

public class WindInstance extends InstancePackage {

    /*
     * One grid's own local wind state — direction and speed. Owned directly
     * by GridInstance so every window's location tracks its own wind
     * independently.
     */

    private final Vector3 localWindDirection = new Vector3();
    private float localWindSpeed;

    // Constructor \\

    public void constructor() {
        this.localWindDirection.set(0f, 0f, 0f);
        this.localWindSpeed = 0f;
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
}
