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
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ProjectileRenderSystem extends SystemPackage {

    /*
     * Draws every projectile still in flight or waiting to settle into a
     * window's world target, each with its own pooled item model. A
     * projectile is placed relative to the viewer's chunk, its shape's centre
     * on its position and turned by its tumble, and one beyond the viewer's
     * neighbouring chunks is skipped.
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

        ObjectArrayList<ProjectileInstance> projectiles = projectileManager.getProjectiles();

        for (int i = 0; i < projectiles.size(); i++) {

            ProjectileInstance projectile = projectiles.get(i);

            if (projectile.getWorldHandle() != worldHandle
                    || !composeModelMatrix(projectile, worldHandle, viewerChunkCoordinate))
                continue;

            ItemDefinitionHandle item = projectile.getItemInstance().getItemDefinitionHandle();
            ModelInstance model = itemModelManager.acquireModel(item.getMeshHandle(), equipmentMaterialID);

            model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, modelScratch);
            renderManager.pushRenderCall(model, targetFbo, EngineSetting.EQUIPMENT_RENDER_DEPTH, window);
        }
    }

    // T(chunk offset + position) * R(tumble) * T(-shape centre) — false beyond the viewer's neighbouring chunks
    private boolean composeModelMatrix(
            ProjectileInstance projectile,
            WorldHandle worldHandle,
            long viewerChunkCoordinate) {

        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                worldHandle,
                viewerChunkCoordinate,
                projectile.getWorldPositionStruct().getChunkCoordinate());
        int deltaX = Coordinate2Long.unpackX(delta);
        int deltaZ = Coordinate2Long.unpackY(delta);

        if (Math.abs(deltaX) > EngineSetting.PROJECTILE_RENDER_CHUNK_RADIUS
                || Math.abs(deltaZ) > EngineSetting.PROJECTILE_RENDER_CHUNK_RADIUS)
            return false;

        Vector3 position = projectile.getWorldPositionStruct().getPosition();
        ItemShapeStruct shape = projectile.getItemInstance().getItemDefinitionHandle().getShape();
        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        modelScratch.set(
                1, 0, 0, deltaX * EngineSetting.CHUNK_SIZE + position.x,
                0, 1, 0, position.y,
                0, 0, 1, deltaZ * EngineSetting.CHUNK_SIZE + position.z,
                0, 0, 0, 1)
                .multiply(projectile.getOrientation().toMatrix(rotationScratch))
                .multiply(
                        1, 0, 0, -(shape.getOffsetX() + shape.getSizeX() * 0.5f) / resolution,
                        0, 1, 0, -(shape.getOffsetY() + shape.getSizeY() * 0.5f) / resolution,
                        0, 0, 1, -(shape.getOffsetZ() + shape.getSizeZ() * 0.5f) / resolution,
                        0, 0, 0, 1);

        return true;
    }
}
