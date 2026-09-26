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
     * Tracks a smoothed, purely cosmetic vertical offset for where a natural
     * block's jittered top sits under an entity's feet. It is never written
     * into WorldPositionStruct, so collision and gravity stay on the flat
     * position; the camera and other consumers add it themselves. Stair-step
     * lifts ease out through the same smoothing.
     */

    // Internal
    private BlockManager blockManager;
    private WorldStreamManager worldStreamManager;
    private PhysicsNoiseManager physicsNoiseManager;

    // Internal \\

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

        return physicsNoiseManager.sampleAxisJitter(centerX, centerZ, EngineSetting.AXIS_Y);
    }
}