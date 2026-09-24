package application.bootstrap.weatherpipeline.season;

import application.bootstrap.weatherpipeline.util.SkyColorUtility;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class SkyPhaseStruct extends StructPackage {

    /*
     * The sky's full color set at one phase of the day — night, twilight,
     * golden hour, or day: zenith and horizon for the dome gradient, the
     * sun-side glow and the anti-solar belt that sit on top of it, and the
     * cloud albedo tint, sunlit tint, and shaded tint that let clouds carry
     * the same palette. Loaded per season from JSON; the sky system also
     * keeps its own instances as working buffers and fills them with blend().
     */

    // Dome
    private final Vector3 zenith;
    private final Vector3 horizon;

    // Twilight
    private final Vector3 glow;
    private final Vector3 belt;

    // Cloud
    private final Vector3 cloud;
    private final Vector3 cloudLight;
    private final Vector3 cloudShadow;

    // Constructor \\

    public SkyPhaseStruct(
            Vector3 zenith,
            Vector3 horizon,
            Vector3 glow,
            Vector3 belt,
            Vector3 cloud,
            Vector3 cloudLight,
            Vector3 cloudShadow) {

        // Dome
        this.zenith = zenith;
        this.horizon = horizon;

        // Twilight
        this.glow = glow;
        this.belt = belt;

        // Cloud
        this.cloud = cloud;
        this.cloudLight = cloudLight;
        this.cloudShadow = cloudShadow;
    }

    // Blend \\

    public void blend(SkyPhaseStruct from, SkyPhaseStruct to, float t) {

        SkyColorUtility.lerp(zenith, from.zenith, to.zenith, t);
        SkyColorUtility.lerp(horizon, from.horizon, to.horizon, t);
        SkyColorUtility.lerp(glow, from.glow, to.glow, t);
        SkyColorUtility.lerp(belt, from.belt, to.belt, t);
        SkyColorUtility.lerp(cloud, from.cloud, to.cloud, t);
        SkyColorUtility.lerp(cloudLight, from.cloudLight, to.cloudLight, t);
        SkyColorUtility.lerp(cloudShadow, from.cloudShadow, to.cloudShadow, t);
    }

    // Accessible \\

    public Vector3 getZenith() {
        return zenith;
    }

    public Vector3 getHorizon() {
        return horizon;
    }

    public Vector3 getGlow() {
        return glow;
    }

    public Vector3 getBelt() {
        return belt;
    }

    public Vector3 getCloud() {
        return cloud;
    }

    public Vector3 getCloudLight() {
        return cloudLight;
    }

    public Vector3 getCloudShadow() {
        return cloudShadow;
    }
}
