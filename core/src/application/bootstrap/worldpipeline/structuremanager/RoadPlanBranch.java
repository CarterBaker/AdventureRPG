package application.bootstrap.worldpipeline.structuremanager;

import java.util.PriorityQueue;

import application.bootstrap.worldpipeline.structure.RoadData;
import application.bootstrap.worldpipeline.structure.RoadPathStruct;
import application.bootstrap.worldpipeline.structure.RoadSegmentType;
import application.bootstrap.worldpipeline.structure.StructureGeometryUtility;
import application.bootstrap.worldpipeline.structure.StructurePlacementStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSampleStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.NoiseUtility;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RoadPlanBranch extends BranchPackage {

    /*
     * Turns two endpoints into a buildable road. Planning runs in three
     * passes.
     *
     * Route: A* over a coarse grid of terrain samples inside a corridor
     * around the straight line between the endpoints. Steps cost their
     * length, plus a penalty for climbing, a much steeper one for any step
     * that climbs faster than the road's max grade, and extra for open water
     * and for cutting through another structure. On gentle ground that
     * finds roughly the straight line, bent by a low-frequency meander
     * field so it wanders like a road rather than a ruler; on a mountain
     * the steep-grade penalty makes long diagonal traverses and
     * switchbacks cheaper than going straight up, so the road snakes.
     *
     * Shape: the grid route is rounded with Chaikin corner cutting and
     * resampled to one point per block.
     *
     * Height: each point starts at the terrain surface, is smoothed, and is
     * then grade-limited forward and backward between the pinned
     * endpoints. Wherever that leaves the road far above the ground it
     * becomes a bridge, far below it a tunnel — which is how a road that
     * could not find a way around a ridge or a valley goes through or over
     * it instead.
     */

    // Internal
    private WorldGenerationManager worldGenerationManager;

    // Base \\

    @Override
    protected void get() {
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // Road \\

    /*
     * Plans a road from A to B. B is taken as the nearest copy of itself
     * across the world seam, so the path always runs the short way round.
     * avoid holds placements the route should steer clear of — never the
     * two it connects.
     */
    RoadPathStruct planRoad(
            WorldHandle worldHandle,
            RoadData road,
            double ax, double az, int ay,
            double bx, double bz, int by,
            ObjectArrayList<StructurePlacementStruct> avoid) {

        double endX = ax + StructureGeometryUtility.wrapDelta(bx - ax, worldHandle.getWorldScale().x);
        double endZ = az + StructureGeometryUtility.wrapDelta(bz - az, worldHandle.getWorldScale().y);

        if (Math.hypot(endX - ax, endZ - az) < 2.0)
            return null;

        TerrainSampleStruct sample = new TerrainSampleStruct();

        DoubleArrayList routeX = new DoubleArrayList();
        DoubleArrayList routeZ = new DoubleArrayList();

        findRoute(worldHandle, road, ax, az, endX, endZ, avoid, sample, routeX, routeZ);

        for (int i = 0; i < EngineSetting.ROAD_PATH_CHAIKIN_ITERATIONS; i++)
            chaikin(routeX, routeZ);

        DoubleArrayList pointsX = new DoubleArrayList();
        DoubleArrayList pointsZ = new DoubleArrayList();
        resample(routeX, routeZ, pointsX, pointsZ);

        int count = pointsX.size();
        float[] target = new float[count];
        boolean[] pinned = new boolean[count];

        for (int i = 0; i < count; i++)
            target[i] = worldGenerationManager.sampleSurfaceHeightBlocks(
                    worldHandle, (long) Math.floor(pointsX.getDouble(i)), (long) Math.floor(pointsZ.getDouble(i)),
                    sample);

        target[0] = ay;
        target[count - 1] = by;
        pinned[0] = true;
        pinned[count - 1] = true;

        int[] heights = solveHeights(target, pinned, road);

        return buildPath(worldHandle, road, pointsX, pointsZ, heights, sample);
    }

    // Route \\

    private void findRoute(
            WorldHandle worldHandle,
            RoadData road,
            double ax, double az,
            double bx, double bz,
            ObjectArrayList<StructurePlacementStruct> avoid,
            TerrainSampleStruct sample,
            DoubleArrayList outX, DoubleArrayList outZ) {

        int grid = EngineSetting.ROAD_PATH_GRID_BLOCKS;
        double length = Math.hypot(bx - ax, bz - az);
        double corridor = Math.max(EngineSetting.ROAD_PATH_MIN_CORRIDOR_BLOCKS,
                length * EngineSetting.ROAD_PATH_CORRIDOR_FRACTION);

        double originX = Math.round(ax / grid) * (double) grid;
        double originZ = Math.round(az / grid) * (double) grid;

        int goalI = (int) Math.round((bx - originX) / grid);
        int goalJ = (int) Math.round((bz - originZ) / grid);
        long goal = Coordinate2Long.pack(goalI, goalJ);
        long start = Coordinate2Long.pack(0, 0);

        GridContext context = new GridContext(
                worldHandle, road, sample, avoid, originX, originZ, ax, az, bx, bz, corridor, grid);

        Long2FloatOpenHashMap gScore = new Long2FloatOpenHashMap();
        gScore.defaultReturnValue(Float.MAX_VALUE);
        Long2LongOpenHashMap cameFrom = new Long2LongOpenHashMap();
        LongOpenHashSet closed = new LongOpenHashSet();
        PriorityQueue<long[]> open = new PriorityQueue<>((p, q) -> Double.compare(
                Double.longBitsToDouble(p[1]), Double.longBitsToDouble(q[1])));

        gScore.put(start, 0f);
        open.add(new long[] { start, Double.doubleToLongBits(heuristic(0, 0, goalI, goalJ, grid)) });

        boolean found = false;
        int expanded = 0;

        while (!open.isEmpty() && expanded < EngineSetting.ROAD_PATH_MAX_EXPANDED_NODES) {

            long current = open.poll()[0];

            if (!closed.add(current))
                continue;

            if (current == goal) {
                found = true;
                break;
            }

            expanded++;

            int ci = Coordinate2Long.unpackX(current);
            int cj = Coordinate2Long.unpackY(current);
            float currentScore = gScore.get(current);

            for (int step = 0; step < STEP_I.length; step++) {

                int ni = ci + STEP_I[step];
                int nj = cj + STEP_J[step];
                long next = Coordinate2Long.pack(ni, nj);

                if (closed.contains(next) || (next != goal && !context.inCorridor(ni, nj)))
                    continue;

                float score = currentScore + context.stepCost(ci, cj, ni, nj);

                if (score >= gScore.get(next))
                    continue;

                gScore.put(next, score);
                cameFrom.put(next, current);
                open.add(new long[] { next,
                        Double.doubleToLongBits(score + heuristic(ni, nj, goalI, goalJ, grid)) });
            }
        }

        outX.add(ax);
        outZ.add(az);

        if (found) {

            LongArrayList reversed = new LongArrayList();

            for (long node = cameFrom.get(goal); node != start; node = cameFrom.get(node))
                reversed.add(node);

            for (int i = reversed.size() - 1; i >= 0; i--) {
                long node = reversed.getLong(i);
                outX.add(originX + Coordinate2Long.unpackX(node) * (double) grid);
                outZ.add(originZ + Coordinate2Long.unpackY(node) * (double) grid);
            }
        }

        // No route inside the budget falls back to the straight line; the
        // height pass still turns whatever it crosses into bridges and tunnels.
        outX.add(bx);
        outZ.add(bz);
    }

    /*
     * Sixteen headings — the eight neighbours plus the knight moves — so a
     * route across open ground can run at any of sixteen angles instead of
     * only along the grid axes and diagonals.
     */
    private static final int[] STEP_I = { 1, 1, 0, -1, -1, -1, 0, 1, 2, 1, -1, -2, -2, -1, 1, 2 };
    private static final int[] STEP_J = { 0, 1, 1, 1, 0, -1, -1, -1, 1, 2, 2, 1, -1, -2, -2, -1 };

    private static double heuristic(int i, int j, int goalI, int goalJ, int grid) {
        return Math.hypot(goalI - i, goalJ - j) * grid;
    }

    private final class GridContext {

        final WorldHandle worldHandle;
        final RoadData road;
        final TerrainSampleStruct sample;
        final ObjectArrayList<StructurePlacementStruct> avoid;
        final double originX, originZ;
        final double ax, az, bx, bz;
        final double corridor;
        final int grid;
        final long seed;
        final Long2IntOpenHashMap heights = new Long2IntOpenHashMap();

        GridContext(
                WorldHandle worldHandle, RoadData road, TerrainSampleStruct sample,
                ObjectArrayList<StructurePlacementStruct> avoid,
                double originX, double originZ,
                double ax, double az, double bx, double bz,
                double corridor, int grid) {

            this.worldHandle = worldHandle;
            this.road = road;
            this.sample = sample;
            this.avoid = avoid;
            this.originX = originX;
            this.originZ = originZ;
            this.ax = ax;
            this.az = az;
            this.bx = bx;
            this.bz = bz;
            this.corridor = corridor;
            this.grid = grid;
            this.seed = worldHandle.getSeed() ^ EngineSetting.ROAD_MEANDER_SEED;
            this.heights.defaultReturnValue(Integer.MIN_VALUE);
        }

        boolean inCorridor(int i, int j) {
            return StructureGeometryUtility.pointSegmentDistance(
                    worldX(i), worldZ(j), ax, az, bx, bz) <= corridor;
        }

        float stepCost(int ci, int cj, int ni, int nj) {

            double length = grid * Math.hypot(ni - ci, nj - cj);
            int fromHeight = height(ci, cj);
            int toHeight = height(ni, nj);
            double climb = Math.abs(toHeight - fromHeight);
            double grade = climb / length;

            double x = worldX(ni);
            double z = worldZ(nj);

            double meander = EngineSetting.ROAD_PATH_MEANDER_STRENGTH * (NoiseUtility.noise2(seed,
                    x / EngineSetting.ROAD_PATH_MEANDER_WAVELENGTH_BLOCKS,
                    z / EngineSetting.ROAD_PATH_MEANDER_WAVELENGTH_BLOCKS) * 0.5 + 0.5);

            double cost = length * (1.0 + meander) + EngineSetting.ROAD_PATH_GRADE_PENALTY * climb;

            if (grade > road.getMaxGrade())
                cost += EngineSetting.ROAD_PATH_STEEP_PENALTY * (grade - road.getMaxGrade()) * length;

            if (toHeight <= EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS && worldGenerationManager.sampleOceanWater(
                    worldHandle, (long) Math.floor(x), (long) Math.floor(z), sample))
                cost += EngineSetting.ROAD_PATH_WATER_PENALTY * length;

            for (int i = 0; i < avoid.size(); i++) {

                StructurePlacementStruct placement = avoid.get(i);
                double dx = StructureGeometryUtility.wrapDelta(placement.getX() - x, worldHandle.getWorldScale().x);
                double dz = StructureGeometryUtility.wrapDelta(placement.getZ() - z, worldHandle.getWorldScale().y);

                if (dx * dx + dz * dz < (double) placement.getRadiusBlocks() * placement.getRadiusBlocks()) {
                    cost += EngineSetting.ROAD_PATH_STRUCTURE_PENALTY * length;
                    break;
                }
            }

            return (float) cost;
        }

        int height(int i, int j) {

            long key = Coordinate2Long.pack(i, j);
            int height = heights.get(key);

            if (height == Integer.MIN_VALUE) {
                height = worldGenerationManager.sampleSurfaceHeightBlocks(
                        worldHandle, (long) Math.floor(worldX(i)), (long) Math.floor(worldZ(j)), sample);
                heights.put(key, height);
            }

            return height;
        }

        double worldX(int i) {
            return originX + i * (double) grid;
        }

        double worldZ(int j) {
            return originZ + j * (double) grid;
        }
    }

    // Shape \\

    // One round of Chaikin corner cutting; the endpoints never move.
    private static void chaikin(DoubleArrayList xs, DoubleArrayList zs) {

        int count = xs.size();

        if (count < 3)
            return;

        DoubleArrayList newX = new DoubleArrayList(count * 2);
        DoubleArrayList newZ = new DoubleArrayList(count * 2);

        newX.add(xs.getDouble(0));
        newZ.add(zs.getDouble(0));

        for (int i = 0; i < count - 1; i++) {

            double x0 = xs.getDouble(i);
            double z0 = zs.getDouble(i);
            double x1 = xs.getDouble(i + 1);
            double z1 = zs.getDouble(i + 1);

            newX.add(0.75 * x0 + 0.25 * x1);
            newZ.add(0.75 * z0 + 0.25 * z1);
            newX.add(0.25 * x0 + 0.75 * x1);
            newZ.add(0.25 * z0 + 0.75 * z1);
        }

        newX.add(xs.getDouble(count - 1));
        newZ.add(zs.getDouble(count - 1));

        xs.clear();
        zs.clear();
        xs.addAll(newX);
        zs.addAll(newZ);
    }

    // Walks the polyline emitting a point every block, always including both ends.
    static void resample(DoubleArrayList xs, DoubleArrayList zs, DoubleArrayList outX, DoubleArrayList outZ) {

        outX.add(xs.getDouble(0));
        outZ.add(zs.getDouble(0));

        double carried = 0.0;

        for (int i = 0; i < xs.size() - 1; i++) {

            double x0 = xs.getDouble(i);
            double z0 = zs.getDouble(i);
            double dx = xs.getDouble(i + 1) - x0;
            double dz = zs.getDouble(i + 1) - z0;
            double segment = Math.sqrt(dx * dx + dz * dz);

            if (segment <= 0.0)
                continue;

            double t = 1.0 - carried;

            while (t <= segment) {
                outX.add(x0 + dx * (t / segment));
                outZ.add(z0 + dz * (t / segment));
                t += 1.0;
            }

            carried = segment - (t - 1.0);
        }

        double lastX = xs.getDouble(xs.size() - 1);
        double lastZ = zs.getDouble(zs.size() - 1);
        int tail = outX.size() - 1;

        if (Math.hypot(outX.getDouble(tail) - lastX, outZ.getDouble(tail) - lastZ) > 0.25) {
            outX.add(lastX);
            outZ.add(lastZ);
        } else {
            outX.set(tail, lastX);
            outZ.set(tail, lastZ);
        }
    }

    // Height \\

    /*
     * Target heights one block apart, some pinned. Unpinned points are
     * first smoothed with a moving average so the road rolls with the land
     * instead of copying every bump, then clamped to the road's max grade
     * forward from the start and backward from the end. Where the pins sit
     * too far apart vertically for the grade over the distance between them
     * the backward pass wins and the last stretch runs steep — a layout
     * asking for that has chosen too short an entrance.
     */
    int[] solveHeights(float[] target, boolean[] pinned, RoadData road) {

        int count = target.length;
        int radius = road.getSmoothingRadiusBlocks();
        float grade = road.getMaxGrade();

        double[] prefix = new double[count + 1];

        for (int i = 0; i < count; i++)
            prefix[i + 1] = prefix[i] + target[i];

        float[] heights = new float[count];

        for (int i = 0; i < count; i++) {

            if (pinned[i] || radius <= 0) {
                heights[i] = target[i];
                continue;
            }

            int low = Math.max(0, i - radius);
            int high = Math.min(count - 1, i + radius);
            heights[i] = (float) ((prefix[high + 1] - prefix[low]) / (high - low + 1));
        }

        for (int i = 1; i < count; i++)
            if (!pinned[i])
                heights[i] = Math.max(heights[i - 1] - grade, Math.min(heights[i - 1] + grade, heights[i]));

        for (int i = count - 2; i >= 0; i--)
            if (!pinned[i])
                heights[i] = Math.max(heights[i + 1] - grade, Math.min(heights[i + 1] + grade, heights[i]));

        int[] result = new int[count];

        for (int i = 0; i < count; i++)
            result[i] = Math.round(heights[i]);

        return result;
    }

    // Build \\

    /*
     * Classifies every point against the real ground under it and bakes the
     * result. Runs of a bridge or tunnel shorter than MIN_RUN points are
     * folded back into the ground road around them, so a road skimming a
     * small bump or dip gets a short cut or embankment instead of a
     * one-block tunnel portal.
     */
    private static final int MIN_RUN = 4;

    RoadPathStruct buildPath(
            WorldHandle worldHandle,
            RoadData road,
            DoubleArrayList xs, DoubleArrayList zs,
            int[] heights,
            TerrainSampleStruct sample) {

        int count = xs.size();
        RoadSegmentType[] types = new RoadSegmentType[count];

        for (int i = 0; i < count; i++) {

            if (road.isAlwaysTunnel()) {
                types[i] = RoadSegmentType.TUNNEL;
                continue;
            }

            int ground = worldGenerationManager.sampleGroundHeightBlocks(
                    worldHandle, (long) Math.floor(xs.getDouble(i)), (long) Math.floor(zs.getDouble(i)), sample);

            if (ground - heights[i] > road.getTunnelThresholdBlocks())
                types[i] = RoadSegmentType.TUNNEL;
            else if (heights[i] - ground > road.getMaxFillBlocks())
                types[i] = RoadSegmentType.BRIDGE;
            else
                types[i] = RoadSegmentType.GROUND;
        }

        int runStart = 0;

        for (int i = 1; i <= count; i++) {

            if (i < count && types[i] == types[runStart])
                continue;

            if (types[runStart] != RoadSegmentType.GROUND && i - runStart < MIN_RUN && !road.isAlwaysTunnel())
                for (int j = runStart; j < i; j++)
                    types[j] = RoadSegmentType.GROUND;

            runStart = i;
        }

        RoadPathStruct path = new RoadPathStruct(road, count);

        for (int i = 0; i < count; i++)
            path.add((float) xs.getDouble(i), (float) zs.getDouble(i), heights[i], types[i]);

        path.finish(
                worldHandle.getWorldScale().x / EngineSetting.CHUNK_SIZE,
                worldHandle.getWorldScale().y / EngineSetting.CHUNK_SIZE);

        return path;
    }
}
