package application.bootstrap.worldpipeline.worldmanager;

import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.util.GlobeUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

class WorldShapeBufferSystem extends SystemPackage {

    /*
     * Pushes the active world's shape to the WorldShapeData UBO whenever the
     * active world changes: the horizon curvature every world shader bends
     * its ground by, one over twice a globe's radius, so the land falls away
     * from the camera as a ball's surface falls below the plane under the
     * eye. A torus is flat and pushes none.
     */

    // Internal
    private WorldManager worldManager;
    private UBOManager uboManager;

    // UBO
    private UBOHandle worldShapeUBO;
    private WorldHandle pushedWorld;

    // Internal \\

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        this.worldShapeUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.WORLD_SHAPE_UBO);
    }

    @Override
    protected void update() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        if (activeWorld == null || activeWorld == pushedWorld)
            return;

        float curvature = activeWorld.isGlobe()
                ? (float) (0.5 / GlobeUtility.getRadiusBlocks(activeWorld))
                : 0f;

        worldShapeUBO.updateUniform(EngineSetting.UNIFORM_HORIZON_CURVATURE, curvature);
        uboManager.push(worldShapeUBO);

        this.pushedWorld = activeWorld;
    }
}
