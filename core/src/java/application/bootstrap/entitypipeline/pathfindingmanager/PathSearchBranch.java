package application.bootstrap.entitypipeline.pathfindingmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.pathfinding.PathStruct;
import application.bootstrap.physicspipeline.util.SubBlockSampleUtility;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate3Long;
import engine.util.mathematics.extras.Direction2Vector;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

class PathSearchBranch extends BranchPackage {

    /*
     * A* over PathTerrainBranch's columns for PathfindingManager. A node is a
     * column and the layer a body stands on there; from each it steps into
     * the eight columns around it, onto whatever floor it can climb to or drop
     * onto, and cuts a corner only on level ground with both sides of the
     * corner open to it. A step costs its length, more for every layer it
     * climbs and a little for every layer it drops, and the search leans toward
     * its goal by PATH_HEURISTIC_WEIGHT, trading a slightly longer way for far
     * fewer nodes. It opens at most PATH_NODE_LIMIT nodes within
     * PATH_SEARCH_RADIUS_BLOCKS of the start; a search that runs out first
     * keeps the way to the node that came nearest the goal, so a far goal is
     * walked toward a stretch at a time. The way is written into the
     * PathStruct without the waypoints along a straight level run, leaving
     * only where it turns, climbs or drops. Every node array, the heap and the
     * node index are kept and reused, so a search allocates nothing.
     */

    // Internal
    private PathTerrainBranch pathTerrainBranch;

    // Nodes
    private int[] nodeX;
    private int[] nodeLayer;
    private int[] nodeZ;
    private int[] nodeParent;
    private float[] nodeCost;
    private float[] nodeEstimate;
    private boolean[] nodeClosed;
    private int[] nodeHeapIndex;
    private int nodeCount;
    private Long2IntOpenHashMap key2Node;

    // Heap
    private int[] heap;
    private int heapSize;

    // Chain
    private int[] chain;

    // Steps
    private int[] stepFloors;

    // Goal
    private int goalX;
    private int goalLayer;
    private int goalZ;

    // Start
    private int startX;
    private int startZ;

    // Internal \\

    @Override
    protected void create() {

        int capacity = EngineSetting.PATH_NODE_CAPACITY;

        // Nodes
        this.nodeX = new int[capacity];
        this.nodeLayer = new int[capacity];
        this.nodeZ = new int[capacity];
        this.nodeParent = new int[capacity];
        this.nodeCost = new float[capacity];
        this.nodeEstimate = new float[capacity];
        this.nodeClosed = new boolean[capacity];
        this.nodeHeapIndex = new int[capacity];
        this.key2Node = new Long2IntOpenHashMap(capacity);
        this.key2Node.defaultReturnValue(EngineSetting.PATH_NODE_NONE);

        // Heap
        this.heap = new int[capacity];

        // Chain
        this.chain = new int[capacity];

        // Steps
        this.stepFloors = new int[Direction2Vector.LENGTH];
    }

    @Override
    protected void get() {

        // Internal
        this.pathTerrainBranch = get(PathTerrainBranch.class);
    }

    // Search \\

    // The best way from where the entity stands toward a goal on the ground of a chunk, written into the path
    void search(
            EntityInstance entity,
            long goalChunkCoordinate,
            float goalPositionX,
            float goalPositionY,
            float goalPositionZ,
            PathStruct path) {

        WorldPositionStruct worldPositionStruct = entity.getWorldPositionStruct();
        long frameChunkCoordinate = worldPositionStruct.getChunkCoordinate();
        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                entity.getWorldHandle(),
                frameChunkCoordinate,
                goalChunkCoordinate);
        Vector3 position = worldPositionStruct.getPosition();
        Vector3 size = entity.getSize();
        float frameGoalX = Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE + goalPositionX;
        float frameGoalZ = Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE + goalPositionZ;

        startX = (int) Math.floor(position.x + size.x * 0.5f);
        startZ = (int) Math.floor(position.z + size.z * 0.5f);
        goalX = (int) Math.floor(frameGoalX);
        goalLayer = toLayer(goalPositionY);
        goalZ = (int) Math.floor(frameGoalZ);

        int startLayer = toLayer(position.y);

        path.begin(frameChunkCoordinate, frameGoalX, frameGoalZ);
        pathTerrainBranch.begin(entity, startX, startLayer, startZ);
        reset();

