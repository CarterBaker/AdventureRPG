package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.road.RoadQueryStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

public final class RoadPathUtility extends EngineUtility {

    /*
     * Pure geometry of road centrelines, shared by every road a layout lays:
     * streets, rings, walls, trails and the roads between settlements. A
     * curve between two points bows sideways along a few seeded sine waves
     * that vanish at both ends, so it starts and arrives where it was asked
     * yet wanders smoothly between, and a ring wobbles about its centre the
     * same way. Points fall a fixed spacing apart, so a road reads round from
     * any distance. locate() finds where a point stands against a planned
     * road and sampleAlong() where the road is a distance along it. Every
     * result is a pure function of its inputs and seed.
     */

    // Settings
    private static final double SPACING = EngineSetting.ROAD_POINT_SPACING_BLOCKS;
    private static final double TAU = Math.PI * 2.0;

    // Curves \\

    // A seeded curve from one point to another, bowing sideways by up to bend times its length
    public static void buildCurve(
            double startX,
            double startZ,
            double endX,
            double endZ,
            float bend,
            long seed,
            DoubleArrayList outX,
            DoubleArrayList outZ) {

        outX.clear();
        outZ.clear();

        double deltaX = endX - startX;
        double deltaZ = endZ - startZ;
        double length = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (length < EngineSetting.DIVISION_EPSILON) {
            outX.add(startX);
            outZ.add(startZ);
            return;
        }

        double normalX = -deltaZ / length;
        double normalZ = deltaX / length;
        double first = signedRoll(seed, 1);
        double second = signedRoll(seed, 2) * EngineSetting.ROAD_CURVE_HARMONIC_FALLOFF;
        double third = signedRoll(seed, 3) * EngineSetting.ROAD_CURVE_HARMONIC_FALLOFF
                * EngineSetting.ROAD_CURVE_HARMONIC_FALLOFF;
        double amplitude = length * bend * EngineSetting.ROAD_CURVE_BEND_SCALE;
        int count = Math.max(1, (int) Math.ceil(length * (1.0 + bend) / SPACING));

        for (int i = 0; i <= count; i++) {

            double t = (double) i / count;
            double offset = amplitude * (first * Math.sin(Math.PI * t)
                    + second * Math.sin(2.0 * Math.PI * t)
                    + third * Math.sin(3.0 * Math.PI * t));

            outX.add(startX + deltaX * t + normalX * offset);
            outZ.add(startZ + deltaZ * t + normalZ * offset);
        }
    }

    // A seeded closed loop about a centre, its radius wandering by up to wobble of itself, ending where it began
    public static void buildRing(
            double centerX,
            double centerZ,
            double radius,
            float wobble,
            long seed,
            DoubleArrayList outX,
            DoubleArrayList outZ) {

        outX.clear();
        outZ.clear();

        double first = signedRoll(seed, 1);
        double second = signedRoll(seed, 2) * EngineSetting.ROAD_CURVE_HARMONIC_FALLOFF;
        double firstPhase = (signedRoll(seed, 3) + 1.0) * Math.PI;
        double secondPhase = (signedRoll(seed, 4) + 1.0) * Math.PI;
        int count = Math.max(EngineSetting.ROAD_RING_MIN_POINTS, (int) Math.ceil(TAU * radius / SPACING));

        for (int i = 0; i <= count; i++) {

            double angle = TAU * (i % count) / count;
            double ringRadius = radius * (1.0 + wobble * (first * Math.sin(2.0 * angle + firstPhase)
                    + second * Math.sin(3.0 * angle + secondPhase)));

            outX.add(centerX + Math.cos(angle) * ringRadius);
            outZ.add(centerZ + Math.sin(angle) * ringRadius);
        }
    }

    private static double signedRoll(long seed, int index) {
        return BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(seed, index, EngineSetting.ROAD_CURVE_ROLL_ROW))
                * 2.0 - 1.0;
    }

    // Query \\

    // True when a point lies within a margin of the road's bounds
    public static boolean reaches(RoadPathStruct path, double x, double z, double margin) {
        return x >= path.getMinX() - margin && x <= path.getMaxX() + margin
                && z >= path.getMinZ() - margin && z <= path.getMaxZ() + margin;
    }

    // Where a point stands against the road's centreline, written out — returns its distance from it
    public static double locate(RoadPathStruct path, double x, double z, RoadQueryStruct out) {

        int count = path.getPointCount();

        if (count == 1) {

            double deltaX = x - path.getPointX(0);
            double deltaZ = z - path.getPointZ(0);
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

            out.set(distance, path.getPointX(0), path.getPointZ(0), 0f, path.getPointY(0), path.getPointSpan(0),
                    0.0, 0.0);
            return distance;
        }

        double nearestSq = Double.MAX_VALUE;
        int nearestSegment = 0;
        double nearestT = 0.0;

        for (int i = 0; i < count - 1; i++) {

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

        writeSegment(path, nearestSegment, nearestT, Math.sqrt(nearestSq), out);

        return out.getDistance();
    }

    // Where the road is a distance along it, clamped to its ends, written out with a distance of zero
    public static void sampleAlong(RoadPathStruct path, float along, RoadQueryStruct out) {

        int count = path.getPointCount();

        if (count == 1) {
            out.set(0.0, path.getPointX(0), path.getPointZ(0), 0f, path.getPointY(0), path.getPointSpan(0),
                    0.0, 0.0);
            return;
        }

        int segment = 0;

        while (segment < count - 2 && path.getPointDistance(segment + 1) < along)
            segment++;

        float segmentLength = path.getPointDistance(segment + 1) - path.getPointDistance(segment);
        double t = segmentLength > 0f
                ? Math.max(0.0, Math.min(1.0, (along - path.getPointDistance(segment)) / segmentLength))
                : 0.0;

        writeSegment(path, segment, t, 0.0, out);
    }

    private static void writeSegment(RoadPathStruct path, int segment, double t, double distance, RoadQueryStruct out) {

        int next = segment + 1;
        double headingX = path.getPointX(next) - path.getPointX(segment);
        double headingZ = path.getPointZ(next) - path.getPointZ(segment);
        double headingLength = Math.max(EngineSetting.DIVISION_EPSILON,
                Math.sqrt(headingX * headingX + headingZ * headingZ));
        float fraction = (float) t;

        out.set(
                distance,
                path.getPointX(segment) + headingX * t,
                path.getPointZ(segment) + headingZ * t,
                path.getPointDistance(segment) + (path.getPointDistance(next) - path.getPointDistance(segment))
                        * fraction,
                path.getPointY(segment) + (path.getPointY(next) - path.getPointY(segment)) * fraction,
                fraction < 0.5f ? path.getPointSpan(segment) : path.getPointSpan(next),
                headingX / headingLength,
                headingZ / headingLength);
    }

    // Headings \\

    // A heading turned by an angle about the vertical
    public static double turnHeadingX(double headingX, double headingZ, double angle) {
        return headingX * Math.cos(angle) - headingZ * Math.sin(angle);
    }

    public static double turnHeadingZ(double headingX, double headingZ, double angle) {
        return headingX * Math.sin(angle) + headingZ * Math.cos(angle);
    }
}
