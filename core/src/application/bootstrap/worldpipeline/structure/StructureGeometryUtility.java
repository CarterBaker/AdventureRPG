package application.bootstrap.worldpipeline.structure;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class StructureGeometryUtility extends EngineUtility {

    /*
     * Pure math shared by structure placement, layout, road planning and
     * stamping: wrapped distances on the seamless world, deterministic
     * hashing so every decision is a function of (seed, identity), and the
     * 2D segment distances layouts use to keep streets apart.
     */

    private StructureGeometryUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // Wrapping \\

    // Shortest signed offset equivalent to delta on a ring of the given size.
    public static double wrapDelta(double delta, double size) {

        double wrapped = delta % size;

        if (wrapped > size * 0.5)
            wrapped -= size;
        else if (wrapped < -size * 0.5)
            wrapped += size;

        return wrapped;
    }

    public static long wrapDelta(long delta, long size) {

        long wrapped = Math.floorMod(delta, size);

        if (wrapped > size / 2)
            wrapped -= size;

        return wrapped;
    }

    // Hashing \\

    public static long mix(long value) {

        long hash = value;

        hash ^= (hash >>> 33);
        hash *= EngineSetting.HASH_FINALIZER_MULTIPLIER_1;
        hash ^= (hash >>> 33);
        hash *= EngineSetting.HASH_FINALIZER_MULTIPLIER_2;
        hash ^= (hash >>> 33);

        return hash;
    }

    public static long hash(long seed, long a, long b, long c) {
        return mix(mix(mix(seed ^ a) ^ b) ^ c);
    }

    public static float hash01(long value) {
        return (float) ((mix(value) >>> 11) / (double) (1L << 53));
    }

    // Uniform integer in [min, max] from a hash.
    public static int hashRange(long value, int min, int max) {

        if (max <= min)
            return min;

        return min + (int) Math.min(max - min, (long) (hash01(value) * (max - min + 1)));
    }

    // Segments \\

    public static double pointSegmentDistance(
            double px, double pz,
            double ax, double az,
            double bx, double bz) {

        double dx = bx - ax;
        double dz = bz - az;
        double lengthSq = dx * dx + dz * dz;

        double t = lengthSq <= 0.0 ? 0.0 : ((px - ax) * dx + (pz - az) * dz) / lengthSq;
        t = Math.max(0.0, Math.min(1.0, t));

        double cx = ax + dx * t - px;
        double cz = az + dz * t - pz;

        return Math.sqrt(cx * cx + cz * cz);
    }

    public static double segmentSegmentDistance(
            double ax, double az, double bx, double bz,
            double cx, double cz, double dx, double dz) {

        if (segmentsIntersect(ax, az, bx, bz, cx, cz, dx, dz))
            return 0.0;

        return Math.min(
                Math.min(pointSegmentDistance(ax, az, cx, cz, dx, dz), pointSegmentDistance(bx, bz, cx, cz, dx, dz)),
                Math.min(pointSegmentDistance(cx, cz, ax, az, bx, bz), pointSegmentDistance(dx, dz, ax, az, bx, bz)));
    }

    private static boolean segmentsIntersect(
            double ax, double az, double bx, double bz,
            double cx, double cz, double dx, double dz) {

        double d1 = cross(dx - cx, dz - cz, ax - cx, az - cz);
        double d2 = cross(dx - cx, dz - cz, bx - cx, bz - cz);
        double d3 = cross(bx - ax, bz - az, cx - ax, cz - az);
        double d4 = cross(bx - ax, bz - az, dx - ax, dz - az);

        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
                && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    private static double cross(double ax, double az, double bx, double bz) {
        return ax * bz - az * bx;
    }
}
