package com.internal.bootstrap.physicspipeline.raycastmanager;

import com.internal.bootstrap.physicspipeline.util.BlockCastStruct;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class RaycastManager extends ManagerPackage {

    /*
     * Owns block raycasting for the physics pipeline. Delegates all DDA
     * traversal to BlockCastBranch and exposes a single typed cast method
     * to the rest of the engine.
     */

    // Internal
    private BlockCastBranch blockCastBranch;

    // Internal \\

    @Override
    protected void create() {
        this.blockCastBranch = create(BlockCastBranch.class);
    }

    // Accessible \\

    public void castBlock(
            long chunkCoordinate,
            Vector3 rayOrigin,
            Vector3 direction,
            float maxDistance,
            BlockCastStruct out) {
        blockCastBranch.cast(chunkCoordinate, rayOrigin, direction, maxDistance, out);
    }
}