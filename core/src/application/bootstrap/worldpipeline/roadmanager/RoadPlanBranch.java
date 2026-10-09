package application.bootstrap.worldpipeline.roadmanager;

import application.bootstrap.worldpipeline.road.RoadHandle;
import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.road.RoadSpanType;
import application.bootstrap.worldpipeline.util.TerrainShapeUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

class RoadPlanBranch extends BranchPackage {

    /*
     * Async — settles a road's centreline against the land once, on whatever
     * thread first needs it. The ground is read at every point, the road
     * aims for it, or a little over the water where water stands, its aim is
     * averaged along the road so it rolls rather than bumps, and its climb is
     * held to the road's grade forward and back, either end pinned where it
     * must meet another road. Wherever it then runs well above the land or
     * over water it takes to a bridge, and well below it to a tunnel, if the
     * road has them; a run too short to be worth one is laid on the ground.
     */

    // Internal
    private WorldGenerationManager worldGenerationManager;
    private RoadPlanAsyncContainer planContainer;

    // Base \\

    @Override
    protected void create() {
        this.planContainer = create(RoadPlanAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // Plan \\

    RoadPathStruct plan(
            WorldHandle worldHandle,
            RoadHandle roadHandle,
            DoubleArrayList pointX,
            DoubleArrayList pointZ,
            float startY,
            float endY) {

        RoadPlanAsyncContainer scratch = planContainer.getInstance();

        try {

            int count = pointX.size();
            float[] distances = resolveDistances(pointX, pointZ);

            sampleLand(scratch, worldHandle, pointX, pointZ);
            smoothTarget(scratch, count);

            float[] heights = scratch.smoothed.toFloatArray();

            holdGrade(heights, distances, roadHandle.getMaxGrade(), startY, endY);

            return new RoadPathStruct(
                    roadHandle,
                    pointX.toDoubleArray(),
                    pointZ.toDoubleArray(),
                    heights,
                    classifySpans(scratch, roadHandle, heights),
                    distances,
                    min(pointX) - resolveReach(roadHandle),
                    max(pointX) + resolveReach(roadHandle),
                    min(pointZ) - resolveReach(roadHandle),
                    max(pointZ) + resolveReach(roadHandle));
        } finally {
            scratch.reset();
        }
    }

    // Land \\

    private void sampleLand(
            RoadPlanAsyncContainer scratch,
            WorldHandle worldHandle,
            DoubleArrayList pointX,
            DoubleArrayList pointZ) {

        TerrainSurfaceSampleStruct sample = scratch.sample;

        for (int i = 0; i < pointX.size(); i++) {

            worldGenerationManager.sampleSurface(
                    worldHandle,
                    WorldWrapUtility.wrapBlockX(worldHandle, pointX.getDouble(i)),
                    WorldWrapUtility.wrapBlockZ(worldHandle, pointZ.getDouble(i)),
                    sample);

            float ground = TerrainShapeUtility.finalizeGroundHeightBlocks(sample.getGroundHeightBlocks());
            boolean wet = sample.isOpenWater() || sample.isLakeWater();

            scratch.ground.add(ground);
            scratch.wet.add(wet);
            scratch.target.add(wet
                    ? Math.max(ground, sample.getWaterSurfaceBlocks() + EngineSetting.ROAD_WATER_CLEARANCE_BLOCKS)
                    : ground);
        }
    }

    // The aimed heights averaged over a few points either side, so the road rolls with the land
    private void smoothTarget(RoadPlanAsyncContainer scratch, int count) {

        int radius = EngineSetting.ROAD_SMOOTHING_RADIUS_POINTS;

        for (int i = 0; i < count; i++) {

            int first = Math.max(0, i - radius);
            int last = Math.min(count - 1, i + radius);
            float sum = 0f;

            for (int j = first; j <= last; j++)
                sum += scratch.target.getFloat(j);

            scratch.smoothed.add(sum / (last - first + 1));
        }
    }

    // Grade \\

    // The climb between neighbouring points held to the grade, forward and back, the pinned ends kept
    private void holdGrade(float[] heights, float[] distances, float maxGrade, float startY, float endY) {

        int last = heights.length - 1;
        boolean pinnedStart = !Float.isNaN(startY);
        boolean pinnedEnd = !Float.isNaN(endY);

        if (pinnedStart)
            heights[0] = startY;

        for (int i = 1; i <= last; i++)
            heights[i] = clampStep(heights[i], heights[i - 1], maxGrade * (distances[i] - distances[i - 1]));

        if (pinnedEnd)
            heights[last] = endY;

        for (int i = last - 1; i >= 0; i--)
            heights[i] = clampStep(heights[i], heights[i + 1], maxGrade * (distances[i + 1] - distances[i]));

        if (pinnedStart)
            heights[0] = startY;
    }

    private float clampStep(float height, float neighbour, float step) {
        return Math.max(neighbour - step, Math.min(neighbour + step, height));
    }

    // Spans \\

    private RoadSpanType[] classifySpans(RoadPlanAsyncContainer scratch, RoadHandle roadHandle, float[] heights) {

        RoadSpanType[] spans = new RoadSpanType[heights.length];

        for (int i = 0; i < heights.length; i++) {

            float rise = heights[i] - scratch.ground.getFloat(i);

            if (roadHandle.hasBridge()
                    && (scratch.wet.getBoolean(i) || rise >= roadHandle.getBridge().getMinHeightBlocks()))
                spans[i] = RoadSpanType.BRIDGE;
            else if (roadHandle.hasTunnel() && -rise >= roadHandle.getTunnel().getMinDepthBlocks())
                spans[i] = RoadSpanType.TUNNEL;
            else
                spans[i] = RoadSpanType.GROUND;
        }

        settleShortSpans(scratch, spans);

        return spans;
    }

    // A dry bridge or tunnel shorter than a few points is laid on the ground instead
    private void settleShortSpans(RoadPlanAsyncContainer scratch, RoadSpanType[] spans) {

        int start = 0;

        while (start < spans.length) {

            int end = start;

            while (end + 1 < spans.length && spans[end + 1] == spans[start])
                end++;

            if (spans[start] != RoadSpanType.GROUND
                    && end - start + 1 < EngineSetting.ROAD_MIN_SPAN_POINTS
                    && !anyWet(scratch, start, end))
                for (int i = start; i <= end; i++)
                    spans[i] = RoadSpanType.GROUND;

            start = end + 1;
        }
    }

    private boolean anyWet(RoadPlanAsyncContainer scratch, int start, int end) {

        for (int i = start; i <= end; i++)
            if (scratch.wet.getBoolean(i))
                return true;

        return false;
    }

    // Utility \\

    private float[] resolveDistances(DoubleArrayList pointX, DoubleArrayList pointZ) {

        float[] distances = new float[pointX.size()];

        for (int i = 1; i < distances.length; i++) {

            double deltaX = pointX.getDouble(i) - pointX.getDouble(i - 1);
            double deltaZ = pointZ.getDouble(i) - pointZ.getDouble(i - 1);

            distances[i] = distances[i - 1] + (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        }

        return distances;
    }

    private double resolveReach(RoadHandle roadHandle) {
        return roadHandle.getHalfWidthBlocks() + roadHandle.getShoulderWidthBlocks() + 1.0;
    }

    private double min(DoubleArrayList values) {

        double result = Double.MAX_VALUE;

        for (int i = 0; i < values.size(); i++)
            result = Math.min(result, values.getDouble(i));

        return result;
    }

    private double max(DoubleArrayList values) {

        double result = -Double.MAX_VALUE;

        for (int i = 0; i < values.size(); i++)
            result = Math.max(result, values.getDouble(i));

        return result;
    }
}
