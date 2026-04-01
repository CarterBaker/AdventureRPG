package program.bootstrap.lightingpipeline.naturallightmanager;

import program.core.engine.SystemPackage;
import program.core.settings.EngineSetting;
import program.core.util.mathematics.vectors.Vector3;

public class SunLightSystem extends SystemPackage {

    /*
     * Computes sun direction, color, and intensity each frame from the current
     * visual time of day. Direction traces a full circle — intensity peaks at
     * noon and falls to zero at the horizon cutoff.
     */

    // Output
    private final Vector3 direction = new Vector3();
    private final Vector3 color = new Vector3(1f, 1f, 1f);
    private float intensity;

    // Settings
    private float SUN_HORIZON_CUTOFF;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.SUN_HORIZON_CUTOFF = EngineSetting.SUN_HORIZON_CUTOFF;
    }

    // Update \\

    public void update(float visualTimeOfDay) {

        float angle = visualTimeOfDay * (float) Math.PI * 2f;
        float dirX = -(float) Math.sin(angle);
        float dirY = -(float) Math.cos(angle);
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);

        if (len > 0f) {
            dirX /= len;
            dirY /= len;
        }

        direction.set(dirX, dirY, 0f);
        color.set(1f, 1f, 1f);
        intensity = computeIntensity(visualTimeOfDay);
    }

    private float computeIntensity(float t) {

        float distFromNoon = Math.abs(t - 0.5f) * 2f;

        if (distFromNoon > SUN_HORIZON_CUTOFF)
            return 0f;

        float blend = 1f - (distFromNoon / SUN_HORIZON_CUTOFF);

        return blend * blend;
    }

    // Accessible \\

    public Vector3 getDirection() {
        return direction;
    }

    public Vector3 getColor() {
        return color;
    }

    public float getIntensity() {
        return intensity;
    }
}