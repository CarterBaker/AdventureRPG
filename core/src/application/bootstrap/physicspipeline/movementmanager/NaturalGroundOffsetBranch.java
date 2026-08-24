package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.physicspipeline.physicsnoisemanager.PhysicsNoiseManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.util.LiquidColumnUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

public class NaturalGroundOffsetBranch extends BranchPackage {

    /*
     * Tracks a smoothed, purely cosmetic vertical offset representing where
     * a natural block's own jittered top surface sits under an entity's
     * feet, resampled every frame at the entity's footprint center. Never
     * written back into WorldPositionStruct — collision, gravity, and block
     * composition keep operating on the flat, jitter-free position exactly
     * as before, so this can never reopen a tunneling path. Consumers such
     * as the camera add the result on top of the flat position themselves.
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

        ChunkInstance chunk = worldStreamManager.getChunkInstance(chunkCoordinate);

        if (chunk == null)
            return 0f;

        int blockX = (int) Math.floor(position.x);
        int blockZ = (int) Math.floor(position.z);
        int belowTotalY = (int) Math.floor(position.y) - 1;

        BlockHandle below = LiquidColumnUtility.getBlockAt(chunk, blockManager, blockX, belowTotalY, blockZ);

        if (below == null || !below.isNatural() || below.getGeometry() != DynamicGeometryType.FULL)
            return 0f;

        Vector3 size = entity.getSize();
        float centerX = position.x + size.x * 0.5f;
        float centerZ = position.z + size.z * 0.5f;

        return physicsNoiseManager.sampleAxisJitter(centerX, centerZ, EngineSetting.AXIS_Y);
    }
}