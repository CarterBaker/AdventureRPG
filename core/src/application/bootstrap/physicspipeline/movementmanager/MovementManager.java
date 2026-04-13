package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.ManagerPackage;
import engine.settings.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

public class MovementManager extends ManagerPackage {

    /*
     * Drives the full movement pipeline for any entity each frame. Coordinates
     * horizontal movement, gravity, collision, post-collision correction, position
     * application, and chunk boundary updates in a fixed order. Reads all input
     * from the entity's InputHandle — never touches InputSystem directly.
     */

    // Internal
    private MovementBranch movementBranch;
    private GravityBranch gravityBranch;
    private BlockCollisionBranch blockCollisionBranch;

    // Cached Vectors
    private Vector3 movement;
    private Vector3 preCollisionSnapshot;

    // Settings
    private int chunkSize;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.movementBranch = create(MovementBranch.class);
        this.gravityBranch = create(GravityBranch.class);
        this.blockCollisionBranch = create(BlockCollisionBranch.class);

        // Cached Vectors
        this.movement = new Vector3();
        this.preCollisionSnapshot = new Vector3();

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
    }

    // Movement \\

    public void move(EntityInstance entity) {

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        long chunkCoordinate = worldPosition.getChunkCoordinate();
        int chunkCoordinateX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkCoordinateY = Coordinate2Long.unpackY(chunkCoordinate);

        movement.set(0, 0, 0);

        // 1. Horizontal — x, z only
        movementBranch.calculate(movement, entity);

        // 2. Gravity — adds to all three axes
        gravityBranch.calculate(movement, entity);

        // 3. Snapshot before collision
        preCollisionSnapshot.set(movement.x, movement.y, movement.z);

        // 4. Collision
        blockCollisionBranch.calculate(position, movement, entity);

        // 5. Post-collision
        gravityBranch.postCollision(preCollisionSnapshot, movement, entity);

        // 6. Apply
        position.x += movement.x;
        position.y += movement.y;
        position.z += movement.z;

        // 7. Chunk update
        chunkCoordinate = updateChunkCoordinateFrom(position, chunkCoordinateX, chunkCoordinateY);

        // 8. World wrap
        WorldWrapUtility.wrapAroundChunk(position);
        chunkCoordinate = WorldWrapUtility.wrapAroundWorld(entity.getWorldHandle(), chunkCoordinate);

        worldPosition.setPosition(position);
        worldPosition.setChunkCoordinate(chunkCoordinate);
    }

    // Chunk \\

    private long updateChunkCoordinateFrom(
            Vector3 position,
            int chunkCoordinateX,
            int chunkCoordinateY) {

        chunkCoordinateX += calculateChunkCoordinateAxisFrom(position.x);
        chunkCoordinateY += calculateChunkCoordinateAxisFrom(position.z);

        return Coordinate2Long.pack(chunkCoordinateX, chunkCoordinateY);
    }

    private int calculateChunkCoordinateAxisFrom(float axis) {

        float axisInput = axis;
        int newChunkAxis = 0;

        while (axisInput < 0) {
            axisInput += chunkSize;
            newChunkAxis -= 1;
        }

        while (axisInput >= chunkSize) {
            axisInput -= chunkSize;
            newChunkAxis += 1;
        }

        return newChunkAxis;
    }
}