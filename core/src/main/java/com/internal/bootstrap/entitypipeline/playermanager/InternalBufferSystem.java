package com.internal.bootstrap.entitypipeline.playermanager;

import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.extras.Coordinate2Long;

class InternalBufferSystem extends SystemPackage {

    /*
     * Pushes the player's current chunk coordinate and world position to the
     * PlayerPositionData UBO each frame. Wired to the player's
     * WorldPositionStruct via updatePlayerPosition().
     */

    // Internal
    private UBOManager uboManager;

    // UBO
    private UBOHandle playerChunkUBO;

    // Internal \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        this.playerChunkUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.PLAYER_POSITION_UBO);
    }

    // Buffer \\

    void updatePlayerPosition(WorldPositionStruct playerPosition) {

        long chunkCoordinate = playerPosition.getChunkCoordinate();
        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        playerChunkUBO.updateUniform("u_playerChunkX", chunkX);
        playerChunkUBO.updateUniform("u_playerChunkZ", chunkZ);
        playerChunkUBO.updateUniform("u_playerPosition", playerPosition.getPosition());

        uboManager.push(playerChunkUBO);
    }
}