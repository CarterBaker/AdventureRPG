package com.internal.bootstrap.entitypipeline.placementmanager.placement;

import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.bootstrap.physicspipeline.util.BlockCastStruct;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.Extras.Coordinate4Long;
import com.internal.core.util.mathematics.Extras.Direction3Vector;
import com.internal.core.util.mathematics.vectors.Vector3;

public class ItemBranch extends BranchPackage {

    // Internal
    private ChunkStreamManager chunkStreamManager;
    private WorldItemPlacementSystem worldItemPlacementSystem;

    private int CHUNK_SIZE;

    // Internal \\

    @Override
    protected void create() {
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
    }

    // Place \\

    /*
     * Returns true if placement succeeded (caller should reset placement timer).
     */
    public boolean place(EntityHandle entity, Vector3 direction, BlockCastStruct castStruct) {

        if (entity.getInventoryHandle().getBackpack().isEmpty())
            return false;

        int placeX = castStruct.blockX + castStruct.hitFace.x;
        int placeY = castStruct.blockY + castStruct.hitFace.y;
        int placeZ = castStruct.blockZ + castStruct.hitFace.z;
        int placeSubChunkY = castStruct.subChunkY;

        int placeChunkX = Coordinate2Long.unpackX(castStruct.chunkCoordinate);
        int placeChunkZ = Coordinate2Long.unpackY(castStruct.chunkCoordinate);

        if (placeX < 0) {
            placeChunkX--;
            placeX += CHUNK_SIZE;
        } else if (placeX >= CHUNK_SIZE) {
            placeChunkX++;
            placeX -= CHUNK_SIZE;
        }

        if (placeZ < 0) {
            placeChunkZ--;
            placeZ += CHUNK_SIZE;
        } else if (placeZ >= CHUNK_SIZE) {
            placeChunkZ++;
            placeZ -= CHUNK_SIZE;
        }

        if (placeY < 0) {
            placeSubChunkY--;
            placeY += CHUNK_SIZE;
        } else if (placeY >= CHUNK_SIZE) {
            placeSubChunkY++;
            placeY -= CHUNK_SIZE;
        }

        long placeChunkCoord = Coordinate2Long.pack(placeChunkX, placeChunkZ);
        ChunkInstance placeChunk = chunkStreamManager.getChunkInstance(placeChunkCoord);
        if (placeChunk == null)
            return false;

        Direction3Vector hitFaceDir = Direction3Vector.getDirection(
                castStruct.hitFace.x, castStruct.hitFace.y, castStruct.hitFace.z);
        int rotation = resolveItemOrientation(hitFaceDir, direction);

        int chunkLocalY = placeSubChunkY * CHUNK_SIZE + placeY;

        int SVR = EngineSetting.SUB_VOXEL_RESOLUTION;
        Direction3Vector face = castStruct.hitFace;

        int subX = placeX * SVR + (face.x != 0 ? (face.x > 0 ? 0 : SVR - 1) : castStruct.hitSubX);
        int subY = chunkLocalY * SVR + (face.y != 0 ? (face.y > 0 ? 0 : SVR - 1) : castStruct.hitSubY);
        int subZ = placeZ * SVR + (face.z != 0 ? (face.z > 0 ? 0 : SVR - 1) : castStruct.hitSubZ);

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