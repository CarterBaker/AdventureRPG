package editor.runtime.worldmap.render;

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
import application.bootstrap.worldpipeline.util.GlobeUtility;
import application.bootstrap.worldpipeline.util.GlobeViewStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.runtime.EditorSetting;
import editor.runtime.placementicon.PlacementIconSystem;
import editor.runtime.worldmap.WorldMapSetting;
import editor.runtime.worldmap.view.WorldMapViewSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix3Double;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Double;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldMapRenderSystem extends SystemPackage {

    /*
     * Draws the world map into this window's scene target: one quad per draw
     * the engine's map resolved for this view this frame, each showing its
     * part of a map tile texture, then an icon for every hand placement of
     * the world through PlacementIconSystem, then the arrow marking the character, at the copy of its
     * position nearest the view's centre and turned to its facing, or under the pointer while
     * it is being dragged. Each tile
     * carries the world region it covers and the shared overlay textures, so
     * its shader can shade day and night and weather over it when the view
     * shows them. Tile quads are pooled with their own materials and grow
     * only to the most tiles a frame has needed. On a globe each tile is a
     * quad over the window rectangle it shows in, whose shader casts the
     * view's rays at the ball and keeps only what lands on the tile's square
     * of its block face; icons and the arrow stand where the ball shows
     * their place, hidden on its far side.
     */

    // Internal
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private MapManager mapManager;
    private WorldManager worldManager;
    private WorldMapViewSystem worldMapViewSystem;
    private PlacementIconSystem placementIconSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Models
    private MeshHandle quadMesh;
    private int tileMaterialID;
    private ObjectArrayList<ModelInstance> tileModels;
    private int globeTileMaterialID;
    private ObjectArrayList<ModelInstance> globeTileModels;
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

    // Globe
    private final Vector4 faceRect = new Vector4();
    private final Vector3 cubeAxisX = new Vector3();
    private final Vector3 cubeAxisY = new Vector3();
    private final Vector3 cubeAxisZ = new Vector3();
    private final Vector3Double globeDirection = new Vector3Double();

    // Base \\

    @Override
    protected void create() {

        // Models
        this.tileModels = new ObjectArrayList<>();
        this.globeTileModels = new ObjectArrayList<>();

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
        this.worldManager = get(WorldManager.class);
        this.worldMapViewSystem = get(WorldMapViewSystem.class);
        this.placementIconSystem = get(PlacementIconSystem.class);
    }

    @Override
    protected void awake() {

        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, context.getWindow());
        this.quadMesh = meshManager.getMeshHandleFromMeshName(WorldMapSetting.MESH_QUAD);
        this.tileMaterialID = materialManager.getMaterialIDFromMaterialName(WorldMapSetting.MATERIAL_TILE);
        this.globeTileMaterialID = materialManager.getMaterialIDFromMaterialName(
                WorldMapSetting.MATERIAL_GLOBE_TILE);
        this.markerModel = modelManager.createModel(
                quadMesh,
                materialManager.cloneMaterial(WorldMapSetting.MATERIAL_MARKER));
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        if (window.getWidth() > 0 && window.getHeight() > 0) {

            if (worldMapViewSystem.isShown())
                renderGlobeTiles(window);
            else
                renderTiles(window);

            renderPlacementIcons(window);
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
        return acquireModel(tileModels, tileMaterialID, index);
    }

    private ModelInstance acquireModel(ObjectArrayList<ModelInstance> models, int materialID, int index) {

        if (index < models.size())
            return models.get(index);

        ModelInstance model = modelManager.createModel(quadMesh, materialManager.cloneMaterial(materialID));
        models.add(model);

        return model;
    }

    // Globe \\

    // Every tile the map resolved over the window rectangle it shows in, kept to its square of its block face
    private void renderGlobeTiles(WindowInstance window) {

        MapViewStruct mapView = worldMapViewSystem.getMapView();
        GlobeViewStruct globeView = mapView.getGlobeView();
        Matrix3Double globeFromCube = worldMapViewSystem.getWorldHandle().getGlobeFromCube();
        float width = window.getWidth();
        float height = window.getHeight();

        cubeAxisX.set((float) globeFromCube.getM00(), (float) globeFromCube.getM10(), (float) globeFromCube.getM20());
        cubeAxisY.set((float) globeFromCube.getM01(), (float) globeFromCube.getM11(), (float) globeFromCube.getM21());
        cubeAxisZ.set((float) globeFromCube.getM02(), (float) globeFromCube.getM12(), (float) globeFromCube.getM22());

        for (int i = 0; i < mapView.getDrawCount(); i++) {

            MapDrawStruct draw = mapView.getDraw(i);
            ModelInstance tileModel = acquireModel(globeTileModels, globeTileMaterialID, i);
            MaterialInstance material = tileModel.getMaterial();

            viewRect.set(
                    draw.getLeft() / width * 2f - 1f,
                    draw.getBottom() / height * 2f - 1f,
                    draw.getRight() / width * 2f - 1f,
                    draw.getTop() / height * 2f - 1f);
            uvRect.set(draw.getU0(), draw.getV0(), draw.getU1(), draw.getV1());
            faceRect.set(draw.getWorldU0(), draw.getWorldV0(), draw.getWorldU1(), draw.getWorldV1());

            material.setUniform(WorldMapSetting.UNIFORM_VIEW_RECT, viewRect);
            material.setUniform(WorldMapSetting.UNIFORM_UV_RECT, uvRect);
            material.setUniform(WorldMapSetting.UNIFORM_TILE_TEXTURE, draw.getTexture());
            material.setUniform(WorldMapSetting.UNIFORM_FACE, (float) draw.getFace());
            material.setUniform(WorldMapSetting.UNIFORM_FACE_RECT, faceRect);
            material.setUniform(WorldMapSetting.UNIFORM_GLOBE_CAMERA, globeView.getPosition());
            material.setUniform(WorldMapSetting.UNIFORM_GLOBE_FORWARD, globeView.getForward());
            material.setUniform(WorldMapSetting.UNIFORM_GLOBE_RIGHT, globeView.getRight());
            material.setUniform(WorldMapSetting.UNIFORM_GLOBE_UP, globeView.getUp());
            material.setUniform(WorldMapSetting.UNIFORM_CUBE_AXIS_X, cubeAxisX);
            material.setUniform(WorldMapSetting.UNIFORM_CUBE_AXIS_Y, cubeAxisY);
            material.setUniform(WorldMapSetting.UNIFORM_CUBE_AXIS_Z, cubeAxisZ);

            renderManager.pushRenderCall(tileModel, sceneFbo, WorldMapSetting.DEPTH_TILES, window);
        }
    }

    // Where a world block position shows on the globe, false on its far side
    private boolean projectGlobe(WindowInstance window, double worldX, double worldZ) {

        GlobeUtility.resolveDirection(worldMapViewSystem.getWorldHandle(), worldX, worldZ, globeDirection);

        return worldMapViewSystem.getMapView().getGlobeView().projectSurface(
                globeDirection.x, globeDirection.y, globeDirection.z, window.getWidth(), window.getHeight());
    }

    // Placements \\

    // Every hand placement's icon, at the copy of its column nearest the view's centre, or where the globe shows it
    private void renderPlacementIcons(WindowInstance window) {

        WorldHandle worldHandle = worldMapViewSystem.getWorldHandle();
        WorldPlacementStruct[] placements = worldManager.getPlacements(worldHandle);
        double blocksPerPixel = worldMapViewSystem.getBlocksPerPixel();
        double halfWidth = window.getWidth() * 0.5;
        double halfHeight = window.getHeight() * 0.5;

        placementIconSystem.begin();

        if (worldMapViewSystem.isShown()) {

            GlobeViewStruct globeView = worldMapViewSystem.getMapView().getGlobeView();

            for (int i = 0; i < placements.length; i++)
                if (projectGlobe(window, placements[i].getWorldX(), placements[i].getWorldZ()))
                    placementIconSystem.push(
                            window, sceneFbo, WorldMapSetting.DEPTH_PLACEMENTS,
                            globeView.getProjectedX(), globeView.getProjectedY(), placements[i]);

            return;
        }

        for (int i = 0; i < placements.length; i++)
            placementIconSystem.push(
                    window, sceneFbo, WorldMapSetting.DEPTH_PLACEMENTS,
                    (float) (halfWidth + WorldWrapUtility.wrappedDelta(
                            placements[i].getWorldX(), worldMapViewSystem.getCenterX(),
                            worldHandle.getWorldScale().x) / blocksPerPixel),
                    (float) (halfHeight - WorldWrapUtility.wrappedDelta(
                            placements[i].getWorldZ(), worldMapViewSystem.getCenterZ(),
                            worldHandle.getWorldScale().y) / blocksPerPixel),
                    placements[i]);
    }

    // Marker \\

    // The arrow's window position, left in its centre, and its facing on screen: across the flat map's wrap, or
    // where the globe shows it, its facing a step along the ground projected; false on the globe's far side
    private boolean placeMarker(WindowInstance window) {

        WorldHandle worldHandle = worldMapViewSystem.getWorldHandle();
        double markerX = worldMapViewSystem.getMarkerX();
        double markerZ = worldMapViewSystem.getMarkerZ();

        if (!worldMapViewSystem.isShown()) {

            double blocksPerPixel = worldMapViewSystem.getBlocksPerPixel();
            double deltaX = WorldWrapUtility.wrappedDelta(
                    markerX, worldMapViewSystem.getCenterX(), worldHandle.getWorldScale().x);
            double deltaZ = WorldWrapUtility.wrappedDelta(
                    markerZ, worldMapViewSystem.getCenterZ(), worldHandle.getWorldScale().y);

            markerCenter.set(
                    (float) (window.getWidth() * 0.5 + deltaX / blocksPerPixel),
                    (float) (window.getHeight() * 0.5 - deltaZ / blocksPerPixel));
            markerDirection.set(worldMapViewSystem.getHeadingX(), -worldMapViewSystem.getHeadingZ());

            return true;
        }

        GlobeViewStruct globeView = worldMapViewSystem.getMapView().getGlobeView();
        double step = WorldMapSetting.GLOBE_HEADING_STEP_BLOCKS;

        if (!projectGlobe(window, markerX + worldMapViewSystem.getHeadingX() * step,
                markerZ + worldMapViewSystem.getHeadingZ() * step))
            return false;

        float aheadX = globeView.getProjectedX();
        float aheadY = globeView.getProjectedY();

        if (!projectGlobe(window, markerX, markerZ))
            return false;

        markerCenter.set(globeView.getProjectedX(), globeView.getProjectedY());
        markerDirection.set(aheadX - markerCenter.x, aheadY - markerCenter.y).normalize();

        return true;
    }

    private void renderMarker(WindowInstance window) {

        if (!worldMapViewSystem.hasPlayer())
            return;

        float width = window.getWidth();
        float height = window.getHeight();
        float margin = WorldMapSetting.MARKER_SIZE_PIXELS;

        if (!placeMarker(window))
            return;

        float screenX = markerCenter.x;
        float screenY = markerCenter.y;

        if (screenX < -margin || screenY < -margin || screenX > width + margin || screenY > height + margin)
            return;

        markerCenter.set(screenX / width * 2f - 1f, screenY / height * 2f - 1f);
        markerScale.set(margin / width * 2f, margin / height * 2f);

        MaterialInstance markerMaterial = markerModel.getMaterial();
        markerMaterial.setUniform(WorldMapSetting.UNIFORM_MARKER_CENTER, markerCenter);
        markerMaterial.setUniform(WorldMapSetting.UNIFORM_MARKER_DIRECTION, markerDirection);
        markerMaterial.setUniform(WorldMapSetting.UNIFORM_MARKER_SCALE, markerScale);

        renderManager.pushRenderCall(markerModel, sceneFbo, WorldMapSetting.DEPTH_MARKER, window);
    }
}
