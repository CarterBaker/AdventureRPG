package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate4Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;

class ItemBranch extends BranchPackage {

    /*
     * Handles world item placement and pickup for PlacementManager. Placement
     * sets down whatever the entity holds in its main hand: it resolves the
     * target block face, computes sub-voxel placement position, determines
     * item orientation from camera direction, and delegates to
     * WorldItemPlacementSystem. The face hit is the face of the sub-block the
     * ray met, so an item set on a half-block slab rests on the slab rather
     * than on the empty half above it, which stays in the same cell. Pickup
     * hands the world item's real item — a chest with everything in it — to
     * the entity's inventory, and only takes it out of the world once the
     * inventory has made room for it.
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

        InventoryHandle inventoryHandle = entity.getInventoryHandle();

        if (!inventoryHandle.hasMainHand())
            return false;

        Direction3Vector hitFace = castStruct.getHitFace();
        int hitOctant = castStruct.getHitOctant();
        int targetOctant = SubBlockUtility.stepOctant(hitOctant, hitFace);
        int cellStep = SubBlockUtility.leavesCell(hitOctant, hitFace) ? 1 : 0;

        int placeX = castStruct.getBlockX() + hitFace.x * cellStep;
        int placeY = castStruct.getBlockY() + hitFace.y * cellStep;
        int placeZ = castStruct.getBlockZ() + hitFace.z * cellStep;
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

        int subX = placeX * subVoxelResolution + (hitFace.x != 0
                ? resolveFaceSubVoxel(SubBlockUtility.getOctantX(targetOctant), hitFace.x)
                : castStruct.getHitSubX());
        int subY = chunkLocalY * subVoxelResolution + (hitFace.y != 0
                ? resolveFaceSubVoxel(SubBlockUtility.getOctantY(targetOctant), hitFace.y)
                : castStruct.getHitSubY());
        int subZ = placeZ * subVoxelResolution + (hitFace.z != 0
                ? resolveFaceSubVoxel(SubBlockUtility.getOctantZ(targetOctant), hitFace.z)
                : castStruct.getHitSubZ());

        long packedPosition = Coordinate4Long.pack(subX, subY, subZ, rotation);
        ItemInstance itemInstance = inventoryHandle.unequip(EquipmentSlot.MAIN_HAND);

        worldItemPlacementSystem.placeItem(placeChunk, placeSubChunkY, packedPosition, itemInstance);

        return true;
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

    // The sub-voxel of the target octant touching the face that was hit, along that face's axis
    private int resolveFaceSubVoxel(int targetOctantAxis, int faceComponent) {
        return targetOctantAxis * subVoxelsPerSubBlock + (faceComponent > 0 ? 0 : subVoxelsPerSubBlock - 1);
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