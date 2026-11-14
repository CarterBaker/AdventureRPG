package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.physicspipeline.physicsnoisemanager.PhysicsNoiseManager;
import application.bootstrap.physicspipeline.util.SubBlockSampleUtility;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

public class NaturalGroundOffsetBranch extends BranchPackage {

    /*
     * Tracks a smoothed vertical offset for where a natural block's top is
     * drawn under an entity's feet: the near-ring detail riding the face
     * normal plus the edge warp's rise, read from the same lattice the
     * tessellation stages displace natural terrain with. It is never written
     * into WorldPositionStruct, so collision and gravity stay on the flat
     * position; the body and the eye are both drawn on it, so the entity
     * stands, and looks out from, the ground it is seen on. Stair-step lifts
     * ease out through the same smoothing.
     */

    // Internal
    private BlockManager blockManager;
    private WorldStreamManager worldStreamManager;
    private PhysicsNoiseManager physicsNoiseManager;

    // Scratch
    private Vector3 detailScratch;
    private Vector3 warpScratch;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.detailScratch = new Vector3();
        this.warpScratch = new Vector3();
    }

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.physicsNoiseManager = get(PhysicsNoiseManager.class);
    }

    // Update \\

    void update(EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        float target = state.isGrounded() ? resolveTargetOffset(entity) : 0f;

        float current = state.getGroundOffset();
        float smoothing = Math.min(1f, internal.getDeltaTime() * EngineSetting.NATURAL_GROUND_OFFSET_SMOOTHING);

        state.setGroundOffset(current + (target - current) * smoothing);
    }

    private float resolveTargetOffset(EntityInstance entity) {

        Vector3 position = entity.getWorldPositionStruct().getPosition();
        long chunkCoordinate = entity.getWorldPositionStruct().getChunkCoordinate();

        Vector3 size = entity.getSize();
        float centerX = position.x + size.x * 0.5f;
        float centerZ = position.z + size.z * 0.5f;

        BlockHandle below = SubBlockSampleUtility.getSubBlockAt(
                worldStreamManager,
                blockManager,
                chunkCoordinate,
                SubBlockSampleUtility.toSub(centerX),
                SubBlockSampleUtility.toSub(position.y - SubBlockUtility.SIZE * 0.5f),
                SubBlockSampleUtility.toSub(centerZ));

        if (below == null || !below.isNatural() || below.getGeometry() != DynamicGeometryType.FULL)
            return 0f;

        physicsNoiseManager.sampleFields(chunkCoordinate, centerX, position.y, centerZ, detailScratch, warpScratch);

        return detailScratch.y * EngineSetting.NATURAL_DETAIL_NORMAL_AMPLITUDE_BLOCKS + warpScratch.y;
    }
}