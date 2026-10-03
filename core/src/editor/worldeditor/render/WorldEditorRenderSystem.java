package editor.worldeditor.render;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.mappipeline.map.MapMarkerStruct;
import application.bootstrap.mappipeline.mapmanager.MapManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
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
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldEditorRenderSystem extends SystemPackage {

    /*
     * Draws the world image into this window's scene target where the view
     * places it, from the GPU texture ImageManager keeps current, then the
     * brush's outline over the pixels a stroke at the pointer would cover,
     * and, while shown, an arrow for every player at their place and facing.
     * Marker quads are pooled with their own materials.
     */

    // Internal
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private ImageManager imageManager;
    private MapManager mapManager;
    private WorldEditorManager worldEditorManager;
    private WorldEditorViewSystem worldEditorViewSystem;
    private WorldEditorToolSystem worldEditorToolSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Models
    private ModelInstance imageModel;
    private ModelInstance brushModel;
    private MeshHandle quadMesh;
    private int markerMaterialID;
    private ObjectArrayList<ModelInstance> markerModels;

    // Markers
    private boolean showingPlayers;
    private ObjectArrayList<MapMarkerStruct> markers;
    private Vector2 markerCenter;
    private Vector2 markerDirection;
    private Vector2 markerScale;

    // Layout
    private Vector4 viewRect;
    private Vector4 uvRect;

    // Base \\

    @Override
    protected void create() {
        this.viewRect = new Vector4();
        this.uvRect = new Vector4(0f, 0f, 1f, 1f);

        // Models
        this.markerModels = new ObjectArrayList<>();

        // Markers
        this.markers = new ObjectArrayList<>();
        this.markerCenter = new Vector2();
        this.markerDirection = new Vector2();
        this.markerScale = new Vector2();
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
        this.mapManager = get(MapManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.worldEditorViewSystem = get(WorldEditorViewSystem.class);
        this.worldEditorToolSystem = get(WorldEditorToolSystem.class);
    }

    @Override
    protected void awake() {

        this.quadMesh = meshManager.getMeshHandleFromMeshName(WorldEditorSetting.MESH_QUAD);
        this.markerMaterialID = materialManager.getMaterialIDFromMaterialName(WorldEditorSetting.MATERIAL_MARKER);

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
            renderMarkers(window);
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

    // Markers \\

    private void renderMarkers(WindowInstance window) {

        if (!showingPlayers)
            return;

        WorldHandle worldHandle = worldEditorManager.getWorldHandle();
        int count = mapManager.resolveMarkers(worldHandle, markers);
        double pixelBlocks = EngineSetting.CHUNKS_PER_PIXEL * (double) EngineSetting.CHUNK_SIZE;
        float width = window.getWidth();
        float height = window.getHeight();
        float size = WorldEditorSetting.MARKER_SIZE_PIXELS;

        markerScale.set(size / width * 2f, size / height * 2f);

        for (int i = 0; i < count; i++) {

            MapMarkerStruct marker = markers.get(i);
            float screenX = worldEditorViewSystem.imageToScreenX(
                    WorldWrapUtility.wrapBlockX(worldHandle, marker.getX()) / pixelBlocks);
            float screenY = worldEditorViewSystem.imageToScreenY(
                    WorldWrapUtility.wrapBlockZ(worldHandle, marker.getZ()) / pixelBlocks);

            if (screenX < -size || screenY < -size || screenX > width + size || screenY > height + size)
                continue;

            markerCenter.set(screenX / width * 2f - 1f, screenY / height * 2f - 1f);
            markerDirection.set(marker.getHeadingX(), -marker.getHeadingZ());

            ModelInstance markerModel = acquireMarkerModel(i);
            MaterialInstance material = markerModel.getMaterial();
            material.setUniform(WorldEditorSetting.UNIFORM_MARKER_CENTER, markerCenter);
            material.setUniform(WorldEditorSetting.UNIFORM_MARKER_DIRECTION, markerDirection);
            material.setUniform(WorldEditorSetting.UNIFORM_MARKER_SCALE, markerScale);

            renderManager.pushRenderCall(markerModel, sceneFbo, WorldEditorSetting.DEPTH_MARKERS, window);
        }
    }

    private ModelInstance acquireMarkerModel(int index) {

        if (index < markerModels.size())
            return markerModels.get(index);

        ModelInstance markerModel = modelManager.createModel(
                quadMesh, materialManager.cloneMaterial(markerMaterialID));
        markerModels.add(markerModel);

        return markerModel;
    }

    public void togglePlayers() {
        showingPlayers = !showingPlayers;
    }

    public boolean isShowingPlayers() {
        return showingPlayers;
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