        int best = open(startX, startLayer, startZ, EngineSetting.PATH_NODE_NONE, 0f);
        float bestHeuristic = heuristic(startX, startLayer, startZ);
        int reached = EngineSetting.PATH_NODE_NONE;
        int expanded = 0;

        while (heapSize > 0 && expanded < EngineSetting.PATH_NODE_LIMIT) {

            int node = pop();

            nodeClosed[node] = true;
            expanded++;

            if (isGoal(node)) {
                reached = node;
                break;
            }

            float nodeHeuristic = heuristic(nodeX[node], nodeLayer[node], nodeZ[node]);

            if (nodeHeuristic < bestHeuristic) {
                best = node;
                bestHeuristic = nodeHeuristic;
            }

            expand(node);
        }

        boolean reachedGoal = reached != EngineSetting.PATH_NODE_NONE;
        boolean written = write(path, reachedGoal ? reached : best);

        path.finish(reachedGoal && written);
    }

    private void reset() {
        nodeCount = 0;
        heapSize = 0;
        key2Node.clear();
    }

    // The sub-block layer the feet stand in, lifted a hair so feet resting on a face read the layer above it
    private int toLayer(float feetY) {
        return SubBlockSampleUtility.toSub(feetY + EngineSetting.PATH_FEET_LIFT_BLOCKS);
    }

    // Expand \\

    // Every column around a node the body can step into — the four sides first, then each corner whose two sides
    // are open to it on level ground
    private void expand(int node) {

        int x = nodeX[node];
        int layer = nodeLayer[node];
        int z = nodeZ[node];
        Direction2Vector[] directions = Direction2Vector.VALUES;
        int count = Direction2Vector.LENGTH;

        for (int i = 0; i < count; i++)
            stepFloors[i] = isDiagonal(directions[i])
                    ? EngineSetting.PATH_LAYER_NONE
                    : resolveStep(x, layer, z, directions[i]);

        for (int i = 0; i < count; i++) {

            Direction2Vector direction = directions[i];

            if (!isDiagonal(direction))
                continue;

            if (stepFloors[(i + count - 1) % count] != layer || stepFloors[(i + 1) % count] != layer)
                continue;

            if (resolveStep(x, layer, z, direction) == layer)
                stepFloors[i] = layer;
        }

        for (int i = 0; i < count; i++) {

            int floor = stepFloors[i];

            if (floor == EngineSetting.PATH_LAYER_NONE)
                continue;

            relax(node, x + directions[i].x, floor, z + directions[i].y, resolveStepCost(directions[i], layer, floor));
        }
    }

    // The floor a step from a column toward a direction lands on, PATH_LAYER_NONE when it cannot be taken
    private int resolveStep(int x, int layer, int z, Direction2Vector direction) {

        int nextX = x + direction.x;
        int nextZ = z + direction.y;
        int radius = EngineSetting.PATH_SEARCH_RADIUS_BLOCKS;

        if (Math.abs(nextX - startX) > radius || Math.abs(nextZ - startZ) > radius)
            return EngineSetting.PATH_LAYER_NONE;

        int floor = pathTerrainBranch.resolveFloor(nextX, nextZ, layer);

        if (floor > layer && !pathTerrainBranch.hasHeadroom(x, z, layer, floor))
            return EngineSetting.PATH_LAYER_NONE;

        return floor;
    }

    private float resolveStepCost(Direction2Vector direction, int fromLayer, int toLayer) {

        float cost = isDiagonal(direction) ? EngineSetting.PATH_DIAGONAL_COST : EngineSetting.PATH_STRAIGHT_COST;

        if (toLayer > fromLayer)
            return cost + (toLayer - fromLayer) * EngineSetting.PATH_CLIMB_COST_PER_LAYER;

        return cost + (fromLayer - toLayer) * EngineSetting.PATH_DROP_COST_PER_LAYER;
    }

    private boolean isDiagonal(Direction2Vector direction) {
        return direction.x != 0 && direction.y != 0;
    }

    // Nodes \\

    // A node reached more cheaply than before is opened again with its new cost; one already closed stays closed
    private void relax(int parent, int x, int layer, int z, float stepCost) {

        long key = Coordinate3Long.pack(x, layer, z);
        int node = key2Node.get(key);
        float cost = nodeCost[parent] + stepCost;

        if (node == EngineSetting.PATH_NODE_NONE) {
            open(x, layer, z, parent, cost);
            return;
        }

        if (nodeClosed[node] || cost >= nodeCost[node])
            return;

        nodeParent[node] = parent;
        nodeCost[node] = cost;
        nodeEstimate[node] = cost + heuristic(x, layer, z) * EngineSetting.PATH_HEURISTIC_WEIGHT;
        siftUp(nodeHeapIndex[node]);
    }

    // A new node on the heap — none once every node is spent, the search going on with those it has
    private int open(int x, int layer, int z, int parent, float cost) {

        if (nodeCount >= nodeX.length)
            return EngineSetting.PATH_NODE_NONE;

        int node = nodeCount++;

        nodeX[node] = x;
        nodeLayer[node] = layer;
        nodeZ[node] = z;
        nodeParent[node] = parent;
        nodeCost[node] = cost;
        nodeEstimate[node] = cost + heuristic(x, layer, z) * EngineSetting.PATH_HEURISTIC_WEIGHT;
        nodeClosed[node] = false;
        key2Node.put(Coordinate3Long.pack(x, layer, z), node);
        push(node);

        return node;
    }

    // Octile distance to the goal's column, plus its height at the cheapest cost a layer can be crossed
    private float heuristic(int x, int layer, int z) {

        int distanceX = Math.abs(x - goalX);
        int distanceZ = Math.abs(z - goalZ);
        int diagonal = Math.min(distanceX, distanceZ);
        int straight = Math.max(distanceX, distanceZ) - diagonal;

        return straight * EngineSetting.PATH_STRAIGHT_COST
                + diagonal * EngineSetting.PATH_DIAGONAL_COST
                + Math.abs(layer - goalLayer) * EngineSetting.PATH_DROP_COST_PER_LAYER;
    }

    // Within a column of the goal on either side, near enough its height to step or drop to it
    private boolean isGoal(int node) {
        return Math.abs(nodeX[node] - goalX) <= EngineSetting.PATH_GOAL_RADIUS_BLOCKS
                && Math.abs(nodeZ[node] - goalZ) <= EngineSetting.PATH_GOAL_RADIUS_BLOCKS
                && Math.abs(nodeLayer[node] - goalLayer) <= EngineSetting.PATH_GOAL_LAYER_TOLERANCE;
    }

    // Heap \\

    private void push(int node) {

        heap[heapSize] = node;
        nodeHeapIndex[node] = heapSize;
        heapSize++;
        siftUp(heapSize - 1);
    }

    private int pop() {

        int top = heap[0];

        heapSize--;

        if (heapSize > 0) {
            place(heap[heapSize], 0);
            siftDown(0);
        }

        return top;
    }

    private void siftUp(int index) {

        int node = heap[index];
        float estimate = nodeEstimate[node];

        while (index > 0) {

            int parentIndex = (index - 1) >> 1;
            int parent = heap[parentIndex];

            if (nodeEstimate[parent] <= estimate)
                break;

            place(parent, index);
            index = parentIndex;
        }

        place(node, index);
    }

    private void siftDown(int index) {

        int node = heap[index];
        float estimate = nodeEstimate[node];

        while (true) {

            int child = (index << 1) + 1;

            if (child >= heapSize)
                break;

            if (child + 1 < heapSize && nodeEstimate[heap[child + 1]] < nodeEstimate[heap[child]])
                child++;

            if (nodeEstimate[heap[child]] >= estimate)
                break;

            place(heap[child], index);
            index = child;
        }

        place(node, index);
    }

    private void place(int node, int index) {
        heap[index] = node;
        nodeHeapIndex[node] = index;
    }

    // Write \\

    // The way from the start to a node, every waypoint along a straight level run left out — false when the path
    // filled before the way was all written
    private boolean write(PathStruct path, int end) {

        int length = 0;

        for (int node = end; node != EngineSetting.PATH_NODE_NONE; node = nodeParent[node])
            chain[length++] = node;

        for (int i = length - 2; i >= 0; i--) {

            int node = chain[i];

            if (i > 0 && isStraight(chain[i + 1], node, chain[i - 1]))
                continue;

            if (!path.addWaypoint(nodeX[node], nodeLayer[node], nodeZ[node]))
                return false;
        }

        return true;
    }

    // True when a node carries on its predecessor's step unchanged, level and in the same direction
    private boolean isStraight(int previous, int node, int next) {
        return nodeLayer[previous] == nodeLayer[node]
                && nodeLayer[node] == nodeLayer[next]
                && nodeX[node] - nodeX[previous] == nodeX[next] - nodeX[node]
                && nodeZ[node] - nodeZ[previous] == nodeZ[next] - nodeZ[node];
    }
}
