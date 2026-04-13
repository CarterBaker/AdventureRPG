package application.bootstrap.physicspipeline.raycastmanager;

import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.physicspipeline.util.ScreenRayStruct;
import application.core.engine.ManagerPackage;
import application.core.util.mathematics.vectors.Vector3;

public class RaycastManager extends ManagerPackage {

    /*
     * Owns block raycasting and the current frame's screen-space ray. The
     * ScreenRayStruct is pre-allocated — ScreenCastBranch writes to it each
     * frame when a click is detected, zero allocation. Other systems read
     * getScreenRay() and check hasScreenRay() to act on clicks. BlockCastBranch
     * handles all DDA world-space traversal.
     */

    // Branches
    private BlockCastBranch blockCastBranch;
    private ScreenCastBranch screenCastBranch;

    // Screen Ray
    private ScreenRayStruct screenRay;
    private boolean hasScreenRay;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.blockCastBranch = create(BlockCastBranch.class);
        this.screenCastBranch = create(ScreenCastBranch.class);

        // Screen Ray
        this.screenRay = new ScreenRayStruct();
    }

    @Override
    protected void update() {
        hasScreenRay = screenCastBranch.cast(screenRay);
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

    public ScreenRayStruct getScreenRay() {
        return screenRay;
    }

    public boolean hasScreenRay() {
        return hasScreenRay;
    }
}