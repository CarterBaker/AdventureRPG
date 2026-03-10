package com.internal.bootstrap.entitypipeline.playermanager;

import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

public class InternalBufferSystem extends SystemPackage {

    private UBOManager uboManager;
    private UBOHandle playerChunkUBO;

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        this.playerChunkUBO = uboManager.getUBOHandleFromUBOName("PlayerPositionData");
    }

    public void updatePlayerPosition(WorldPositionStruct playerPosition) {
        int chunkX = Coordinate2Long.unpackX(playerPosition.getChunkCoordinate());
        int chunkZ = Coordinate2Long.unpackY(playerPosition.getChunkCoordinate());
        playerChunkUBO.updateUniform("u_playerChunkX", chunkX);
        playerChunkUBO.updateUniform("u_playerChunkZ", chunkZ);
        playerChunkUBO.updateUniform("u_playerPosition", playerPosition.getPosition());
        playerChunkUBO.push();
    }
}