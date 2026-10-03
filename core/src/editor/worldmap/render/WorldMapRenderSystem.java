package editor.worldmap.render;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.mappipeline.map.MapDrawStruct;
import application.bootstrap.mappipeline.map.MapViewStruct;
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
import editor.runtime.EditorSetting;
import editor.worldmap.WorldMapSetting;
import editor.worldmap.view.WorldMapViewSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldMapRenderSystem extends SystemPackage {

    /*
     * Draws the world map into this window's scene target: one quad per draw
     * the engine's map resolved for this view this frame, each showing its
     * part of a map tile texture, then the arrow marking the character, at the copy of its
     * position nearest the view's centre and turned to its facing. Each tile
     * carries the world region it covers and the shared overlay textures, so
     * its shader can shade day and night and weather over it when the view
     * shows them. Tile quads are pooled with their own materials and grow
     * only to the most tiles a frame has needed.
     */

    // Internal
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private MapManager mapManager;
    private WorldMapViewSystem worldMapViewSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Models
    private MeshHandle quadMesh;
    private int tileMaterialID;
    private ObjectArrayList<ModelInstance> tileModels;
    private ModelInstance markerModel;

    // Tiles
    private Vector4 viewRect;
    private Vector4 uvRect;
    private Vector4 worldRect;

    // Overlays
    private Vector2 weatherOffset;

    // Marker
    private Vector2 markerCenter;
    private Vector2 markerDirection;
    private Vector2 markerScale;

    // Base \\

    @Override
    protected void create() {

        // Models
        this.tileModels = new ObjectArrayList<>();

        // Tiles
        this.viewRect = new Vector4();
        this.uvRect = new Vector4();
        this.worldRect = new Vector4();

        // Overlays
        this.weatherOffset = new Vector2();

        // Marker
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
        this.mapManager = get(MapManager.class);
        this.worldMapViewSystem = get(WorldMapViewSystem.class);
    }

    @Override
    protected void awake() {

        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, context.getWindow());
        this.quadMesh = meshManager.getMeshHandleFromMeshName(WorldMapSetting.MESH_QUAD);
        this.tileMaterialID = materialManager.getMaterialIDFromMaterialName(WorldMapSetting.MATERIAL_TILE);
        this.markerModel = modelManager.createModel(
                quadMesh,
                materialManager.cloneMaterial(WorldMapSetting.MATERIAL_MARKER));
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        if (window.getWidth() > 0 && window.getHeight() > 0) {
            renderTiles(window);
            renderMarker(window);
        }

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    // Tiles \\

    private void renderTiles(WindowInstance window) {

        MapViewStruct mapView = worldMapViewSystem.getMapView();
        float width = window.getWidth();
        float height = window.getHeight();
        float showDayNight = mapView.isShowingDayNight() && mapManager.hasDayNightOverlay() ? 1f : 0f;
        float showWeather = mapView.isShowingWeather() && mapManager.hasWeatherOverlay() ? 1f : 0f;

        if (showWeather > 0f)
            weatherOffset.set(mapManager.getWeatherOffsetU(), mapManager.getWeatherOffsetV());

        for (int i = 0; i < mapView.getDrawCount(); i++) {

            MapDrawStruct draw = mapView.getDraw(i);
            ModelInstance tileModel = acquireTileModel(i);
            MaterialInstance material = tileModel.getMaterial();

            viewRect.set(
                    draw.getLeft() / width * 2f - 1f,
                    draw.getBottom() / height * 2f - 1f,
                    draw.getRight() / width * 2f - 1f,
                    draw.getTop() / height * 2f - 1f);
            uvRect.set(draw.getU0(), draw.getV0(), draw.getU1(), draw.getV1());
            worldRect.set(draw.getWorldU0(), draw.getWorldV0(), draw.getWorldU1(), draw.getWorldV1());

            material.setUniform(WorldMapSetting.UNIFORM_VIEW_RECT, viewRect);
            material.setUniform(WorldMapSetting.UNIFORM_UV_RECT, uvRect);
            material.setUniform(WorldMapSetting.UNIFORM_TILE_TEXTURE, draw.getTexture());
            material.setUniform(WorldMapSetting.UNIFORM_WORLD_RECT, worldRect);
            material.setUniform(WorldMapSetting.UNIFORM_SHOW_DAY_NIGHT, showDayNight);
            material.setUniform(WorldMapSetting.UNIFORM_SHOW_WEATHER, showWeather);

            if (showDayNight > 0f)
                material.setUniform(WorldMapSetting.UNIFORM_DAYLIGHT_TEXTURE, mapManager.getDaylightTexture());

            if (showWeather > 0f) {
                material.setUniform(WorldMapSetting.UNIFORM_WEATHER_TEXTURE, mapManager.getWeatherTexture());
                material.setUniform(WorldMapSetting.UNIFORM_WEATHER_OFFSET, weatherOffset);
            }
            renderManager.pushRenderCall(tileModel, sceneFbo, WorldMapSetting.DEPTH_TILES, window);
        }
    }

    private ModelInstance acquireTileModel(int index) {

        if (index < tileModels.size())
            return tileModels.get(index);

        ModelInstance tileModel = modelManager.createModel(quadMesh, materialManager.cloneMaterial(tileMaterialID));
        tileModels.add(tileModel);

        return tileModel;
    }

    // Marker \\

    private void renderMarker(WindowInstance window) {

        if (!worldMapViewSystem.hasPlayer())
            return;

        WorldHandle worldHandle = worldMapViewSystem.getWorldHandle();
        float width = window.getWidth();
        float height = window.getHeight();
        double blocksPerPixel = worldMapViewSystem.getBlocksPerPixel();

        double deltaX = WorldWrapUtility.wrappedDelta(
                worldMapViewSystem.getPlayerX(), worldMapViewSystem.getCenterX(), worldHandle.getWorldScale().x);
        double deltaZ = WorldWrapUtility.wrappedDelta(
                worldMapViewSystem.getPlayerZ(), worldMapViewSystem.getCenterZ(), worldHandle.getWorldScale().y);

        float screenX = (float) (width * 0.5 + deltaX / blocksPerPixel);
        float screenY = (float) (height * 0.5 - deltaZ / blocksPerPixel);
        float margin = WorldMapSetting.MARKER_SIZE_PIXELS;

        if (screenX < -margin || screenY < -margin || screenX > width + margin || screenY > height + margin)
            return;

        markerCenter.set(screenX / width * 2f - 1f, screenY / height * 2f - 1f);
        markerDirection.set(worldMapViewSystem.getHeadingX(), -worldMapViewSystem.getHeadingZ());
        markerScale.set(margin / width * 2f, margin / height * 2f);

        MaterialInstance markerMaterial = markerModel.getMaterial();
        markerMaterial.setUniform(WorldMapSetting.UNIFORM_MARKER_CENTER, markerCenter);
        markerMaterial.setUniform(WorldMapSetting.UNIFORM_MARKER_DIRECTION, markerDirection);
        markerMaterial.setUniform(WorldMapSetting.UNIFORM_MARKER_SCALE, markerScale);

        renderManager.pushRenderCall(markerModel, sceneFbo, WorldMapSetting.DEPTH_MARKER, window);
    }
}
