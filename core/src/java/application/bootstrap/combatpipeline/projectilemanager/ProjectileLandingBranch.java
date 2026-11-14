package application.bootstrap.combatpipeline.projectilemanager;

import application.bootstrap.combatpipeline.projectile.ProjectileInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.itemrotationmanager.ItemRotationBufferSystem;
import application.bootstrap.physicspipeline.util.SubBlockSampleUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleCargoSystem;
import application.bootstrap.worldpipeline.blockmanager.BlockPlacementSystem;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worlditem.WorldItemPlacementStruct;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemPlacementSystem;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemSpaceSystem;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;

class ProjectileLandingBranch extends BranchPackage {

    /*
     * Hands a resting projectile back to the world, snapped to the sub-voxel
     * grid. A block piece becomes a sub-block of its block in the nearest
     * octant that can take it — the one it rests in, then the ones around it,
     * climbing a layer at a time — so it only ever exists as a sub-block in
     * the world. Any other item becomes a world item turned to the orientation
     * nearest its tumble, its shape centred where it came to rest and its
     * lowest point set on the ground, raised onto whatever items already lie
     * there so it never sinks into a pile. Anything resting on a vehicle's
     * deck — a block piece too, since a vehicle has no blocks to build on —
     * becomes cargo the same way, turned and set in the vehicle's own frame,
     * and takes flight again when the vehicle has no room for it. settle() is
     * false while there is nowhere to put it yet.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockPlacementSystem blockPlacementSystem;
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private WorldItemSpaceSystem worldItemSpaceSystem;
    private ItemRotationBufferSystem itemRotationBufferSystem;
    private VehicleCargoSystem vehicleCargoSystem;

    // Settings
    private int chunkSize;
    private int subVoxelResolution;
    private int worldTopCell;

    // Scratch
    private Matrix4 rotationScratch;
    private Vector3 cornerScratch;
    private Quaternion tumbleScratch;
    private WorldItemPlacementStruct placementStruct;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.subVoxelResolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        this.worldTopCell = EngineSetting.WORLD_HEIGHT * chunkSize;

        // Scratch
        this.rotationScratch = new Matrix4();
        this.cornerScratch = new Vector3();
        this.tumbleScratch = new Quaternion();
        this.placementStruct = new WorldItemPlacementStruct();
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockPlacementSystem = get(BlockPlacementSystem.class);
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
        this.worldItemSpaceSystem = get(WorldItemSpaceSystem.class);
        this.itemRotationBufferSystem = get(ItemRotationBufferSystem.class);
        this.vehicleCargoSystem = get(VehicleCargoSystem.class);
    }

    // Settle \\

    boolean settle(ProjectileInstance projectile) {

        if (projectile.isRestingOnVehicle())
            return settleOnVehicle(projectile);

        if (projectile.getItemInstance().getItemDefinitionHandle().isBlockPiece())
            return settlePiece(projectile);

        return settleItem(projectile);
    }

    // Piece \\

    private boolean settlePiece(ProjectileInstance projectile) {

        Vector3 position = projectile.getWorldPositionStruct().getPosition();
        int subX = SubBlockSampleUtility.toSub(position.x);
        int subY = SubBlockSampleUtility.toSub(projectile.getGroundY() + SubBlockUtility.SIZE * 0.5f);
        int subZ = SubBlockSampleUtility.toSub(position.z);

        for (int climb = 0; climb < EngineSetting.PROJECTILE_PIECE_CLIMB_LIMIT; climb++)
            if (placePieceAround(projectile, subX, subY + climb, subZ))
                return true;

        return false;
    }

    // The octant itself first, then its edge neighbours, then its corner neighbours
    private boolean placePieceAround(ProjectileInstance projectile, int subX, int subY, int subZ) {

        for (int ring = 0; ring <= EngineSetting.PROJECTILE_PIECE_RING_LIMIT; ring++)
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++)
                for (int offsetX = -1; offsetX <= 1; offsetX++) {

                    if (Math.abs(offsetX) + Math.abs(offsetZ) != ring)
                        continue;

                    if (placePiece(projectile, subX + offsetX, subY, subZ + offsetZ))
                        return true;
                }

        return false;
    }

    private boolean placePiece(ProjectileInstance projectile, int subX, int subY, int subZ) {

        int cellX = Math.floorDiv(subX, SubBlockUtility.DIVISIONS);
        int cellY = Math.floorDiv(subY, SubBlockUtility.DIVISIONS);
        int cellZ = Math.floorDiv(subZ, SubBlockUtility.DIVISIONS);

        if (cellY < 0 || cellY >= worldTopCell)
            return false;

        ChunkInstance chunk = resolveChunk(
                projectile,
                Math.floorDiv(cellX, chunkSize),
                Math.floorDiv(cellZ, chunkSize));

        if (chunk == null)
            return false;

        int packedXYZ = Coordinate3Int.pack(
                Math.floorMod(cellX, chunkSize),
                cellY % chunkSize,
                Math.floorMod(cellZ, chunkSize));
        int octant = SubBlockUtility.getOctant(
                Math.floorMod(subX, SubBlockUtility.DIVISIONS),
                Math.floorMod(subY, SubBlockUtility.DIVISIONS),
                Math.floorMod(subZ, SubBlockUtility.DIVISIONS));

        return blockPlacementSystem.placeSubBlock(
                chunk,
                cellY / chunkSize,
                packedXYZ,
                octant,
                projectile.getItemInstance().getItemDefinitionHandle().getBlockID());
    }

    // Item \\

    private boolean settleItem(ProjectileInstance projectile) {

        ItemInstance itemInstance = projectile.getItemInstance();
        ItemShapeStruct shape = itemInstance.getItemDefinitionHandle().getShape();
        Vector3 position = projectile.getWorldPositionStruct().getPosition();

        int orientation = itemRotationBufferSystem.findNearestOrientation(
                projectile.getOrientation().toMatrix(rotationScratch));
        Matrix4 rotation = itemRotationBufferSystem.getRotation(orientation);

        rotateAboutCentre(
                rotation,
                (shape.getOffsetX() + shape.getSizeX() * 0.5f) / subVoxelResolution,
                (shape.getOffsetY() + shape.getSizeY() * 0.5f) / subVoxelResolution,
                (shape.getOffsetZ() + shape.getSizeZ() * 0.5f) / subVoxelResolution);

        int subX = Math.round((position.x - cornerScratch.x) * subVoxelResolution);
        int subZ = Math.round((position.z - cornerScratch.z) * subVoxelResolution);
        int subY = (int) Math.ceil((projectile.getGroundY() - resolveLowestPoint(rotation, shape))
                * subVoxelResolution - EngineSetting.PROJECTILE_SNAP_EPSILON);

        boolean resolved = worldItemSpaceSystem.resolveCornerPlacement(
                projectile.getWorldHandle(),
                projectile.getWorldPositionStruct().getChunkCoordinate(),
                itemInstance.getItemDefinitionHandle(),
                orientation,
                subX,
                subY,
                subZ,
                Direction3Vector.UP,
                placementStruct);

        if (!resolved)
            return false;

        worldItemPlacementSystem.placeItem(placementStruct, itemInstance);

        return true;
    }

    // Vehicle \\

    // Cargo turned to the orientation nearest its tumble as the vehicle sees it, its shape centred over the point it
    // rests on and its lowest point set on the deck — or back in flight, when the vehicle has no room for it
    private boolean settleOnVehicle(ProjectileInstance projectile) {

        VehicleInstance vehicle = projectile.getRestVehicle();
        ItemInstance itemInstance = projectile.getItemInstance();
        ItemShapeStruct shape = itemInstance.getItemDefinitionHandle().getShape();
        Vector3 rest = projectile.getRestModelPoint();

        tumbleScratch.set(vehicle.getOrientation()).conjugate().multiply(projectile.getOrientation());

        int orientation = itemRotationBufferSystem.findNearestOrientation(tumbleScratch.toMatrix(rotationScratch));
        Matrix4 rotation = itemRotationBufferSystem.getRotation(orientation);

        rotateAboutCentre(
                rotation,
                (shape.getOffsetX() + shape.getSizeX() * 0.5f) / subVoxelResolution,
                (shape.getOffsetY() + shape.getSizeY() * 0.5f) / subVoxelResolution,
                (shape.getOffsetZ() + shape.getSizeZ() * 0.5f) / subVoxelResolution);

        int subX = Math.round((rest.x - cornerScratch.x) * subVoxelResolution);
        int subZ = Math.round((rest.z - cornerScratch.z) * subVoxelResolution);
        int subY = (int) Math.ceil((rest.y - resolveLowestPoint(rotation, shape)) * subVoxelResolution
                - EngineSetting.PROJECTILE_SNAP_EPSILON);

        if (vehicleCargoSystem.land(vehicle, itemInstance, orientation, subX, subY, subZ))
            return true;

        projectile.resume();

        return false;
    }

    // The height of the turned shape's lowest corner above its model grid's floor, in blocks
    private float resolveLowestPoint(Matrix4 rotation, ItemShapeStruct shape) {

        float lowest = Float.MAX_VALUE;

        for (int corner = 0; corner < EngineSetting.BOX_CORNER_COUNT; corner++) {

            int x = (corner & EngineSetting.BOX_CORNER_BIT_X) != 0 ? shape.getSizeX() : 0;
            int y = (corner & EngineSetting.BOX_CORNER_BIT_Y) != 0 ? shape.getSizeY() : 0;
            int z = (corner & EngineSetting.BOX_CORNER_BIT_Z) != 0 ? shape.getSizeZ() : 0;

            rotateAboutCentre(
                    rotation,
                    (float) (shape.getOffsetX() + x) / subVoxelResolution,
                    (float) (shape.getOffsetY() + y) / subVoxelResolution,
                    (float) (shape.getOffsetZ() + z) / subVoxelResolution);

            lowest = Math.min(lowest, cornerScratch.y);
        }

        return lowest;
    }

    // Where a point of the model grid lands once turned about the grid's centre, exactly as the item shader turns it
    private void rotateAboutCentre(Matrix4 rotation, float x, float y, float z) {

        float[] m = rotation.val;
        float localX = x - 0.5f;
        float localY = y - 0.5f;
        float localZ = z - 0.5f;

        cornerScratch.set(
                m[0] * localX + m[4] * localY + m[8] * localZ + 0.5f,
                m[1] * localX + m[5] * localY + m[9] * localZ + 0.5f,
                m[2] * localX + m[6] * localY + m[10] * localZ + 0.5f);
    }

    // Utility \\

    private ChunkInstance resolveChunk(ProjectileInstance projectile, int chunkOffsetX, int chunkOffsetZ) {

        WorldPositionStruct worldPosition = projectile.getWorldPositionStruct();
        WorldHandle worldHandle = projectile.getWorldHandle();
        long chunkCoordinate = WorldWrapUtility.wrapAroundWorld(
                worldHandle,
                Coordinate2Long.add(worldPosition.getChunkCoordinate(), chunkOffsetX, chunkOffsetZ));

        return worldStreamManager.getChunkInstance(chunkCoordinate);
    }
}
