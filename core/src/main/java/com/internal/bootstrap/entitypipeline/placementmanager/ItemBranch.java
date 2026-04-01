package com.internal.bootstrap.entitypipeline.placementmanager;

import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.bootstrap.physicspipeline.util.BlockCastStruct;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.internal.core.engine.BranchPackage;
import com.internal.core.settings.EngineSetting;
import com.internal.core.util.mathematics.extras.Coordinate2Long;
import com.internal.core.util.mathematics.extras.Coordinate4Long;
import com.internal.core.util.mathematics.extras.Direction3Vector;
import com.internal.core.util.mathematics.vectors.Vector3;

class ItemBranch extends BranchPackage {

    /*
     * Handles world item placement for PlacementManager. Resolves the target
     * block face, computes sub-voxel placement position, determines item
     * orientation from camera direction, and delegates to WorldItemPlacementSystem.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private WorldItemPlacementSystem worldItemPlacementSystem;

    // Settings
    private int chunkSize;
    private int subVoxelResolution;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.subVoxelResolution = EngineSetting.SUB_VOXEL_RESOLUTION;
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
    }

    // Place \\

    boolean place(EntityInstance entity, Vector3 direction, BlockCastStruct castStruct) {

        if (entity.getInventoryHandle().getBackpack().isEmpty())
            return false;

        Direction3Vector hitFace = castStruct.getHitFace();

        int placeX = castStruct.getBlockX() + hitFace.x;
        int placeY = castStruct.getBlockY() + hitFace.y;
        int placeZ = castStruct.getBlockZ() + hitFace.z;
        int placeSubChunkY = castStruct.getSubChunkY();

        int placeChunkX = Coordinate2Long.unpackX(castStruct.getChunkCoordinate());
        int placeChunkZ = Coordinate2Long.unpackY(castStruct.getChunkCoordinate());

        if (placeX < 0) {
            placeChunkX--;
            placeX += chunkSize;
        } else if (placeX >= chunkSize) {
            placeChunkX++;
            placeX -= chunkSize;
        }

        if (placeZ < 0) {
            placeChunkZ--;
            placeZ += chunkSize;
        } else if (placeZ >= chunkSize) {
            placeChunkZ++;
            placeZ -= chunkSize;
        }

        if (placeY < 0) {
            placeSubChunkY--;
            placeY += chunkSize;
        } else if (placeY >= chunkSize) {
            placeSubChunkY++;
            placeY -= chunkSize;
        }

        long placeChunkCoord = Coordinate2Long.pack(placeChunkX, placeChunkZ);
        ChunkInstance placeChunk = worldStreamManager.getChunkInstance(placeChunkCoord);

        if (placeChunk == null)
            return false;

        Direction3Vector hitFaceDir = Direction3Vector.getDirection(hitFace.x, hitFace.y, hitFace.z);
        int rotation = resolveItemOrientation(hitFaceDir, direction);
        int chunkLocalY = placeSubChunkY * chunkSize + placeY;

        int subX = placeX * subVoxelResolution
                + (hitFace.x != 0 ? (hitFace.x > 0 ? 0 : subVoxelResolution - 1) : castStruct.getHitSubX());
        int subY = chunkLocalY * subVoxelResolution
                + (hitFace.y != 0 ? (hitFace.y > 0 ? 0 : subVoxelResolution - 1) : castStruct.getHitSubY());
        int subZ = placeZ * subVoxelResolution
                + (hitFace.z != 0 ? (hitFace.z > 0 ? 0 : subVoxelResolution - 1) : castStruct.getHitSubZ());

        ItemDefinitionHandle def = entity.getInventoryHandle().getBackpack().getItems().get(0);
        int packedItem = def.getItemID();
        long packedPosition = Coordinate4Long.pack(subX, subY, subZ, rotation);

        worldItemPlacementSystem.placeItem(placeChunk, placeSubChunkY, packedPosition, packedItem, def);

        return true;
    }

    // Orientation \\

    private int resolveItemOrientation(Direction3Vector hitFace, Vector3 cameraDirection) {

        Direction3Vector facing;

        if (hitFace == Direction3Vector.UP || hitFace == Direction3Vector.DOWN)
            facing = Direction3Vector.VALUES[EngineSetting.DEFAULT_BLOCK_DIRECTION];
        else
            facing = hitFace;

        int spin = 0;

        if (facing == Direction3Vector.UP || facing == Direction3Vector.DOWN) {

            float ax = Math.abs(cameraDirection.x);
            float az = Math.abs(cameraDirection.z);

            if (ax >= az)
                spin = cameraDirection.x > 0 ? 1 : 3;
            else
                spin = cameraDirection.z > 0 ? 0 : 2;
        }

        return facing.ordinal() * 4 + spin;
    }
}