package com.internal.bootstrap.entitypipeline.playermanager;

import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;

public class InternalBufferSystem extends SystemPackage {

    // Internal
    private UBOManager uboManager;
    private UBOHandle playerPositionUBO;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {

        // Internal
        this.playerPositionUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.PLAYER_POSITION_UBO);
    }

    // Update Methods \\

    public void updatePlayerPosition(WorldPositionStruct playerPosition) {

        // Update the UBO
        playerPositionUBO.updateUniform("u_playerPosition", playerPosition.getPosition());
        playerPositionUBO.push();
    }
}