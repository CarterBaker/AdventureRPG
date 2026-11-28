package application.bootstrap.calendarpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class CelestialUtility extends EngineUtility {

    /*
     * The geometry of the sky, shared by everything that needs to know where
     * something in it stands. The sun and the moon ride one great circle that
     * rises in the east, crosses the zenith and sets in the west; a body's
     * place on it is a fraction of the day, and the moon can lift above or
     * sink below it by its orbital latitude. The stars sit on a sphere that
     * turns once per sidereal day about the celestial pole, which stands due
     * north at an elevation equal to the observer's latitude, so the pole star
     * gives north and latitude while the turning sky gives the hour and the
     * season. All of it is worked in the sky's own frame, up +Y, north +Z and
     * east -X; a grid whose +Z is not true north turns it by its north
     * heading.
     */

    private static final double TWO_PI = Math.PI * 2.0;

    // Orbit \\

    // The vertical component of the sun's direction at a visual time of day, -1 at nadir and 1 at zenith
    public static double resolveSolarElevation(double visualTimeOfDay) {
        return -Math.cos(visualTimeOfDay * TWO_PI);
    }

    // Direction toward the sun at a visual time of day — the sun rides its own path
    public static void resolveSunDirection(double visualTimeOfDay, Vector3 out) {
        resolveOrbitDirection(visualTimeOfDay, 0.0, out);
    }

    // Direction toward a body on the sun's path at a fraction of the day, 0 at nadir and 0.5 at zenith, lifted off
    // the path toward the north by latitude radians
    public static void resolveOrbitDirection(double orbitTime, double latitude, Vector3 out) {

        double angle = orbitTime * TWO_PI;
        double pathScale = Math.cos(latitude);

        out.set(
                (float) (-Math.sin(angle) * pathScale),
                (float) (-Math.cos(angle) * pathScale),
                (float) Math.sin(latitude));
    }

    // North Heading \\

    // Turns a direction from the sky's own frame into a grid's, whose true north lies northHeading radians from its
    // +Z toward its +X
    public static void applyNorthHeading(double northHeading, Vector3 direction) {

        if (northHeading == 0.0)
            return;

        float cosHeading = (float) Math.cos(northHeading);
        float sinHeading = (float) Math.sin(northHeading);
        float x = direction.x;
        float z = direction.z;

        direction.x = x * cosHeading + z * sinHeading;
        direction.z = z * cosHeading - x * sinHeading;
    }

    // Star Sphere \\

    // Sidereal time as a fraction of a turn: one turn per day plus one more per year, so the stars rise a little
    // earlier each night and the night sky walks through the year
    public static double resolveSiderealTime(double visualTimeOfDay, double visualYearProgress) {
        return wrapFraction(visualTimeOfDay + visualYearProgress);
    }

    // The observer's latitude in radians from a grid's latitude factor in [-1, 1]
    public static double resolveObserverLatitude(double latitudeFactor) {
        return latitudeFactor * Math.toRadians(EngineSetting.CELESTIAL_POLE_MAX_ELEVATION_DEGREES);
    }

    // Rotation from a grid's space into the star sphere's own fixed frame, whose +Z is the north celestial pole —
    // the grid first turned back by its north heading into the sky's own frame
    public static void resolveStarRotation(double siderealTime, double latitude, double northHeading, Matrix4 out) {

        double turn = siderealTime * TWO_PI;
        float cosTurn = (float) Math.cos(turn);
        float sinTurn = (float) Math.sin(turn);
        float cosLatitude = (float) Math.cos(latitude);
        float sinLatitude = (float) Math.sin(latitude);
        float cosHeading = (float) Math.cos(northHeading);
        float sinHeading = (float) Math.sin(northHeading);

        float m00 = cosTurn;
        float m10 = sinTurn;
        float m20 = 0f;
        float m02 = sinTurn * sinLatitude;
        float m12 = -cosTurn * sinLatitude;
        float m22 = cosLatitude;

        out.set(
                m00 * cosHeading + m02 * sinHeading, -sinTurn * cosLatitude, m02 * cosHeading - m00 * sinHeading, 0f,
                m10 * cosHeading + m12 * sinHeading, cosTurn * cosLatitude, m12 * cosHeading - m10 * sinHeading, 0f,
                m20 * cosHeading + m22 * sinHeading, sinLatitude, m22 * cosHeading - m20 * sinHeading, 0f,
                0f, 0f, 0f, 1f);
    }

    // Utility \\

    public static double wrapFraction(double value) {

        double wrapped = value % 1.0;

        if (wrapped < 0)
            wrapped += 1.0;

        return wrapped;
    }
}
