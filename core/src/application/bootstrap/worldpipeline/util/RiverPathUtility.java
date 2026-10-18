package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.river.RiverPathStruct;
import application.bootstrap.worldpipeline.river.RiverQueryStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class RiverPathUtility extends EngineUtility {

    /*
     * Stateless geometry of rivers. A span of the world is narrowed once to
     * the run of a reach's segments whose points stand within a margin of it,
     * and a chunk narrows that run again to the segments that can be nearest
     * to any of its columns, so every column measures itself against those
     * alone; a point is located against a run of segments: its distance from
     * the centreline and the channel interpolated at the nearest place on it.
     * Positions are offsets from the reach's origin. A river's channel widens
     * and deepens by a step for every doubling of its flow.
     */

    public static final int SEGMENT_NONE = EngineSetting.INDEX_NOT_FOUND;

    // Span \\

    // The first segment touching a point within the margin of a span of offsets, SEGMENT_NONE when none does
    public static int findFirstSegment(
            RiverPathStruct path,
            double fromX,
            double fromZ,
            double toX,
            double toZ,
            double margin) {

        int count = path.getPointCount();

        for (int i = 0; i < count; i++)
            if (isWithin(path, i, fromX, fromZ, toX, toZ, margin))
                return Math.max(0, Math.min(i - 1, count - 2));

        return SEGMENT_NONE;
    }

    // The last segment touching a point within the margin of a span of offsets, SEGMENT_NONE when none does
    public static int findLastSegment(
            RiverPathStruct path,
            double fromX,
            double fromZ,
            double toX,
            double toZ,
            double margin) {

        int count = path.getPointCount();

        for (int i = count - 1; i >= 0; i--)
            if (isWithin(path, i, fromX, fromZ, toX, toZ, margin))
                return Math.max(0, Math.min(i, count - 2));

        return SEGMENT_NONE;
    }

    private static boolean isWithin(
            RiverPathStruct path,
            int index,
            double fromX,
            double fromZ,
            double toX,
            double toZ,
            double margin) {

        float x = path.getPointX(index);
        float z = path.getPointZ(index);

        return x >= fromX - margin && x <= toX + margin && z >= fromZ - margin && z <= toZ + margin;
    }

    // The first segment of a run lying within a distance of a point, SEGMENT_NONE when none does
    public static int findFirstSegmentWithin(
            RiverPathStruct path,
            double x,
            double z,
            int firstSegment,
            int lastSegment,
            double maxDistance) {

        for (int i = firstSegment; i <= lastSegment; i++)
            if (distanceToSegment(path, i, x, z) <= maxDistance)
                return i;

        return SEGMENT_NONE;
    }

    // The last segment of a run lying within a distance of a point, SEGMENT_NONE when none does
    public static int findLastSegmentWithin(
            RiverPathStruct path,
            double x,
            double z,
            int firstSegment,
            int lastSegment,
            double maxDistance) {

        for (int i = lastSegment; i >= firstSegment; i--)
            if (distanceToSegment(path, i, x, z) <= maxDistance)
                return i;

        return SEGMENT_NONE;
    }

    private static double distanceToSegment(RiverPathStruct path, int segment, double x, double z) {
        return distanceToSegment(
                x, z, path.getPointX(segment), path.getPointZ(segment),
                path.getPointX(segment + 1), path.getPointZ(segment + 1));
    }

    // How far a point stands from a straight segment
    public static double distanceToSegment(double x, double z, double ax, double az, double bx, double bz) {

        double segmentX = bx - ax;
        double segmentZ = bz - az;
        double lengthSq = segmentX * segmentX + segmentZ * segmentZ;
        double t = lengthSq > 0.0
                ? Math.max(0.0, Math.min(1.0, ((x - ax) * segmentX + (z - az) * segmentZ) / lengthSq))
                : 0.0;
        double deltaX = x - (ax + segmentX * t);
        double deltaZ = z - (az + segmentZ * t);

        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    // Channel \\

    public static float resolveHalfWidth(float strength) {
        return Math.max(EngineSetting.RIVER_MIN_HALF_WIDTH_BLOCKS,
                Math.min(EngineSetting.RIVER_MAX_HALF_WIDTH_BLOCKS,
                        EngineSetting.RIVER_MIN_HALF_WIDTH_BLOCKS + EngineSetting.RIVER_HALF_WIDTH_PER_DOUBLING_BLOCKS
                                * Math.max(0f, strength - 1f) / EngineSetting.RIVER_STRENGTH_STEPS_PER_DOUBLING));
    }

    public static float resolveDepth(float strength) {
        return Math.min(EngineSetting.RIVER_MAX_DEPTH_BLOCKS,
                EngineSetting.RIVER_MIN_DEPTH_BLOCKS + EngineSetting.RIVER_DEPTH_PER_DOUBLING_BLOCKS
                        * Math.max(0f, strength - 1f) / EngineSetting.RIVER_STRENGTH_STEPS_PER_DOUBLING);
    }

    // Locate \\

    // Where a point stands against a run of the reach's segments, written out — returns its distance from them
    public static double locate(
            RiverPathStruct path,
            double x,
            double z,
            int firstSegment,
            int lastSegment,
            RiverQueryStruct out) {

        double nearestSq = Double.MAX_VALUE;
        int nearestSegment = firstSegment;
        double nearestT = 0.0;

        for (int i = firstSegment; i <= lastSegment; i++) {

            double ax = path.getPointX(i);
            double az = path.getPointZ(i);
            double segmentX = path.getPointX(i + 1) - ax;
            double segmentZ = path.getPointZ(i + 1) - az;
            double lengthSq = segmentX * segmentX + segmentZ * segmentZ;
            double t = lengthSq > 0.0
                    ? Math.max(0.0, Math.min(1.0, ((x - ax) * segmentX + (z - az) * segmentZ) / lengthSq))
                    : 0.0;
            double deltaX = x - (ax + segmentX * t);
            double deltaZ = z - (az + segmentZ * t);
            double distanceSq = deltaX * deltaX + deltaZ * deltaZ;

            if (distanceSq >= nearestSq)
                continue;

            nearestSq = distanceSq;
            nearestSegment = i;
            nearestT = t;
        }

        int next = nearestSegment + 1;
        int nearer = nearestT < 0.5 ? nearestSegment : next;
        float fraction = (float) nearestT;

        out.set(
                Math.sqrt(nearestSq),
                lerp(path.getPointDistance(nearestSegment), path.getPointDistance(next), fraction),
                path.getPointLevel(nearer),
                lerp(path.getPointHalfWidth(nearestSegment), path.getPointHalfWidth(next), fraction),
                lerp(path.getPointDepth(nearestSegment), path.getPointDepth(next), fraction),
                path.getPointZone(nearer),
                path.getPointBiome(nearer));

        return out.getDistance();
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
