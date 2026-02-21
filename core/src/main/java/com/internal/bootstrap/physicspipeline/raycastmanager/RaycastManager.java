package com.internal.bootstrap.physicspipeline.raycastmanager;

import com.internal.bootstrap.physicspipeline.raycastmanager.raycast.BlockCastBranch;
import com.internal.bootstrap.physicspipeline.util.BlockCastStruct;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class RaycastManager extends ManagerPackage {

    // Internal
    private BlockCastBranch blockCastBranch;
    // Internal \\

    @Override
    protected void create() {
        this.blockCastBranch = create(BlockCastBranch.class);
    }

    // Block Raycasting \\

    public void castBlock(
            long chunkCoordinate,
            Vector3 rayOrigin,
            Vector3 direction,
            float maxDistance,
            BlockCastStruct out) {

        blockCastBranch.cast(chunkCoordinate, rayOrigin, direction, maxDistance, out);
    }
}