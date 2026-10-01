package application.bootstrap.worldpipeline.worlditemmanager;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.itemdefinition.LidClearanceStruct;
import application.bootstrap.itempipeline.itemrotationmanager.ItemRotationBufferSystem;
import application.bootstrap.physicspipeline.util.SubBlockSampleUtility;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worlditem.WorldItemCastStruct;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle;
import application.bootstrap.worldpipeline.worlditem.WorldItemPlacementStruct;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate4Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldItemSpaceSystem extends SystemPackage {

    /*
     * The one place world item space is queried. A placed item claims the
     * sub-voxel cells of its shape, turned by its orientation inside the
     * one-block model grid cornered at its packed position, and carries the
     * rough box around them; an open container also claims its lid's clearance.
     * fits() decides whether a shape may stand somewhere — inside the world,
     * clear of solid blocks and sub-blocks and of every claimed cell — and
     * resolvePlacement() sets a shape flush on a face and pushes it out until
     * it fits, as resolveCornerPlacement() does from a given corner.
     * isRegionClaimed() keeps blocks out of items, isLidClear() decides whether
     * a container may open, collectSolidBoxes() hands movement the rough boxes
     * near an entity, cast() finds the first claimed cell a ray meets, and
     * findColumnTop() tells precipitation where the highest item over a block
     * column ends. Queries read committed live palettes on the main thread
     * only, visit each candidate once by stamp, and allocate nothing;
     * resolveBounds() alone also runs on the streaming thread and touches no
     * shared state.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private ItemRotationBufferSystem itemRotationBufferSystem;

    // Settings
    private int chunkSize;
    private int resolution;
    private int chunkSpan;
    private int subVoxelsPerSubBlock;
    private int worldTopBlock;
    private int worldTop;
    private int placementPushLimit;
    private float rayEpsilon;
    private int axisX;
    private int axisY;
    private int axisZ;

    // Query
    private int queryStamp;
    private boolean queryComplete;
    private ObjectArrayList<WorldItemInstance> candidates;
    private IntArrayList candidateOffsetX;
    private IntArrayList candidateOffsetZ;

    // Open Containers
    private ObjectArrayList<WorldItemInstance> openContainers;
    private IntArrayList openOffsetX;
    private IntArrayList openOffsetZ;

    // Scratch — sub-voxel bounds, minimum inclusive and maximum exclusive, indexed by axis
    private int[] boundsMin;
    private int[] boundsMax;

    // Scratch — placement, indexed by axis
    private int[] cornerScratch;
    private int[] anchorScratch;
    private int[] faceScratch;

    // Scratch — solid sub-blocks under a shape's bounds
    private boolean[] solidScratch;
    private int[] solidOrigin;
    private int[] solidSpan;

    // Scratch — cast, indexed by axis
    private float[] rayOrigin;
    private float[] rayDirection;
    private int[] cellScratch;
    private int[] stepScratch;
    private float[] tMaxScratch;
    private float[] tDeltaScratch;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        this.chunkSpan = chunkSize * resolution;
        this.subVoxelsPerSubBlock = resolution / SubBlockUtility.DIVISIONS;
        this.worldTopBlock = EngineSetting.WORLD_HEIGHT * chunkSize;
        this.worldTop = worldTopBlock * resolution;
        this.placementPushLimit = EngineSetting.ITEM_PLACEMENT_PUSH_LIMIT;
        this.rayEpsilon = EngineSetting.ITEM_RAY_EPSILON;
        this.axisX = EngineSetting.AXIS_X;
        this.axisY = EngineSetting.AXIS_Y;
        this.axisZ = EngineSetting.AXIS_Z;

        // Query
        this.candidates = new ObjectArrayList<>();
        this.candidateOffsetX = new IntArrayList();
        this.candidateOffsetZ = new IntArrayList();

        // Open Containers
        this.openContainers = new ObjectArrayList<>();
        this.openOffsetX = new IntArrayList();
        this.openOffsetZ = new IntArrayList();

        // Scratch
        this.boundsMin = new int[EngineSetting.AXIS_COUNT];
        this.boundsMax = new int[EngineSetting.AXIS_COUNT];
        this.cornerScratch = new int[EngineSetting.AXIS_COUNT];
        this.anchorScratch = new int[EngineSetting.AXIS_COUNT];
        this.faceScratch = new int[EngineSetting.AXIS_COUNT];
        this.solidSpan = new int[EngineSetting.AXIS_COUNT];
        this.solidOrigin = new int[EngineSetting.AXIS_COUNT];
        this.solidScratch = new boolean[countSolidCells()];
        this.rayOrigin = new float[EngineSetting.AXIS_COUNT];
        this.rayDirection = new float[EngineSetting.AXIS_COUNT];
        this.cellScratch = new int[EngineSetting.AXIS_COUNT];
        this.stepScratch = new int[EngineSetting.AXIS_COUNT];
        this.tMaxScratch = new float[EngineSetting.AXIS_COUNT];
        this.tDeltaScratch = new float[EngineSetting.AXIS_COUNT];
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.itemRotationBufferSystem = get(ItemRotationBufferSystem.class);
    }

    // A one-block shape spans at most three sub-blocks on an axis, wherever its corner falls
    private int countSolidCells() {

        int span = resolution / subVoxelsPerSubBlock + 1;

        return span * span * span;
    }

    // Bounds \\

    // The item's rough box — safe on the streaming thread, as it touches no shared scratch
    public void resolveBounds(WorldItemInstance instance) {

        ItemShapeStruct shape = instance.getItemDefinitionHandle().getShape();
        long packed = instance.getPackedPosition();
        int orientation = Coordinate4Long.unpackW(packed);
        int cornerX = Coordinate4Long.unpackX(packed);
        int cornerY = Coordinate4Long.unpackY(packed);
        int cornerZ = Coordinate4Long.unpackZ(packed);

        instance.setBounds(
                cornerX + rotateShapeMin(shape, orientation, axisX),
                cornerY + rotateShapeMin(shape, orientation, axisY),
                cornerZ + rotateShapeMin(shape, orientation, axisZ),
                cornerX + rotateShapeMax(shape, orientation, axisX) + 1,
                cornerY + rotateShapeMax(shape, orientation, axisY) + 1,
                cornerZ + rotateShapeMax(shape, orientation, axisZ) + 1);
    }

    private void resolveShapeBounds(ItemShapeStruct shape, int orientation, int[] corner) {

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {
            boundsMin[axis] = corner[axis] + rotateShapeMin(shape, orientation, axis);
            boundsMax[axis] = corner[axis] + rotateShapeMax(shape, orientation, axis) + 1;
        }
    }

    private int rotateShapeMin(ItemShapeStruct shape, int orientation, int axis) {
        return Math.min(rotateShapeFirst(shape, orientation, axis), rotateShapeLast(shape, orientation, axis));
    }

    private int rotateShapeMax(ItemShapeStruct shape, int orientation, int axis) {
        return Math.max(rotateShapeFirst(shape, orientation, axis), rotateShapeLast(shape, orientation, axis));
    }

    private int rotateShapeFirst(ItemShapeStruct shape, int orientation, int axis) {
        return itemRotationBufferSystem.rotateCell(
                orientation, axis, shape.getOffsetX(), shape.getOffsetY(), shape.getOffsetZ());
    }

    private int rotateShapeLast(ItemShapeStruct shape, int orientation, int axis) {
        return itemRotationBufferSystem.rotateCell(
                orientation,
                axis,
                shape.getOffsetX() + shape.getSizeX() - 1,
                shape.getOffsetY() + shape.getSizeY() - 1,
                shape.getOffsetZ() + shape.getSizeZ() - 1);
    }

    private void resolveClearanceBounds(LidClearanceStruct clearance, int orientation, int[] corner) {

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {

            int first = itemRotationBufferSystem.rotateCell(
                    orientation, axis, clearance.getMinX(), clearance.getMinY(), clearance.getMinZ());
            int last = itemRotationBufferSystem.rotateCell(
                    orientation, axis, clearance.getMaxX(), clearance.getMaxY(), clearance.getMaxZ());

            boundsMin[axis] = corner[axis] + Math.min(first, last);
            boundsMax[axis] = corner[axis] + Math.max(first, last) + 1;
        }
    }

    // Fit \\

    // True when the turned shape cornered here, in the frame chunk's sub-voxels, claims nothing already taken
    public boolean fits(
            WorldHandle world,
            long frameChunk,
            ItemDefinitionHandle itemDefinitionHandle,
            int orientation,
            int cornerX,
            int cornerY,
            int cornerZ) {

        ItemShapeStruct shape = itemDefinitionHandle.getShape();

        cornerScratch[axisX] = cornerX;
        cornerScratch[axisY] = cornerY;
        cornerScratch[axisZ] = cornerZ;
        resolveShapeBounds(shape, orientation, cornerScratch);

        if (boundsMin[axisY] < 0 || boundsMax[axisY] > worldTop)
            return false;

        if (!gatherBounds(world, frameChunk))
            return false;

        sampleSolidSubBlocks(frameChunk);

        for (int i = 0; i < shape.getCellCount(); i++) {

            int gridX = shape.getOffsetX() + shape.getCellX(i);
            int gridY = shape.getOffsetY() + shape.getCellY(i);
            int gridZ = shape.getOffsetZ() + shape.getCellZ(i);

            int x = cornerX + itemRotationBufferSystem.rotateCell(orientation, axisX, gridX, gridY, gridZ);
            int y = cornerY + itemRotationBufferSystem.rotateCell(orientation, axisY, gridX, gridY, gridZ);
            int z = cornerZ + itemRotationBufferSystem.rotateCell(orientation, axisZ, gridX, gridY, gridZ);

            if (isSampledSolid(x, y, z) || isCellTaken(x, y, z, null))
                return false;
        }

        return true;
    }

    // Placement \\

    // The shape flush on the face, centred on the anchor cell outside it, pushed out along the face until it fits
    public boolean resolvePlacement(
            WorldHandle world,
            long frameChunk,
            ItemDefinitionHandle itemDefinitionHandle,
            int orientation,
            int anchorX,
            int anchorY,
            int anchorZ,
            Direction3Vector face,
            WorldItemPlacementStruct out) {

        ItemShapeStruct shape = itemDefinitionHandle.getShape();

        anchorScratch[axisX] = anchorX;
        anchorScratch[axisY] = anchorY;
        anchorScratch[axisZ] = anchorZ;
        faceScratch[axisX] = face.x;
        faceScratch[axisY] = face.y;
        faceScratch[axisZ] = face.z;

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {

            int low = rotateShapeMin(shape, orientation, axis);
            int high = rotateShapeMax(shape, orientation, axis);

            if (faceScratch[axis] > 0)
                cornerScratch[axis] = anchorScratch[axis] - low;
            else if (faceScratch[axis] < 0)
                cornerScratch[axis] = anchorScratch[axis] - high;
            else
                cornerScratch[axis] = anchorScratch[axis] - (low + high) / 2;
        }

        return resolveCornerPlacement(
                world,
                frameChunk,
                itemDefinitionHandle,
                orientation,
                cornerScratch[axisX],
                cornerScratch[axisY],
                cornerScratch[axisZ],
                face,
                out);
    }

    // The model grid cornered here, pushed out along a direction until the shape fits
    public boolean resolveCornerPlacement(
            WorldHandle world,
            long frameChunk,
            ItemDefinitionHandle itemDefinitionHandle,
            int orientation,
            int baseX,
            int baseY,
            int baseZ,
            Direction3Vector push,
            WorldItemPlacementStruct out) {

        for (int step = 0; step <= placementPushLimit; step++) {

            int cornerX = baseX + push.x * step;
            int cornerY = baseY + push.y * step;
            int cornerZ = baseZ + push.z * step;

            if (!fits(world, frameChunk, itemDefinitionHandle, orientation, cornerX, cornerY, cornerZ))
                continue;

            int chunkOffsetX = Math.floorDiv(cornerX, chunkSpan);
            int chunkOffsetZ = Math.floorDiv(cornerZ, chunkSpan);

            out.set(
                    WorldWrapUtility.wrapAroundWorld(
                            world, Coordinate2Long.add(frameChunk, chunkOffsetX, chunkOffsetZ)),
                    Coordinate4Long.pack(
                            Math.floorMod(cornerX, chunkSpan),
                            cornerY,
                            Math.floorMod(cornerZ, chunkSpan),
                            orientation));

            return true;
        }

        return false;
    }

    // Blocks \\

    // True when any item cell lies in the region, or the region reaches items not yet loaded
    public boolean isRegionClaimed(
            WorldHandle world,
            long frameChunk,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ) {

        boundsMin[axisX] = minX;
        boundsMin[axisY] = minY;
        boundsMin[axisZ] = minZ;
        boundsMax[axisX] = maxX;
        boundsMax[axisY] = maxY;
        boundsMax[axisZ] = maxZ;

        if (!gatherBounds(world, frameChunk))
            return true;

        if (candidates.isEmpty() && openContainers.isEmpty())
            return false;

        for (int z = minZ; z < maxZ; z++)
            for (int y = minY; y < maxY; y++)
                for (int x = minX; x < maxX; x++)
                    if (isCellTaken(x, y, z, null))
                        return true;

        return false;
    }

    // Lid \\

    // True when nothing but air or liquid fills the container's lid clearance
    public boolean isLidClear(WorldItemInstance container) {

        LidClearanceStruct clearance = container.getItemDefinitionHandle().getContainerSpace().getLidClearance();

        if (clearance.isEmpty())
            return true;

        long frameChunk = container.getChunkCoordinate();
        ChunkInstance chunk = worldStreamManager.getChunkInstance(frameChunk);

        if (chunk == null)
            return false;

        long packed = container.getPackedPosition();
        int orientation = Coordinate4Long.unpackW(packed);
        int cornerX = Coordinate4Long.unpackX(packed);
        int cornerY = Coordinate4Long.unpackY(packed);
        int cornerZ = Coordinate4Long.unpackZ(packed);

        cornerScratch[axisX] = cornerX;
        cornerScratch[axisY] = cornerY;
        cornerScratch[axisZ] = cornerZ;
        resolveClearanceBounds(clearance, orientation, cornerScratch);

        if (!gatherBounds(chunk.getWorldHandle(), frameChunk))
            return false;

        for (int i = 0; i < clearance.getCellCount(); i++) {

            int gridX = clearance.getCellX(i);
            int gridY = clearance.getCellY(i);
            int gridZ = clearance.getCellZ(i);

            int x = cornerX + itemRotationBufferSystem.rotateCell(orientation, axisX, gridX, gridY, gridZ);
            int y = cornerY + itemRotationBufferSystem.rotateCell(orientation, axisY, gridX, gridY, gridZ);
            int z = cornerZ + itemRotationBufferSystem.rotateCell(orientation, axisZ, gridX, gridY, gridZ);

            if (isSolid(frameChunk, x, y, z) || isCellTaken(x, y, z, container))
                return false;
        }

        return true;
    }

    // An open container claims its clearance until it closes, so nothing is set where its lid comes down
    public void setOpen(WorldItemInstance container, boolean open) {

        if (!open) {
            openContainers.remove(container);
            return;
        }

        if (!openContainers.contains(container))
            openContainers.add(container);
    }

    // Collision \\

    // The rough boxes of solid items near a region given in the frame chunk's blocks, six floats each
    public void collectSolidBoxes(
            WorldHandle world,
            long frameChunk,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            FloatArrayList out) {

        out.clear();
        beginQuery(world, frameChunk);
        collect(
                world,
                frameChunk,
                (int) Math.floor(minX),
                (int) Math.floor(minY),
                (int) Math.floor(minZ),
                (int) Math.floor(maxX),
                (int) Math.floor(maxY),
                (int) Math.floor(maxZ));

        for (int i = 0; i < candidates.size(); i++) {

            WorldItemInstance item = candidates.get(i);

            if (!item.getItemDefinitionHandle().isSolid())
                continue;

            float boxMinX = (item.getMinX() + candidateOffsetX.getInt(i)) / (float) resolution;
            float boxMinY = item.getMinY() / (float) resolution;
            float boxMinZ = (item.getMinZ() + candidateOffsetZ.getInt(i)) / (float) resolution;
            float boxMaxX = (item.getMaxX() + candidateOffsetX.getInt(i)) / (float) resolution;
            float boxMaxY = item.getMaxY() / (float) resolution;
            float boxMaxZ = (item.getMaxZ() + candidateOffsetZ.getInt(i)) / (float) resolution;

            if (boxMaxX <= minX || boxMinX >= maxX
                    || boxMaxY <= minY || boxMinY >= maxY
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

    // Cast \\

    // Walks the blocks along the ray and tests each item that can reach them, stopping past the nearest hit
    public void cast(
            WorldHandle world,
            long frameChunk,
            Vector3 origin,
            Vector3 direction,
            float maxDistance,
            WorldItemCastStruct out) {

        out.setHit(false);
        beginQuery(world, frameChunk);

        int blockX = (int) Math.floor(origin.x);
        int blockY = (int) Math.floor(origin.y);
        int blockZ = (int) Math.floor(origin.z);

        int stepX = direction.x > 0 ? 1 : (direction.x < 0 ? -1 : 0);
        int stepY = direction.y > 0 ? 1 : (direction.y < 0 ? -1 : 0);
        int stepZ = direction.z > 0 ? 1 : (direction.z < 0 ? -1 : 0);

        float tDeltaX = stepX != 0 ? Math.abs(1f / direction.x) : Float.MAX_VALUE;
        float tDeltaY = stepY != 0 ? Math.abs(1f / direction.y) : Float.MAX_VALUE;
        float tDeltaZ = stepZ != 0 ? Math.abs(1f / direction.z) : Float.MAX_VALUE;

        float tMaxX = resolveFirstCrossing(origin.x, direction.x, blockX, stepX);
        float tMaxY = resolveFirstCrossing(origin.y, direction.y, blockY, stepY);
        float tMaxZ = resolveFirstCrossing(origin.z, direction.z, blockZ, stepZ);

        float reach = maxDistance;
        float t = 0f;
        int tested = 0;

        while (t < reach) {

            collect(world, frameChunk, blockX, blockY, blockZ, blockX, blockY, blockZ);

            for (; tested < candidates.size(); tested++)
                reach = castCandidate(tested, origin, direction, reach, out);

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                t = tMaxX;
                blockX += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                t = tMaxY;
                blockY += stepY;
                tMaxY += tDeltaY;
            } else {
                t = tMaxZ;
                blockZ += stepZ;
                tMaxZ += tDeltaZ;
            }
        }
    }

    private float resolveFirstCrossing(float origin, float direction, int block, int step) {

        if (step > 0)
            return (block + 1 - origin) / direction;

        if (step < 0)
            return (origin - block) / -direction;

        return Float.MAX_VALUE;
    }

    // The new reach — the distance to this item's first claimed cell when it is nearer than the old one
    private float castCandidate(int index, Vector3 origin, Vector3 direction, float reach, WorldItemCastStruct out) {

        WorldItemInstance item = candidates.get(index);

        rayOrigin[axisX] = origin.x * resolution - candidateOffsetX.getInt(index);
        rayOrigin[axisY] = origin.y * resolution;
        rayOrigin[axisZ] = origin.z * resolution - candidateOffsetZ.getInt(index);
        rayDirection[axisX] = direction.x * resolution;
        rayDirection[axisY] = direction.y * resolution;
        rayDirection[axisZ] = direction.z * resolution;

        boundsMin[axisX] = item.getMinX();
        boundsMin[axisY] = item.getMinY();
        boundsMin[axisZ] = item.getMinZ();
        boundsMax[axisX] = item.getMaxX();
        boundsMax[axisY] = item.getMaxY();
        boundsMax[axisZ] = item.getMaxZ();

        float near = 0f;
        float far = reach;
        int faceAxis = EngineSetting.INDEX_NOT_FOUND;

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {

            if (rayDirection[axis] == 0f) {

                if (rayOrigin[axis] < boundsMin[axis] || rayOrigin[axis] >= boundsMax[axis])
                    return reach;

                continue;
            }

            float first = (boundsMin[axis] - rayOrigin[axis]) / rayDirection[axis];
            float second = (boundsMax[axis] - rayOrigin[axis]) / rayDirection[axis];
            float enter = Math.min(first, second);

            if (enter > near) {
                near = enter;
                faceAxis = axis;
            }

            far = Math.min(far, Math.max(first, second));
        }

        if (near > far)
            return reach;

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {

            int cell = (int) Math.floor(rayOrigin[axis] + rayDirection[axis] * (near + rayEpsilon));

            cellScratch[axis] = Math.max(boundsMin[axis], Math.min(boundsMax[axis] - 1, cell));
            stepScratch[axis] = rayDirection[axis] > 0 ? 1 : (rayDirection[axis] < 0 ? -1 : 0);
            tDeltaScratch[axis] = stepScratch[axis] != 0 ? Math.abs(1f / rayDirection[axis]) : Float.MAX_VALUE;

            if (stepScratch[axis] > 0)
                tMaxScratch[axis] = (cellScratch[axis] + 1 - rayOrigin[axis]) / rayDirection[axis];
            else if (stepScratch[axis] < 0)
                tMaxScratch[axis] = (cellScratch[axis] - rayOrigin[axis]) / rayDirection[axis];
            else
                tMaxScratch[axis] = Float.MAX_VALUE;
        }

        float t = near;

        while (true) {

            // The cell the ray starts in is never a hit, exactly as a block cast never hits its own cell
            if (faceAxis != EngineSetting.INDEX_NOT_FOUND
                    && claimsCell(item, cellScratch[axisX], cellScratch[axisY], cellScratch[axisZ])) {
                writeHit(item, t, faceAxis, out);
                return t;
            }

            int axis = tMaxScratch[axisX] < tMaxScratch[axisY]
                    ? (tMaxScratch[axisX] < tMaxScratch[axisZ] ? axisX : axisZ)
                    : (tMaxScratch[axisY] < tMaxScratch[axisZ] ? axisY : axisZ);

            t = tMaxScratch[axis];

            if (t >= reach || t > far)
                return reach;

            cellScratch[axis] += stepScratch[axis];
            tMaxScratch[axis] += tDeltaScratch[axis];
            faceAxis = axis;

            if (cellScratch[axis] < boundsMin[axis] || cellScratch[axis] >= boundsMax[axis])
                return reach;
        }
    }

    private void writeHit(WorldItemInstance item, float distance, int faceAxis, WorldItemCastStruct out) {

        int normal = rayDirection[faceAxis] > 0 ? -1 : 1;
        Direction3Vector face = faceAxis == axisX
                ? Direction3Vector.getDirectionX(normal)
                : faceAxis == axisY
                        ? Direction3Vector.getDirectionY(normal)
                        : Direction3Vector.getDirectionZ(normal);

        out.setHit(true);
        out.setDistance(distance);
        out.setWorldItemInstance(item);
        out.setHitFace(face);
        out.setChunkCoordinate(item.getChunkCoordinate());
        out.setAnchor(
                cellScratch[axisX] + face.x,
                cellScratch[axisY] + face.y,
                cellScratch[axisZ] + face.z);
    }

    // Precipitation \\

    // The sub-voxel Y just above the highest item box over a block column of the chunk, 0 when there is none
    public int findColumnTop(WorldHandle world, long chunkCoordinate, int blockX, int blockZ) {

        int top = 0;
        int columnMinX = blockX * resolution;
        int columnMinZ = blockZ * resolution;

        for (int cornerZ = blockZ - 1; cornerZ <= blockZ; cornerZ++)
            for (int cornerX = blockX - 1; cornerX <= blockX; cornerX++) {

                int chunkOffsetX = Math.floorDiv(cornerX, chunkSize);
                int chunkOffsetZ = Math.floorDiv(cornerZ, chunkSize);
                WorldItemInstancePaletteHandle palette = findCommittedPalette(
                        world, chunkCoordinate, chunkOffsetX, chunkOffsetZ);

                if (palette == null)
                    continue;

                ObjectArrayList<WorldItemInstance> items = palette.getItemsAtColumn(
                        Math.floorMod(cornerX, chunkSize), Math.floorMod(cornerZ, chunkSize));

                if (items == null)
                    continue;

                int offsetX = chunkOffsetX * chunkSpan;
                int offsetZ = chunkOffsetZ * chunkSpan;

                for (int i = 0; i < items.size(); i++) {

                    WorldItemInstance item = items.get(i);

                    if (item.getMinX() + offsetX >= columnMinX + resolution
                            || item.getMaxX() + offsetX <= columnMinX
                            || item.getMinZ() + offsetZ >= columnMinZ + resolution
                            || item.getMaxZ() + offsetZ <= columnMinZ)
                        continue;

                    top = Math.max(top, item.getMaxY());
                }
            }

        return top;
    }

    // Query \\

    private void beginQuery(WorldHandle world, long frameChunk) {

        queryStamp++;
        queryComplete = true;
        candidates.clear();
        candidateOffsetX.clear();
        candidateOffsetZ.clear();
        openOffsetX.clear();
        openOffsetZ.clear();

        for (int i = 0; i < openContainers.size(); i++) {

            long chunkCoordinate = openContainers.get(i).getChunkCoordinate();

            openOffsetX.add(chunkSpan * (int) WorldWrapUtility.wrappedDeltaX(
                    world, Coordinate2Long.unpackX(chunkCoordinate), Coordinate2Long.unpackX(frameChunk)));
            openOffsetZ.add(chunkSpan * (int) WorldWrapUtility.wrappedDeltaZ(
                    world, Coordinate2Long.unpackY(chunkCoordinate), Coordinate2Long.unpackY(frameChunk)));
        }
    }

    private boolean gatherBounds(WorldHandle world, long frameChunk) {

        beginQuery(world, frameChunk);
        collect(
                world,
                frameChunk,
                Math.floorDiv(boundsMin[axisX], resolution),
                Math.floorDiv(boundsMin[axisY], resolution),
                Math.floorDiv(boundsMin[axisZ], resolution),
                Math.floorDiv(boundsMax[axisX] - 1, resolution),
                Math.floorDiv(boundsMax[axisY] - 1, resolution),
                Math.floorDiv(boundsMax[axisZ] - 1, resolution));

        return queryComplete;
    }

    // Every item able to reach a block in the range — its corner lies in that block or the one before on each axis
    private void collect(
            WorldHandle world,
            long frameChunk,
            int minBlockX,
            int minBlockY,
            int minBlockZ,
            int maxBlockX,
            int maxBlockY,
            int maxBlockZ) {

        int firstX = minBlockX - 1;
        int firstZ = minBlockZ - 1;
        int firstY = Math.max(minBlockY - 1, 0);
        int lastY = Math.min(maxBlockY, worldTopBlock - 1);

        if (firstY > lastY)
            return;

        for (int chunkOffsetZ = Math.floorDiv(firstZ, chunkSize);
                chunkOffsetZ <= Math.floorDiv(maxBlockZ, chunkSize);
                chunkOffsetZ++)
            for (int chunkOffsetX = Math.floorDiv(firstX, chunkSize);
                    chunkOffsetX <= Math.floorDiv(maxBlockX, chunkSize);
                    chunkOffsetX++) {

                WorldItemInstancePaletteHandle palette = findCommittedPalette(
                        world, frameChunk, chunkOffsetX, chunkOffsetZ);

                if (palette == null) {
                    queryComplete = false;
                    continue;
                }

                if (palette.isEmpty())
                    continue;

                int baseX = chunkOffsetX * chunkSize;
                int baseZ = chunkOffsetZ * chunkSize;
                int fromX = Math.max(firstX - baseX, 0);
                int toX = Math.min(maxBlockX - baseX, chunkSize - 1);
                int fromZ = Math.max(firstZ - baseZ, 0);
                int toZ = Math.min(maxBlockZ - baseZ, chunkSize - 1);

                for (int blockZ = fromZ; blockZ <= toZ; blockZ++)
                    for (int blockX = fromX; blockX <= toX; blockX++)
                        for (int blockY = firstY; blockY <= lastY; blockY++)
                            addCandidates(
                                    palette.getItemsAtBlock(blockX, blockY, blockZ),
                                    chunkOffsetX * chunkSpan,
                                    chunkOffsetZ * chunkSpan);
            }
    }

    private void addCandidates(ObjectArrayList<WorldItemInstance> items, int offsetX, int offsetZ) {

        if (items == null)
            return;

        for (int i = 0; i < items.size(); i++) {

            WorldItemInstance item = items.get(i);

            if (!item.visit(queryStamp))
                continue;

            candidates.add(item);
            candidateOffsetX.add(offsetX);
            candidateOffsetZ.add(offsetZ);
        }
    }

    // The live palette of the chunk at this offset from the frame, null until it is loaded and committed
    private WorldItemInstancePaletteHandle findCommittedPalette(
            WorldHandle world,
            long frameChunk,
            int chunkOffsetX,
            int chunkOffsetZ) {

        long chunkCoordinate = WorldWrapUtility.wrapAroundWorld(
                world, Coordinate2Long.add(frameChunk, chunkOffsetX, chunkOffsetZ));
        ChunkInstance chunk = worldStreamManager.getChunkInstance(chunkCoordinate);

        if (chunk == null || !chunk.getWorldItemInstancePaletteHandle().isCommitted())
            return null;

        return chunk.getWorldItemInstancePaletteHandle();
    }

    // Cells \\

    // True when a gathered item or an open container claims this cell of the frame chunk
    private boolean isCellTaken(int x, int y, int z, WorldItemInstance ignored) {

        for (int i = 0; i < candidates.size(); i++) {

            WorldItemInstance item = candidates.get(i);

            if (item != ignored && claimsCell(item, x - candidateOffsetX.getInt(i), y, z - candidateOffsetZ.getInt(i)))
                return true;
        }

        for (int i = 0; i < openContainers.size(); i++) {

            WorldItemInstance container = openContainers.get(i);

            if (container != ignored
                    && holdsClearanceCell(container, x - openOffsetX.getInt(i), y, z - openOffsetZ.getInt(i)))
                return true;
        }

        return false;
    }

    // True when the item's turned shape claims this cell of its own chunk
    private boolean claimsCell(WorldItemInstance item, int x, int y, int z) {

        if (x < item.getMinX() || y < item.getMinY() || z < item.getMinZ()
                || x >= item.getMaxX() || y >= item.getMaxY() || z >= item.getMaxZ())
            return false;

        long packed = item.getPackedPosition();
        int orientation = Coordinate4Long.unpackW(packed);
        int gridX = x - Coordinate4Long.unpackX(packed);
        int gridY = y - Coordinate4Long.unpackY(packed);
        int gridZ = z - Coordinate4Long.unpackZ(packed);

        return item.getItemDefinitionHandle().getShape().claimsGridCell(
                itemRotationBufferSystem.unrotateCell(orientation, axisX, gridX, gridY, gridZ),
                itemRotationBufferSystem.unrotateCell(orientation, axisY, gridX, gridY, gridZ),
                itemRotationBufferSystem.unrotateCell(orientation, axisZ, gridX, gridY, gridZ));
    }

    // True when the open container's turned lid clearance holds this cell of its own chunk
    private boolean holdsClearanceCell(WorldItemInstance container, int x, int y, int z) {

        long packed = container.getPackedPosition();
        int orientation = Coordinate4Long.unpackW(packed);
        int gridX = x - Coordinate4Long.unpackX(packed);
        int gridY = y - Coordinate4Long.unpackY(packed);
        int gridZ = z - Coordinate4Long.unpackZ(packed);

        return container.getItemDefinitionHandle().getContainerSpace().getLidClearance().holdsCell(
                itemRotationBufferSystem.unrotateCell(orientation, axisX, gridX, gridY, gridZ),
                itemRotationBufferSystem.unrotateCell(orientation, axisY, gridX, gridY, gridZ),
                itemRotationBufferSystem.unrotateCell(orientation, axisZ, gridX, gridY, gridZ));
    }

    // Solid \\

    // Reads once every sub-block the current bounds touch, so each shape cell checks a flag
    private void sampleSolidSubBlocks(long frameChunk) {

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {
            solidOrigin[axis] = Math.floorDiv(boundsMin[axis], subVoxelsPerSubBlock);
            solidSpan[axis] = Math.floorDiv(boundsMax[axis] - 1, subVoxelsPerSubBlock) - solidOrigin[axis] + 1;
        }

        for (int z = 0; z < solidSpan[axisZ]; z++)
            for (int y = 0; y < solidSpan[axisY]; y++)
                for (int x = 0; x < solidSpan[axisX]; x++)
                    solidScratch[toSolidIndex(x, y, z)] = SubBlockSampleUtility.isSolid(
                            worldStreamManager,
                            blockManager,
                            frameChunk,
                            solidOrigin[axisX] + x,
                            solidOrigin[axisY] + y,
                            solidOrigin[axisZ] + z);
    }

    private boolean isSampledSolid(int x, int y, int z) {
        return solidScratch[toSolidIndex(
                Math.floorDiv(x, subVoxelsPerSubBlock) - solidOrigin[axisX],
                Math.floorDiv(y, subVoxelsPerSubBlock) - solidOrigin[axisY],
                Math.floorDiv(z, subVoxelsPerSubBlock) - solidOrigin[axisZ])];
    }

    private int toSolidIndex(int x, int y, int z) {
        return x + solidSpan[axisX] * (y + solidSpan[axisY] * z);
    }

    private boolean isSolid(long frameChunk, int x, int y, int z) {
        return SubBlockSampleUtility.isSolid(
                worldStreamManager,
                blockManager,
                frameChunk,
                Math.floorDiv(x, subVoxelsPerSubBlock),
                Math.floorDiv(y, subVoxelsPerSubBlock),
                Math.floorDiv(z, subVoxelsPerSubBlock));
    }
}
