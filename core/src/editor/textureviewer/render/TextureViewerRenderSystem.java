package editor.textureviewer.render;

import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.textureviewer.TextureViewerSetting;
import editor.textureviewer.atlas.TextureViewerAtlasSystem;
import editor.textureviewer.select.TextureViewerSelectSystem;
import engine.editor.EditorSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;

public class TextureViewerRenderSystem extends SystemPackage {

    /*
     * Draws the item atlas into this window's scene target where the atlas
     * system placed it. The shader outlines every source image, highlighting
     * the one under the pointer and the one chosen as the brush.
     */

    // Internal
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;
    private ItemEditorManager itemEditorManager;
    private TextureViewerAtlasSystem textureViewerAtlasSystem;
    private TextureViewerSelectSystem textureViewerSelectSystem;

    // Render Target
    private FboInstance sceneFbo;

    // Models
    private ModelInstance atlasModel;

    // Layout
    private Vector4 viewRect;

    // Base \\

    @Override
    protected void create() {
        this.viewRect = new Vector4();
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
        this.itemEditorManager = get(ItemEditorManager.class);
        this.textureViewerAtlasSystem = get(TextureViewerAtlasSystem.class);
        this.textureViewerSelectSystem = get(TextureViewerSelectSystem.class);
    }

    @Override
    protected void awake() {

        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, context.getWindow());
        this.atlasModel = modelManager.createModel(
                meshManager.getMeshHandleFromMeshName(TextureViewerSetting.MESH_QUAD),
                materialManager.getMaterialIDFromMaterialName(TextureViewerSetting.MATERIAL_ATLAS));

        MaterialInstance material = atlasModel.getMaterial();
        material.setUniform(TextureViewerSetting.UNIFORM_TILE_RECTS, textureViewerAtlasSystem.getTileRects());
        material.setUniform(TextureViewerSetting.UNIFORM_TILE_COUNT, textureViewerAtlasSystem.getTileRects().length);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        if (textureViewerAtlasSystem.getAtlasSize() > 0f) {
            updateUniforms(window);
            renderManager.pushRenderCall(atlasModel, sceneFbo, TextureViewerSetting.DEPTH_ATLAS, window);
        }

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    private void updateUniforms(WindowInstance window) {

        float width = window.getWidth();
        float height = window.getHeight();
        float left = textureViewerAtlasSystem.getAtlasX();
        float bottom = textureViewerAtlasSystem.getAtlasY();
        float size = textureViewerAtlasSystem.getAtlasSize();

        viewRect.set(
                left / width * 2f - 1f,
                bottom / height * 2f - 1f,
                (left + size) / width * 2f - 1f,
                (bottom + size) / height * 2f - 1f);

        MaterialInstance material = atlasModel.getMaterial();
        material.setUniform(TextureViewerSetting.UNIFORM_VIEW_RECT, viewRect);
        material.setUniform(TextureViewerSetting.UNIFORM_HOVERED_TILE, textureViewerSelectSystem.getHoveredTile());
        material.setUniform(
                TextureViewerSetting.UNIFORM_SELECTED_TILE,
                textureViewerAtlasSystem.indexOfTile(itemEditorManager.getBrushTextureName()));
    }
}
