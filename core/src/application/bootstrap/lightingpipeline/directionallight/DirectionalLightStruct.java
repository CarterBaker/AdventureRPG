package application.bootstrap.lightingpipeline.directionallight;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class DirectionalLightStruct extends StructPackage {

    /*
     * Pure data holder for one directional light's direction, color, and
     * intensity. Used identically for the sun and the moon — each owns its
     * own instance, computed each frame by its respective light system and
     * pushed to its own UBO by NaturalLightManager.
     */

    // Light Properties
    private final Vector3 direction;
    private final Vector3 color;
    private float intensity;

    // Constructor \\

    public DirectionalLightStruct() {
        this.direction = new Vector3();
        this.color = new Vector3();
    }

    // Accessible \\

    public Vector3 getDirection() {
        return direction;
    }

    public void setDirection(float x, float y, float z) {
        direction.set(x, y, z);
    }

    public Vector3 getColor() {
        return color;
    }

    public void setColor(float r, float g, float b) {
        color.set(r, g, b);
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }
}