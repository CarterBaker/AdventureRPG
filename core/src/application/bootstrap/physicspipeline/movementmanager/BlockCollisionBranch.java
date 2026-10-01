package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.physicspipeline.util.SubBlockSampleUtility;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemSpaceSystem;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class BlockCollisionBranch extends BranchPackage {

    /*
     * Sweeps an entity's box through the sub-block grid one axis at a time and
     * clamps each axis at the first solid face, keeping a thin skin. The rough
     * boxes of solid world items near the move are gathered once and clamp
     * the same sweep, so a pile stops an entity and items stack into walls and
     * floors. Sub-blocks and item boxes already overlapped never block. A
     * grounded entity cut short tries a stair step and eases the lift into the
     * cosmetic ground offset, so sub-block terrain and low items walk like
     * stairs.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private WorldItemSpaceSystem worldItemSpaceSystem;

    // Settings
    private float skin;
    private float stepHeight;
    private float subBlockSize;
    private int axisX;
    private int axisY;
    private int axisZ;

    // Box — min and max corner, indexed by axis
    private float[] boxMin;
    private float[] boxMax;

    // Scratch — sub-block coordinate, indexed by axis
    private int[] subScratch;

    // Item Boxes — min and max corner of each solid item box near the move, six floats each
    private FloatArrayList itemBoxes;
    private int boxStride;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.skin = EngineSetting.COLLISION_SKIN_BLOCKS;
        this.stepHeight = EngineSetting.STEP_UP_HEIGHT_BLOCKS;
        this.subBlockSize = SubBlockUtility.SIZE;
        this.axisX = EngineSetting.AXIS_X;
        this.axisY = EngineSetting.AXIS_Y;
        this.axisZ = EngineSetting.AXIS_Z;

        // Box
        this.boxMin = new float[EngineSetting.AXIS_COUNT];
        this.boxMax = new float[EngineSetting.AXIS_COUNT];

        // Scratch
        this.subScratch = new int[EngineSetting.AXIS_COUNT];

        // Item Boxes
        this.itemBoxes = new FloatArrayList();
        this.boxStride = EngineSetting.AXIS_COUNT * 2;
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.worldItemSpaceSystem = get(WorldItemSpaceSystem.class);
    }

    // Collision \\

    void calculate(Vector3 position, Vector3 movement, EntityInstance entity) {

        long chunkCoordinate = entity.getWorldPositionStruct().getChunkCoordinate();
        Vector3 size = entity.getSize();

        setBox(position, size);
        gatherItemBoxes(entity, chunkCoordinate, movement);

        float moveY = sweep(chunkCoordinate, axisY, movement.y);
        translate(axisY, moveY);

        float moveX = sweep(chunkCoordinate, axisX, movement.x);
        translate(axisX, moveX);

        float moveZ = sweep(chunkCoordinate, axisZ, movement.z);
        translate(axisZ, moveZ);

        boolean horizontallyBlocked = moveX != movement.x || moveZ != movement.z;

        if (horizontallyBlocked && entity.getEntityStateHandle().isGrounded()) {
            stepUp(chunkCoordinate, movement, moveX, moveY, moveZ, entity);
            return;
        }

        movement.set(moveX, moveY, moveZ);
    }

    // Step \\

    private void stepUp(
            long chunkCoordinate,
            Vector3 movement,
            float moveX, float moveY, float moveZ,
            EntityInstance entity) {

        float intendedX = movement.x;
        float intendedZ = movement.z;

        translate(axisX, -moveX);
        translate(axisZ, -moveZ);

        float rise = sweep(chunkCoordinate, axisY, stepHeight);
        translate(axisY, rise);

        float stepX = sweep(chunkCoordinate, axisX, intendedX);
        translate(axisX, stepX);

        float stepZ = sweep(chunkCoordinate, axisZ, intendedZ);
        translate(axisZ, stepZ);

        float settle = sweep(chunkCoordinate, axisY, -rise);
        float lift = rise + settle;

        boolean climbed = lift > skin
                && horizontalDistance(stepX, stepZ) > horizontalDistance(moveX, moveZ) + skin;

        if (!climbed) {
            movement.set(moveX, moveY, moveZ);
            return;
        }

        movement.set(stepX, moveY + lift, stepZ);

        EntityStateHandle state = entity.getEntityStateHandle();
        state.setGroundOffset(state.getGroundOffset() - lift);
    }

    private float horizontalDistance(float x, float z) {
        return (float) Math.sqrt(x * x + z * z);
    }

    // Item Boxes \\

    // Every solid item box the move or a stair step could reach, gathered once for all its sweeps
    private void gatherItemBoxes(EntityInstance entity, long chunkCoordinate, Vector3 movement) {

        worldItemSpaceSystem.collectSolidBoxes(
                entity.getWorldHandle(),
                chunkCoordinate,
                boxMin[axisX] + Math.min(movement.x, 0f) - skin,
                boxMin[axisY] + Math.min(movement.y, 0f) - skin,
                boxMin[axisZ] + Math.min(movement.z, 0f) - skin,
                boxMax[axisX] + Math.max(movement.x, 0f) + skin,
                boxMax[axisY] + Math.max(movement.y, 0f) + stepHeight + skin,
                boxMax[axisZ] + Math.max(movement.z, 0f) + skin,
                itemBoxes);
    }

    // The distance left once the first item box ahead along the axis stops it
    private float sweepItems(int axis, float distance) {

        if (distance == 0f || itemBoxes.isEmpty())
            return distance;

        int tangentA = (axis + 1) % EngineSetting.AXIS_COUNT;
        int tangentB = (axis + 2) % EngineSetting.AXIS_COUNT;
        float[] boxes = itemBoxes.elements();

        for (int box = 0; box < itemBoxes.size(); box += boxStride) {

            int min = box;
            int max = box + EngineSetting.AXIS_COUNT;

            if (boxes[max + tangentA] <= boxMin[tangentA] + skin || boxes[min + tangentA] >= boxMax[tangentA] - skin)
                continue;

            if (boxes[max + tangentB] <= boxMin[tangentB] + skin || boxes[min + tangentB] >= boxMax[tangentB] - skin)
                continue;

            if (distance > 0f) {

                float face = boxes[min + axis];
                float lead = boxMax[axis];

                if (face >= lead - skin)
                    distance = Math.min(distance, Math.max(face - lead - skin, 0f));

                continue;
            }

            float face = boxes[max + axis];
            float lead = boxMin[axis];

            if (face <= lead + skin)
                distance = Math.max(distance, -Math.max(lead - face - skin, 0f));
        }

        return distance;
    }

    // Sweep \\

    private float sweep(long chunkCoordinate, int axis, float distance) {
        return sweepItems(axis, sweepBlocks(chunkCoordinate, axis, distance));
    }

    private float sweepBlocks(long chunkCoordinate, int axis, float distance) {

        if (distance == 0f)
            return 0f;

        int tangentA = (axis + 1) % EngineSetting.AXIS_COUNT;
        int tangentB = (axis + 2) % EngineSetting.AXIS_COUNT;

        int firstA = SubBlockSampleUtility.toSub(boxMin[tangentA] + skin);
        int lastA = SubBlockSampleUtility.toSub(boxMax[tangentA] - skin);
        int firstB = SubBlockSampleUtility.toSub(boxMin[tangentB] + skin);
        int lastB = SubBlockSampleUtility.toSub(boxMax[tangentB] - skin);

        if (distance > 0f) {

            float lead = boxMax[axis];
            int lastLayer = SubBlockSampleUtility.toSub(lead + distance);

            for (int layer = SubBlockSampleUtility.toSub(lead - skin); layer <= lastLayer; layer++) {

                float face = layer * subBlockSize;

                if (face < lead - skin)
                    continue;

                if (isLayerSolid(chunkCoordinate, axis, layer, tangentA, firstA, lastA, tangentB, firstB, lastB))
                    return Math.min(distance, Math.max(face - lead - skin, 0f));
            }

            return distance;
        }

        float lead = boxMin[axis];
        int lastLayer = SubBlockSampleUtility.toSub(lead + distance);

        for (int layer = SubBlockSampleUtility.toSub(lead + skin); layer >= lastLayer; layer--) {

            float face = (layer + 1) * subBlockSize;

            if (face > lead + skin)
                continue;

            if (isLayerSolid(chunkCoordinate, axis, layer, tangentA, firstA, lastA, tangentB, firstB, lastB))
                return Math.max(distance, -Math.max(lead - face - skin, 0f));
        }

        return distance;
    }

    private boolean isLayerSolid(
            long chunkCoordinate,
            int axis, int layer,
            int tangentA, int firstA, int lastA,
            int tangentB, int firstB, int lastB) {

        subScratch[axis] = layer;

        for (int a = firstA; a <= lastA; a++) {
            for (int b = firstB; b <= lastB; b++) {

                subScratch[tangentA] = a;
                subScratch[tangentB] = b;

                if (SubBlockSampleUtility.isSolid(
                        worldStreamManager,
                        blockManager,
                        chunkCoordinate,
                        subScratch[axisX], subScratch[axisY], subScratch[axisZ]))
                    return true;
            }
        }

        return false;
    }

    // Box \\

    private void setBox(Vector3 position, Vector3 size) {

        boxMin[axisX] = position.x;
        boxMin[axisY] = position.y;
        boxMin[axisZ] = position.z;

        boxMax[axisX] = position.x + size.x;
        boxMax[axisY] = position.y + size.y;
        boxMax[axisZ] = position.z + size.z;
    }

    private void translate(int axis, float distance) {
        boxMin[axis] += distance;
        boxMax[axis] += distance;
    }
}
