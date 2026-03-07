package com.internal.bootstrap.physicspipeline.moveementmanager;

import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.physicspipeline.moveementmanager.movement.BlockCollisionBranch;
import com.internal.bootstrap.physicspipeline.moveementmanager.movement.GravityBranch;
import com.internal.bootstrap.physicspipeline.moveementmanager.movement.MovementBranch;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class MovementManager extends ManagerPackage {

    // Internal
    private MovementBranch movementBranch;
    private GravityBranch gravityBranch;
    private BlockCollisionBranch blockCollisionBranch;

    private Vector3 movement;

    // Settings
    private int CHUNK_SIZE;

    // Internal \\

    @Override
    protected void create() {
        this.movementBranch = create(MovementBranch.class);
        this.gravityBranch = create(GravityBranch.class);
        this.blockCollisionBranch = create(BlockCollisionBranch.class);
        this.movement = new Vector3();
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    }

    // Movement \\

    /**
     * Moves an entity based on a normalized Vector3Int input.
     * Does not know or care if input came from a player or AI.
     * All physics, gravity, and collision are self-contained here.
     */
    public void move(Vector3Int input, Vector3 direction, EntityHandle entity) {

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        long chunkCoordinate = worldPosition.getChunkCoordinate();
        int chunkCoordinateX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkCoordinateY = Coordinate2Long.unpackY(chunkCoordinate);

        entity.update();

        // 1. Horizontal — movement.y left at 0 here
        movementBranch.calculate(input, direction, movement, entity);

        // 2. Vertical — gravity always acts, jump when input.y == 1
        float preCollisionY = gravityBranch.calculate(input.y, entity);
        movement.y = preCollisionY;

        // 3. Collision — all axes
        blockCollisionBranch.calculate(position, movement, entity);
        float postCollisionY = movement.y;

        // 4. Post-collision — resolve landing and ceiling hits
        gravityBranch.postCollision(preCollisionY, postCollisionY, entity);

        // 5. Apply movement
        position.x += movement.x;
        position.y += movement.y;
        position.z += movement.z;

        // 6. Chunk coordinate update
        chunkCoordinate = updateChunkCoordinateFrom(position, chunkCoordinateX, chunkCoordinateY);

        // 7. World wrap
        WorldWrapUtility.wrapAroundChunk(position);
        chunkCoordinate = WorldWrapUtility.wrapAroundWorld(entity.getWorldHandle(), chunkCoordinate);

        worldPosition.setPosition(position);
        worldPosition.setChunkCoordinate(chunkCoordinate);
    }

    // Chunk \\

    private long updateChunkCoordinateFrom(Vector3 position, int chunkCoordinateX, int chunkCoordinateY) {
        chunkCoordinateX += calculateChunkCoordinateAxisFrom(position.x);
        chunkCoordinateY += calculateChunkCoordinateAxisFrom(position.z);
        return Coordinate2Long.pack(chunkCoordinateX, chunkCoordinateY);
    }

    private int calculateChunkCoordinateAxisFrom(float axis) {
        float axisInput = axis;
        int newChunkAxis = 0;
        while (axisInput < 0) {
            axisInput += CHUNK_SIZE;
            newChunkAxis -= 1;
        }
        while (axisInput >= CHUNK_SIZE) {
            axisInput -= CHUNK_SIZE;
            newChunkAxis += 1;
        }
        return newChunkAxis;
    }
}