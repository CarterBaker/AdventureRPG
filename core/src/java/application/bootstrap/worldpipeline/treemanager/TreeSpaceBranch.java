package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.tree.TreeCastStruct;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeNodeStruct;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import application.bootstrap.worldpipeline.util.TreeNodeBoxUtility;
import application.bootstrap.worldpipeline.util.TreeRasterUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TreeSpaceBranch extends BranchPackage {

    /*
     * Main thread — where trees stand for everything that touches them, in a
     * chunk's frame: blocks from that chunk's corner across, the world's own
     * height up. The trees near a place are the ones the chunks around it
     * hold, each visited once however many of those chunks hold it. A ray
     * meets wood exactly as it is drawn, notches and all, by laying the wood
     * the ray could reach onto a small grid and walking it sub-voxel by
     * sub-voxel, and meets a leaf cluster at the ellipsoid it is drawn
     * around. A box moving through the world meets wood as the boxes its
     * nodes are made of, so an entity stands on a limb and walks into a trunk
     * but never into leaves.
     */

    // Internal
    private WorldStreamManager worldStreamManager;

    // Settings
    private int chunkSize;
    private int resolution;

    // Scratch
    private SubVoxelGridStruct grid;
    private ObjectArrayList<TreeInstance> nearby;
    private int[] boxes;
    private int[] cell;
    private int[] step;
    private float[] tMax;
    private float[] tDelta;
    private float[] rayOrigin;
    private float[] rayDirection;
    private int queryStamp;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        // Scratch
        this.grid = new SubVoxelGridStruct();
        this.nearby = new ObjectArrayList<>();
        this.boxes = new int[EngineSetting.TREE_NODE_MAX_BOXES * EngineSetting.BOX_INT_STRIDE];
        this.cell = new int[EngineSetting.AXIS_COUNT];
        this.step = new int[EngineSetting.AXIS_COUNT];
        this.tMax = new float[EngineSetting.AXIS_COUNT];
        this.tDelta = new float[EngineSetting.AXIS_COUNT];
        this.rayOrigin = new float[EngineSetting.AXIS_COUNT];
        this.rayDirection = new float[EngineSetting.AXIS_COUNT];
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Nearby \\

    // Every tree the chunks under a level box of the frame hold, each once
    private void gatherTrees(
            WorldHandle worldHandle,
            long chunkCoordinate,
            float minX,
            float minZ,
            float maxX,
            float maxZ) {

        nearby.clear();
        queryStamp++;

        int baseX = Coordinate2Long.unpackX(chunkCoordinate);
        int baseZ = Coordinate2Long.unpackY(chunkCoordinate);
        int firstX = Math.floorDiv((int) Math.floor(minX), chunkSize);
        int firstZ = Math.floorDiv((int) Math.floor(minZ), chunkSize);
        int lastX = Math.floorDiv((int) Math.floor(maxX), chunkSize);
        int lastZ = Math.floorDiv((int) Math.floor(maxZ), chunkSize);

        for (int offsetZ = firstZ; offsetZ <= lastZ; offsetZ++)
            for (int offsetX = firstX; offsetX <= lastX; offsetX++) {

                ChunkInstance chunk = worldStreamManager.getChunkInstance(WorldWrapUtility.wrapAroundWorld(
                        worldHandle, Coordinate2Long.pack(baseX + offsetX, baseZ + offsetZ)));

                if (chunk == null || chunk.getWorldHandle() != worldHandle)
                    continue;

                TreeInstance[] trees = chunk.getTreePaletteHandle().getTrees();

                for (int i = 0; i < trees.length; i++)
                    if (trees[i].visit(queryStamp))
                        nearby.add(trees[i]);
            }
    }

    // The block a tree's root stands in, in the frame — x and z across, y up
    private int resolveRootBlockX(WorldHandle worldHandle, long chunkCoordinate, TreeInstance tree) {
        return (int) WorldWrapUtility.wrappedBlockDeltaX(
                worldHandle, tree.getAnchorX(), (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize);
    }

    private int resolveRootBlockZ(WorldHandle worldHandle, long chunkCoordinate, TreeInstance tree) {
        return (int) WorldWrapUtility.wrappedBlockDeltaZ(
                worldHandle, tree.getAnchorZ(), (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize);
    }

    // Cast \\

    // The nearest wood or leaf cluster a ray from a point of the frame meets within its reach
    void cast(
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3 origin,
            Vector3 direction,
            float maxDistance,
            TreeCastStruct out) {

        out.clear();

        float endX = origin.x + direction.x * maxDistance;
        float endY = origin.y + direction.y * maxDistance;
        float endZ = origin.z + direction.z * maxDistance;
        float minX = Math.min(origin.x, endX);
        float minY = Math.min(origin.y, endY);
        float minZ = Math.min(origin.z, endZ);
        float maxX = Math.max(origin.x, endX);
        float maxY = Math.max(origin.y, endY);
        float maxZ = Math.max(origin.z, endZ);

        gatherTrees(worldHandle, chunkCoordinate, minX, minZ, maxX, maxZ);

        for (int i = 0; i < nearby.size(); i++) {

            TreeInstance tree = nearby.get(i);
            TreeShapeStruct shape = tree.getShape();
            int rootBlockX = resolveRootBlockX(worldHandle, chunkCoordinate, tree);
            int rootBlockZ = resolveRootBlockZ(worldHandle, chunkCoordinate, tree);
            float rootX = rootBlockX + EngineSetting.TREE_ROOT_CENTER_BLOCKS;
            float rootY = tree.getBaseY();
            float rootZ = rootBlockZ + EngineSetting.TREE_ROOT_CENTER_BLOCKS;

            if (shape.isEmpty()
                    || rootX + shape.getMaxX() < minX || rootX + shape.getMinX() > maxX
                    || rootY + shape.getMaxY() < minY || rootY + shape.getMinY() > maxY
                    || rootZ + shape.getMaxZ() < minZ || rootZ + shape.getMinZ() > maxZ)
                continue;

            castLeaves(tree, shape, rootX, rootY, rootZ, origin, direction, maxDistance, out);
            castWood(tree, shape, rootBlockX, tree.getBaseY(), rootBlockZ,
                    (int) Math.floor(minX) - 1, (int) Math.floor(minY) - 1, (int) Math.floor(minZ) - 1,
                    (int) Math.floor(maxX) + 2, (int) Math.floor(maxY) + 2, (int) Math.floor(maxZ) + 2,
                    origin, direction, maxDistance, out);
        }

        if (out.isHit())
            out.setPoint(
                    origin.x + direction.x * out.getDistance(),
                    origin.y + direction.y * out.getDistance(),
                    origin.z + direction.z * out.getDistance());
    }

    // The nearest cluster ellipsoid the ray enters, nearer than anything met so far
    private void castLeaves(
            TreeInstance tree,
            TreeShapeStruct shape,
            float rootX,
            float rootY,
            float rootZ,
            Vector3 origin,
            Vector3 direction,
            float maxDistance,
            TreeCastStruct out) {

        for (int leaf = 0; leaf < shape.getLeafCount(); leaf++) {

            float radiusH = shape.getLeafRadiusH(leaf) * EngineSetting.TREE_LEAF_CAST_SHARE;
            float radiusV = shape.getLeafRadiusV(leaf) * EngineSetting.TREE_LEAF_CAST_SHARE;
            float originX = (origin.x - rootX - shape.getLeafX(leaf)) / radiusH;
            float originY = (origin.y - rootY - shape.getLeafY(leaf)) / radiusV;
            float originZ = (origin.z - rootZ - shape.getLeafZ(leaf)) / radiusH;
            float stepX = direction.x / radiusH;
            float stepY = direction.y / radiusV;
            float stepZ = direction.z / radiusH;
            float a = stepX * stepX + stepY * stepY + stepZ * stepZ;
            float b = originX * stepX + originY * stepY + originZ * stepZ;
            float c = originX * originX + originY * originY + originZ * originZ - 1f;
            float discriminant = b * b - a * c;

            if (c <= 0f || discriminant < 0f)
                continue;

            float distance = (-b - (float) Math.sqrt(discriminant)) / a;

            if (distance > 0f && distance <= maxDistance && distance < out.getDistance())
                out.setLeaf(tree, distance, leaf);
        }
    }

    // The first sub-voxel of wood the ray enters within a block region of the frame, nearer than anything met so far
    private void castWood(
            TreeInstance tree,
            TreeShapeStruct shape,
            int rootBlockX,
            int rootBlockY,
            int rootBlockZ,
            int regionMinX,
            int regionMinY,
            int regionMinZ,
            int regionMaxX,
            int regionMaxY,
            int regionMaxZ,
            Vector3 origin,
            Vector3 direction,
            float maxDistance,
            TreeCastStruct out) {

        TreeNodeStruct nodes = shape.getNodes();

        if (nodes.isEmpty())
            return;

        int sizeX = (regionMaxX - regionMinX) * resolution;
        int sizeY = (regionMaxY - regionMinY) * resolution;
        int sizeZ = (regionMaxZ - regionMinZ) * resolution;
        int gridRootX = rootBlockX - regionMinX;
        int gridRootY = rootBlockY - regionMinY;
        int gridRootZ = rootBlockZ - regionMinZ;

        grid.clear();
        TreeRasterUtility.rasterize(
                shape, gridRootX, gridRootY, gridRootZ, 0, 0, 0, sizeX, sizeY, sizeZ, 0, grid);

        if (grid.isEmpty())
            return;

        rayOrigin[0] = (origin.x - regionMinX) * resolution;
        rayOrigin[1] = (origin.y - regionMinY) * resolution;
        rayOrigin[2] = (origin.z - regionMinZ) * resolution;
        rayDirection[0] = direction.x;
        rayDirection[1] = direction.y;
        rayDirection[2] = direction.z;

        float limit = Math.min(maxDistance, out.getDistance()) * resolution;
        int faceAxis = walk(sizeX, sizeY, sizeZ, limit);

        if (faceAxis == EngineSetting.INDEX_NOT_FOUND)
            return;

        float distance = Math.max(0f, tMax[faceAxis] - tDelta[faceAxis]) / resolution;
        int cellX = cell[0] - gridRootX * resolution;
        int cellY = cell[1] - gridRootY * resolution;
        int cellZ = cell[2] - gridRootZ * resolution;

        out.setWood(tree, distance, cellX, cellY, cellZ, faceAxis, step[faceAxis] < 0,
                nodes.findNode(
                        Math.floorDiv(cellX, resolution),
                        Math.floorDiv(cellY, resolution),
                        Math.floorDiv(cellZ, resolution)));
    }

    // Steps cell by cell from the ray's origin until a filled cell — the axis of the face it entered through, or
    // INDEX_NOT_FOUND when nothing is met within the limit and the grid
    private int walk(int sizeX, int sizeY, int sizeZ, float limit) {

        int[] size = { sizeX, sizeY, sizeZ };

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {

            cell[axis] = (int) Math.floor(rayOrigin[axis]);
            step[axis] = rayDirection[axis] > 0f ? 1 : -1;

            float speed = Math.abs(rayDirection[axis]);

            tDelta[axis] = speed > 0f ? 1f / speed : Float.MAX_VALUE;
            tMax[axis] = speed > 0f
                    ? (rayDirection[axis] > 0f ? cell[axis] + 1 - rayOrigin[axis] : rayOrigin[axis] - cell[axis])
                            / speed
                    : Float.MAX_VALUE;
        }

        while (true) {

            int axis = tMax[0] <= tMax[1] && tMax[0] <= tMax[2] ? 0 : tMax[1] <= tMax[2] ? 1 : 2;

            if (tMax[axis] > limit)
                return EngineSetting.INDEX_NOT_FOUND;

            cell[axis] += step[axis];
            tMax[axis] += tDelta[axis];

            if (cell[axis] < 0 && step[axis] < 0 || cell[axis] >= size[axis] && step[axis] > 0)
                return EngineSetting.INDEX_NOT_FOUND;

            if (cell[0] >= 0 && cell[1] >= 0 && cell[2] >= 0
                    && cell[0] < sizeX && cell[1] < sizeY && cell[2] < sizeZ
                    && grid.isFilled(cell[0], cell[1], cell[2]))
                return axis;
        }
    }

    // Collision \\

    // Every wood box of a tree that overlaps a box of the frame, appended six floats each — min corner then max
    void collectWoodBoxes(
            WorldHandle worldHandle,
            long chunkCoordinate,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            FloatArrayList out) {

        gatherTrees(worldHandle, chunkCoordinate, minX, minZ, maxX, maxZ);

        for (int i = 0; i < nearby.size(); i++) {

            TreeInstance tree = nearby.get(i);
            TreeNodeStruct nodes = tree.getShape().getNodes();

            if (nodes.isEmpty())
                continue;

            int rootBlockX = resolveRootBlockX(worldHandle, chunkCoordinate, tree);
            int rootBlockY = tree.getBaseY();
            int rootBlockZ = resolveRootBlockZ(worldHandle, chunkCoordinate, tree);
            int spill = Math.max(0, nodes.getMaxRadius() - resolution / 2 + resolution - 1) / resolution;

            for (int blockY = (int) Math.floor(minY) - rootBlockY - spill;
                    blockY <= (int) Math.floor(maxY) - rootBlockY + spill; blockY++)
                for (int blockZ = (int) Math.floor(minZ) - rootBlockZ - spill;
                        blockZ <= (int) Math.floor(maxZ) - rootBlockZ + spill; blockZ++)
                    for (int blockX = (int) Math.floor(minX) - rootBlockX - spill;
                            blockX <= (int) Math.floor(maxX) - rootBlockX + spill; blockX++) {

                        int node = nodes.findNode(blockX, blockY, blockZ);

                        if (node != EngineSetting.INDEX_NOT_FOUND)
                            addNodeBoxes(nodes, node, rootBlockX + blockX, rootBlockY + blockY, rootBlockZ + blockZ,
                                    minX, minY, minZ, maxX, maxY, maxZ, out);
                    }
        }
    }

    private void addNodeBoxes(
            TreeNodeStruct nodes,
            int node,
            int blockX,
            int blockY,
            int blockZ,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            FloatArrayList out) {

        int count = TreeNodeBoxUtility.resolveBoxes(nodes, node, 0, boxes);
        float scale = 1f / resolution;

        for (int box = 0; box < count; box++) {

            int base = box * EngineSetting.BOX_INT_STRIDE;
            float boxMinX = blockX + boxes[base + EngineSetting.BOX_MIN_X] * scale;
            float boxMinY = blockY + boxes[base + EngineSetting.BOX_MIN_Y] * scale;
            float boxMinZ = blockZ + boxes[base + EngineSetting.BOX_MIN_Z] * scale;
            float boxMaxX = blockX + boxes[base + EngineSetting.BOX_MAX_X] * scale;
            float boxMaxY = blockY + boxes[base + EngineSetting.BOX_MAX_Y] * scale;
            float boxMaxZ = blockZ + boxes[base + EngineSetting.BOX_MAX_Z] * scale;

            if (boxMaxX <= minX || boxMinX >= maxX || boxMaxY <= minY || boxMinY >= maxY
                    || boxMaxZ <= minZ || boxMinZ >= maxZ)
                continue;

            out.add(boxMinX);
            out.add(boxMinY);
            out.add(boxMinZ);
            out.add(boxMaxX);
            out.add(boxMaxY);
            out.add(boxMaxZ);
        }
    }
}
