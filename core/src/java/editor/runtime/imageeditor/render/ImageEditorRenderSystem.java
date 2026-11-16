package editor.runtime.imageeditor.render;

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
import editor.bootstrap.imageeditorpipeline.imageeditormanager.ImageEditorManager;
import editor.bootstrap.imageeditorpipeline.imageproject.ImageProjectInstance;
import editor.runtime.EditorSetting;
import editor.runtime.imageeditor.ImageEditorSetting;
import editor.runtime.uveditor.view.UVEditorViewSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;

public class ImageEditorRenderSystem extends SystemPackage {

    /*
     * Draws the Image Editor into this window's scene target: what is shown
     * of the image worked — its layers laid flat, or the map picked once it
     * is made — over a checker where it is clear, framed by its border, at
     * the view's pan and zoom, on one window-filling quad, the same quad and
     * view mapping the UV Editor draws with. The image's texture is kept
     * current by ImageManager; only uniforms move each frame.
     */

    // Internal
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private ImageEditorManager imageEditorManager;
    private UVEditorViewSystem uvEditorViewSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Models
    private ModelInstance imageModel;

    // Scratch
    private Vector2 extent;

    // Base \\

    @Override
    protected void create() {
        this.extent = new Vector2();
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.imageEditorManager = get(ImageEditorManager.class);
        this.uvEditorViewSystem = get(UVEditorViewSystem.class);
    }

    @Override
    protected void awake() {
        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, context.getWindow());
        this.imageModel = modelManager.createModel(
                meshManager.getMeshHandleFromMeshName(EditorSetting.MESH_EDITOR_VIEWPORT_QUAD),
                materialManager.cloneMaterial(ImageEditorSetting.MATERIAL_IMAGE));
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        if (window.getWidth() <= 0 || window.getHeight() <= 0)
            return;

        MaterialInstance material = imageModel.getMaterial();
        ImageProjectInstance project = imageEditorManager.getActiveProject();
        int texture = project != null ? imageEditorManager.useShownTexture() : 0;

        if (project != null) {
            float side = Math.max(project.getWidth(), project.getHeight());
            extent.set(project.getWidth() / side, project.getHeight() / side);
        } else
            extent.set(1f, 1f);

        material.setUniform(EditorSetting.FLAT_VIEW_UNIFORM_UV_TO_CLIP, uvEditorViewSystem.getUVToClip());
        material.setUniform(ImageEditorSetting.UNIFORM_IMAGE_EXTENT, extent);
        material.setUniform(ImageEditorSetting.UNIFORM_HAS_IMAGE, texture != 0 ? 1 : 0);

        if (texture != 0)
            material.setUniform(ImageEditorSetting.UNIFORM_IMAGE_TEXTURE, texture);

        renderManager.pushRenderCall(imageModel, sceneFbo, ImageEditorSetting.DEPTH_IMAGE, window);
        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }
}
