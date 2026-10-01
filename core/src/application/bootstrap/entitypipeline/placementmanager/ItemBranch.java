package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.worlditem.WorldItemCastStruct;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemPlacementSystem;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;

class ItemBranch extends BranchPackage {

    /*
     * World item placement and pickup for PlacementManager. Placement sets the
     * main-hand item against the face the ray met — a block's, a sub-block's
     * or another item's, containers included — oriented by the camera, and
     * leaves it in hand when no spot by that face is free; pickup hands the
     * world item's real item, contents included, to the entity and removes it
     * from the world only once it fits.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private WorldItemPlacementSystem worldItemPlacementSystem;

    // Settings
    private int chunkSize;
    private int subVoxelResolution;
    private int subVoxelsPerSubBlock;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.subVoxelResolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        this.subVoxelsPerSubBlock = subVoxelResolution / SubBlockUtility.DIVISIONS;
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
    }

    // Place \\

    boolean place(EntityInstance entity, Vector3 direction, BlockCastStruct castStruct) {

        Direction3Vector hitFace = castStruct.getHitFace();
        int hitOctant = castStruct.getHitOctant();
        int cellY = castStruct.getSubChunkY() * chunkSize + castStruct.getBlockY();

        return placeAgainst(
                entity,
                direction,
                castStruct.getChunkCoordinate(),
                resolveAnchor(castStruct.getBlockX(), SubBlockUtility.getOctantX(hitOctant),
                        castStruct.getHitSubX(), hitFace.x),
                resolveAnchor(cellY, SubBlockUtility.getOctantY(hitOctant),
                        castStruct.getHitSubY(), hitFace.y),
                resolveAnchor(castStruct.getBlockZ(), SubBlockUtility.getOctantZ(hitOctant),
                        castStruct.getHitSubZ(), hitFace.z),
                hitFace);
    }

    boolean placeOnItem(EntityInstance entity, Vector3 direction, WorldItemCastStruct itemCastStruct) {
        return placeAgainst(
                entity,
                direction,
                itemCastStruct.getChunkCoordinate(),
                itemCastStruct.getAnchorX(),
                itemCastStruct.getAnchorY(),
                itemCastStruct.getAnchorZ(),
                itemCastStruct.getHitFace());
    }

    // The main-hand item leaves the hand only once the world has taken it
    private boolean placeAgainst(
            EntityInstance entity,
            Vector3 direction,
            long frameChunk,
            int anchorX,
            int anchorY,
            int anchorZ,
            Direction3Vector hitFace) {

        InventoryHandle inventoryHandle = entity.getInventoryHandle();

        if (!inventoryHandle.hasMainHand())
            return false;

        WorldItemInstance placed = worldItemPlacementSystem.placeItem(
                entity.getWorldHandle(),
                frameChunk,
                anchorX,
                anchorY,
                anchorZ,
                hitFace,
                resolveItemOrientation(hitFace, direction),
                inventoryHandle.getMainHand());

        if (placed == null)
            return false;

        inventoryHandle.unequip(EquipmentSlot.MAIN_HAND);

        return true;
    }

    // The sub-voxel just outside the hit sub-block's face along that face's axis, or the hit sub-voxel across it
    private int resolveAnchor(int cell, int octantAxis, int hitSub, int faceComponent) {

        int cellStart = cell * subVoxelResolution;

        if (faceComponent > 0)
            return cellStart + (octantAxis + 1) * subVoxelsPerSubBlock;

        if (faceComponent < 0)
            return cellStart + octantAxis * subVoxelsPerSubBlock - 1;

        return cellStart + hitSub;
    }

    // Pick Up \\

    boolean pickUp(EntityInstance entity, WorldItemInstance worldItemInstance) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(worldItemInstance.getChunkCoordinate());

        if (chunk == null)
            return false;

        ItemInstance itemInstance = worldItemPlacementSystem.resolveItemInstance(worldItemInstance);

        if (!entity.getInventoryHandle().give(itemInstance))
            return false;

        worldItemPlacementSystem.removeItem(chunk, worldItemInstance);

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