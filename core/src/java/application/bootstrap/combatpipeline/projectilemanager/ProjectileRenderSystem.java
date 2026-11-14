package application.bootstrap.combatpipeline.projectilemanager;

import application.bootstrap.combatpipeline.projectile.ProjectileInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ProjectileRenderSystem extends SystemPackage {

    /*
     * Draws every projectile still in flight or waiting to settle into a
     * window's world target, each with its own pooled item model. A
     * projectile is placed relative to the viewer's chunk, its shape's centre
     * on its position and turned by its tumble, and one beyond the viewer's
     * neighbouring chunks is skipped. pushItem() is the one way a loose item
     * is drawn, so a recording's thrown items look just as they flew. Held
     * projectiles are not drawn.
     */

    // Internal
    private RenderManager renderManager;
    private MaterialManager materialManager;
    private ItemModelManager itemModelManager;
    private ProjectileManager projectileManager;

    // Material
    private int equipmentMaterialID;

    // Scratch
    private Matrix4 modelScratch;
    private Matrix4 rotationScratch;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.modelScratch = new Matrix4();
        this.rotationScratch = new Matrix4();
    }

    @Override
    protected void get() {

        // Internal
        this.renderManager = get(RenderManager.class);
        this.materialManager = get(MaterialManager.class);
        this.itemModelManager = get(ItemModelManager.class);
        this.projectileManager = get(ProjectileManager.class);
    }

    @Override
    protected void awake() {

        // Material
        this.equipmentMaterialID = materialManager.getMaterialIDFromMaterialName(
                EngineSetting.EQUIPMENT_ITEM_MATERIAL);
    }

    // Render \\

    public void pushProjectiles(
            WorldHandle worldHandle,
            long viewerChunkCoordinate,
            FBOInstance targetFbo,
            WindowInstance window) {

        if (projectileManager.isHeld())
            return;

        ObjectArrayList<ProjectileInstance> projectiles = projectileManager.getProjectiles();

        for (int i = 0; i < projectiles.size(); i++) {

            ProjectileInstance projectile = projectiles.get(i);

            if (projectile.getWorldHandle() != worldHandle)
                continue;

            pushItem(
                    projectile.getItemInstance().getItemDefinitionHandle(),
                    worldHandle,
                    viewerChunkCoordinate,
                    projectile.getWorldPositionStruct().getChunkCoordinate(),
                    projectile.getWorldPositionStruct().getPosition(),
                    projectile.getOrientation(),
                    targetFbo,
                    window);
        }
    }

    // One item loose in the world, its shape's centre on a position in a chunk's frame and turned by an orientation
    public void pushItem(
            ItemDefinitionHandle item,
            WorldHandle worldHandle,
            long viewerChunkCoordinate,
            long chunkCoordinate,
            Vector3 position,
            Quaternion orientation,
            FBOInstance targetFbo,
            WindowInstance window) {

        if (!composeModelMatrix(item, worldHandle, viewerChunkCoordinate, chunkCoordinate, position, orientation))
            return;

        ModelInstance model = itemModelManager.acquireModel(item.getMeshHandle(), equipmentMaterialID);

        model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, modelScratch);
        renderManager.pushRenderCall(model, targetFbo, EngineSetting.EQUIPMENT_RENDER_DEPTH, window);
    }

    // T(chunk offset + position) * R(tumble) * T(-shape centre) — false beyond the viewer's neighbouring chunks
    private boolean composeModelMatrix(
            ItemDefinitionHandle item,
            WorldHandle worldHandle,
            long viewerChunkCoordinate,
            long chunkCoordinate,
            Vector3 position,
            Quaternion orientation) {

        long delta = WorldWrapUtility.unwrapToGridCoordinate(worldHandle, viewerChunkCoordinate, chunkCoordinate);
        int deltaX = Coordinate2Long.unpackX(delta);
        int deltaZ = Coordinate2Long.unpackY(delta);

        if (Math.abs(deltaX) > EngineSetting.PROJECTILE_RENDER_CHUNK_RADIUS
                || Math.abs(deltaZ) > EngineSetting.PROJECTILE_RENDER_CHUNK_RADIUS)
            return false;

        ItemShapeStruct shape = item.getShape();
        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        modelScratch.set(
                1, 0, 0, deltaX * EngineSetting.CHUNK_SIZE + position.x,
                0, 1, 0, position.y,
                0, 0, 1, deltaZ * EngineSetting.CHUNK_SIZE + position.z,
                0, 0, 0, 1)
                .multiply(orientation.toMatrix(rotationScratch))
                .multiply(
                        1, 0, 0, -(shape.getOffsetX() + shape.getSizeX() * 0.5f) / resolution,
                        0, 1, 0, -(shape.getOffsetY() + shape.getSizeY() * 0.5f) / resolution,
                        0, 0, 1, -(shape.getOffsetZ() + shape.getSizeZ() * 0.5f) / resolution,
                        0, 0, 0, 1);

        return true;
    }
}
