package application.bootstrap.physicspipeline.raycastmanager;

import application.bootstrap.physicspipeline.util.BlockCastStruct;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;

public class RaycastManager extends ManagerPackage {

    /*
     * Owns block raycasting. Screen-space mouse position is sourced directly
     * from InputSystem and WindowManager — no intermediate struct needed.
     * Click detection lives in ElementHitSystem.
     * BlockCastBranch handles all DDA world-space traversal.
     */

    // Branches
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