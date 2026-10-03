package editor.worldeditor.render;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorTool;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import editor.worldeditor.WorldEditorSetting;
import editor.worldeditor.tool.WorldEditorToolSystem;
import editor.worldeditor.view.WorldEditorViewSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;

public class WorldEditorRenderSystem extends SystemPackage {

    /*
     * Draws the world image into this window's scene target where the view
     * places it, from the GPU texture ImageManager keeps current, then the
     * brush's outline over the pixels a stroke at the pointer would cover.
     */

    // Internal
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private ImageManager imageManager;
    private WorldEditorManager worldEditorManager;
    private WorldEditorViewSystem worldEditorViewSystem;
    private WorldEditorToolSystem worldEditorToolSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Models
    private ModelInstance imageModel;
    private ModelInstance brushModel;

    // Layout
    private Vector4 viewRect;
    private Vector4 uvRect;

    // Base \\

    @Override
    protected void create() {
        this.viewRect = new Vector4();
        this.uvRect = new Vector4(0f, 0f, 1f, 1f);
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.imageManager = get(ImageManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.worldEditorViewSystem = get(WorldEditorViewSystem.class);
        this.worldEditorToolSystem = get(WorldEditorToolSystem.class);
    }

    @Override
    protected void awake() {

        MeshHandle quadMesh = meshManager.getMeshHandleFromMeshName(WorldEditorSetting.MESH_QUAD);

        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, context.getWindow());
        this.imageModel = modelManager.createModel(
                quadMesh, materialManager.cloneMaterial(WorldEditorSetting.MATERIAL_IMAGE));
        this.brushModel = modelManager.createModel(
                quadMesh, materialManager.cloneMaterial(WorldEditorSetting.MATERIAL_BRUSH));
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        ImageDocumentInstance image = worldEditorManager.getWorldImage();
        int texture = imageManager.useTexture(image);

        if (texture != 0 && window.getWidth() > 0 && window.getHeight() > 0) {
            renderImage(window, image, texture);
            renderBrush(window);
        }

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    // Image \\

    private void renderImage(WindowInstance window, ImageDocumentInstance image, int texture) {

        setViewRect(window, 0.0, 0.0, image.getWidth(), image.getHeight());

        MaterialInstance material = imageModel.getMaterial();
        material.setUniform(WorldEditorSetting.UNIFORM_VIEW_RECT, viewRect);
        material.setUniform(WorldEditorSetting.UNIFORM_UV_RECT, uvRect);
        material.setUniform(WorldEditorSetting.UNIFORM_TILE_TEXTURE, texture);

        renderManager.pushRenderCall(imageModel, sceneFbo, WorldEditorSetting.DEPTH_IMAGE, window);
    }

    // Brush \\

    private void renderBrush(WindowInstance window) {

        if (!worldEditorToolSystem.isHovering() || worldEditorManager.getTool() != WorldEditorTool.BRUSH)
            return;

        int radius = worldEditorManager.getBrushRadius();
        int pixelX = worldEditorToolSystem.getHoveredX();
        int pixelY = worldEditorToolSystem.getHoveredY();

        setViewRect(window, pixelX - radius, pixelY - radius, pixelX + radius + 1, pixelY + radius + 1);
        brushModel.getMaterial().setUniform(WorldEditorSetting.UNIFORM_VIEW_RECT, viewRect);

        renderManager.pushRenderCall(brushModel, sceneFbo, WorldEditorSetting.DEPTH_BRUSH, window);
    }

    // Utility \\

    // Image rectangle, y down, to normalized device coordinates as (left, bottom, right, top)
    private void setViewRect(WindowInstance window, double left, double top, double right, double bottom) {

        float width = window.getWidth();
        float height = window.getHeight();

        viewRect.set(
                worldEditorViewSystem.imageToScreenX(left) / width * 2f - 1f,
                worldEditorViewSystem.imageToScreenY(bottom) / height * 2f - 1f,
                worldEditorViewSystem.imageToScreenX(right) / width * 2f - 1f,
                worldEditorViewSystem.imageToScreenY(top) / height * 2f - 1f);
    }
}
