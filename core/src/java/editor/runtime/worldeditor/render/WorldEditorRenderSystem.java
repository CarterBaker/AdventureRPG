package editor.runtime.worldeditor.render;

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
import application.bootstrap.worldpipeline.util.GlobeViewStruct;
import application.bootstrap.worldpipeline.util.WorldImagePointStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.brushpipeline.brush.BrushHandle;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.brushpipeline.util.BrushField;
import editor.bootstrap.brushpipeline.util.BrushShape;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorLayer;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorMode;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import editor.runtime.placementicon.PlacementIconSystem;
import editor.runtime.worldeditor.WorldEditorSetting;
import editor.runtime.worldeditor.layer.WorldEditorLayerSystem;
import editor.runtime.worldeditor.tool.WorldEditorToolSystem;
import editor.runtime.worldeditor.view.WorldEditorViewSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix3Double;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldEditorRenderSystem extends SystemPackage {

    /*
     * Draws every layer shown in the Layers tab into this window's scene
     * target where the view places it, from the GPU textures ImageManager
     * keeps current: the world image, then the height layer over it pixel for
     * pixel as the black and white heightmap it is, partly see-through so
     * every level lines up with the land beneath. Then comes the brush
     * preset's outline, in its own shape, turned and squashed as its tip is,
     * over the pixels a stroke at the pointer would cover, or, while placing
     * or carrying a design from the Settlement Planner, a ring as wide as its
     * design around the column it would stand on, the box the Select tool is
     * dragging and, while the layer picked places, an icon for every
     * hand-placed settlement, structure and camera through
     * PlacementIconSystem, those being moved drawn where they would stand,
     * and, while shown, an arrow for every player at their place and facing.
     * A globe world is drawn instead as one quad over the window whose shader
     * casts the view camera's rays at the ball and reads both layers there,
     * drawing the brush, the footprint, the box and, while the Cube layer is
     * shown, the block grid's cube in the same pass; markers stand where the
     * ball shows their place and hide behind it. Marker quads are pooled with
     * their own materials.
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
    private BrushManager brushManager;
    private SettlementPlannerManager settlementPlannerManager;
    private WorldEditorViewSystem worldEditorViewSystem;
    private WorldEditorToolSystem worldEditorToolSystem;
    private WorldEditorLayerSystem worldEditorLayerSystem;
    private PlacementIconSystem placementIconSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Models
    private ModelInstance imageModel;
    private ModelInstance heightModel;
    private ModelInstance brushModel;
    private ModelInstance footprintModel;
    private ModelInstance boxModel;
    private ModelInstance globeModel;
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
    private Vector4 windowRect;

    // Globe
    private final Vector2 brushCenter = new Vector2();
    private final Vector2 footprintCenter = new Vector2();
    private final Vector4 boxRect = new Vector4();
    private final Vector3 cubeAxisX = new Vector3();
    private final Vector3 cubeAxisY = new Vector3();
    private final Vector3 cubeAxisZ = new Vector3();
    private final WorldImagePointStruct imagePoint = new WorldImagePointStruct();

    // Base \\

    @Override
    protected void create() {
        this.viewRect = new Vector4();
        this.uvRect = new Vector4(0f, 0f, 1f, 1f);
        this.windowRect = new Vector4(-1f, -1f, 1f, 1f);

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
        this.brushManager = get(BrushManager.class);
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
        this.worldEditorViewSystem = get(WorldEditorViewSystem.class);
        this.worldEditorToolSystem = get(WorldEditorToolSystem.class);
        this.worldEditorLayerSystem = get(WorldEditorLayerSystem.class);
        this.placementIconSystem = get(PlacementIconSystem.class);
    }

    @Override
    protected void awake() {

        this.quadMesh = meshManager.getMeshHandleFromMeshName(WorldEditorSetting.MESH_QUAD);
        this.markerMaterialID = materialManager.getMaterialIDFromMaterialName(WorldEditorSetting.MATERIAL_MARKER);

        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, context.getWindow());
        this.imageModel = modelManager.createModel(
                quadMesh, materialManager.cloneMaterial(WorldEditorSetting.MATERIAL_IMAGE));
        this.heightModel = modelManager.createModel(
                quadMesh, materialManager.cloneMaterial(WorldEditorSetting.MATERIAL_HEIGHTS));
        this.brushModel = modelManager.createModel(
                quadMesh, materialManager.cloneMaterial(WorldEditorSetting.MATERIAL_BRUSH));
        this.footprintModel = modelManager.createModel(
                quadMesh, materialManager.cloneMaterial(WorldEditorSetting.MATERIAL_BRUSH));
        this.boxModel = modelManager.createModel(
                quadMesh, materialManager.cloneMaterial(WorldEditorSetting.MATERIAL_BRUSH));
        this.globeModel = modelManager.createModel(
                quadMesh, materialManager.cloneMaterial(WorldEditorSetting.MATERIAL_GLOBE));

        fadeHeights();
        outlineBox();
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        ImageDocumentInstance image = worldEditorManager.getWorldImage();
        int texture = imageManager.useTexture(image);

        if (texture != 0 && window.getWidth() > 0 && window.getHeight() > 0) {

            if (worldEditorViewSystem.isShown())
                renderGlobe(window, image, texture);
            else {
                renderImage(window, image, texture);
                renderHeights(window);
                renderBrush(window);
                renderFootprint(window);
                renderBox(window);
            }

            renderMarkers(window);
        }

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    // Image \\

    private void renderImage(WindowInstance window, ImageDocumentInstance image, int texture) {

        setViewRect(window, 0.0, 0.0, image.getWidth(), image.getHeight());

        if (!worldEditorLayerSystem.isShown(WorldEditorLayer.BIOMES))
            return;

        MaterialInstance material = imageModel.getMaterial();
        material.setUniform(WorldEditorSetting.UNIFORM_VIEW_RECT, viewRect);
        material.setUniform(WorldEditorSetting.UNIFORM_UV_RECT, uvRect);
        material.setUniform(WorldEditorSetting.UNIFORM_TILE_TEXTURE, texture);

        renderManager.pushRenderCall(imageModel, sceneFbo, WorldEditorSetting.DEPTH_IMAGE, window);
    }

    // Globe \\

    // The whole ball in one pass: both layers as shown, the brush, footprint and box, and the cube while shown
    private void renderGlobe(WindowInstance window, ImageDocumentInstance image, int texture) {

        MaterialInstance material = globeModel.getMaterial();
        int heightTexture = imageManager.useTexture(worldEditorManager.getHeightImage());
        boolean heightsShown = heightTexture != 0 && worldEditorLayerSystem.isShown(WorldEditorLayer.HEIGHTS);

        material.setUniform(WorldEditorSetting.UNIFORM_VIEW_RECT, windowRect);
        material.setUniform(WorldEditorSetting.UNIFORM_TILE_TEXTURE, texture);
        material.setUniform(WorldEditorSetting.UNIFORM_HEIGHT_TEXTURE, heightsShown ? heightTexture : texture);
        material.setUniform(WorldEditorSetting.UNIFORM_SHOW_BIOMES,
                toSwitch(worldEditorLayerSystem.isShown(WorldEditorLayer.BIOMES)));
        material.setUniform(WorldEditorSetting.UNIFORM_SHOW_HEIGHTS, toSwitch(heightsShown));
        material.setUniform(WorldEditorSetting.UNIFORM_OVERLAY_ALPHA, WorldEditorSetting.HEIGHT_OVERLAY_ALPHA);
        material.setUniform(WorldEditorSetting.UNIFORM_FACE_TEXELS, (float) image.getHeight());
        GlobeViewStruct globeView = worldEditorViewSystem.getGlobeView();

        material.setUniform(WorldEditorSetting.UNIFORM_GLOBE_CAMERA, globeView.getPosition());
        material.setUniform(WorldEditorSetting.UNIFORM_GLOBE_FORWARD, globeView.getForward());
        material.setUniform(WorldEditorSetting.UNIFORM_GLOBE_RIGHT, globeView.getRight());
        material.setUniform(WorldEditorSetting.UNIFORM_GLOBE_UP, globeView.getUp());

        aimGlobeBrush(material, image.getHeight());
        aimGlobeFootprint(material, image.getHeight());
        aimGlobeBox(material);
        aimGlobeCube(material);

        renderManager.pushRenderCall(globeModel, sceneFbo, WorldEditorSetting.DEPTH_IMAGE, window);
    }

    // The brush on the plane of the face the hovered pixel lies on, centred on that pixel, as a stroke stamps it
    private void aimGlobeBrush(MaterialInstance material, int faceTexels) {

        WorldEditorLayer layer = worldEditorToolSystem.getLayer();
        boolean shown = worldEditorToolSystem.isHovering() && !worldEditorToolSystem.isDropping()
                && worldEditorManager.isPainting(layer) && worldEditorManager.getTool(layer).isStroke();

        if (!shown) {
            material.setUniform(WorldEditorSetting.UNIFORM_BRUSH_FACE, WorldEditorSetting.GLOBE_NO_FACE);
            return;
        }

        int pixelX = worldEditorToolSystem.getHoveredX();
        int face = Math.floorDiv(pixelX, faceTexels);
        BrushHandle brush = brushManager.getActiveBrush();

        brushCenter.set(
                (float) (pixelX - face * faceTexels + EngineSetting.PIXEL_CENTER_OFFSET),
                (float) (worldEditorToolSystem.getHoveredY() + EngineSetting.PIXEL_CENTER_OFFSET));

        material.setUniform(WorldEditorSetting.UNIFORM_BRUSH_FACE, (float) face);
        material.setUniform(WorldEditorSetting.UNIFORM_BRUSH_CENTER, brushCenter);
        material.setUniform(WorldEditorSetting.UNIFORM_BRUSH_RADIUS,
                (float) (worldEditorManager.getBrushRadius(layer) + EngineSetting.PIXEL_CENTER_OFFSET));
        material.setUniform(WorldEditorSetting.UNIFORM_BRUSH_SHAPE, (float) brush.getShape().ordinal());
        material.setUniform(WorldEditorSetting.UNIFORM_BRUSH_RATIO, brush.getValue(BrushField.RATIO));
        material.setUniform(
                WorldEditorSetting.UNIFORM_BRUSH_ANGLE,
                (float) Math.toRadians(brush.getValue(BrushField.ANGLE)));
    }

    // The design's reach about the exact point under the pointer, on the plane of the face it lies on
    private void aimGlobeFootprint(MaterialInstance material, int faceTexels) {

        int radiusBlocks = settlementPlannerManager.getFootprintRadiusBlocks();
        boolean shown = worldEditorToolSystem.isHovering() && isStanding() && radiusBlocks > 0;
        double imageX = worldEditorToolSystem.getHoveredImageX();
        int face = Math.floorDiv(worldEditorToolSystem.getHoveredX(), faceTexels);

        footprintCenter.set(
                (float) (imageX - face * (double) faceTexels),
                (float) worldEditorToolSystem.getHoveredImageY());

        material.setUniform(WorldEditorSetting.UNIFORM_FOOTPRINT_FACE, (float) face);
        material.setUniform(WorldEditorSetting.UNIFORM_FOOTPRINT_CENTER, footprintCenter);
        material.setUniform(WorldEditorSetting.UNIFORM_FOOTPRINT_RADIUS,
                shown ? (float) (radiusBlocks / WorldImagePointStruct.resolvePixelBlocks()) : 0f);
    }

    private void aimGlobeBox(MaterialInstance material) {

        boolean boxing = worldEditorToolSystem.isBoxing();
        double pressX = worldEditorToolSystem.getPressImageX();
        double pressY = worldEditorToolSystem.getPressImageY();
        double hoveredX = worldEditorToolSystem.getHoveredImageX();
        double hoveredY = worldEditorToolSystem.getHoveredImageY();

        boxRect.set(
                (float) Math.min(pressX, hoveredX),
                (float) Math.min(pressY, hoveredY),
                (float) Math.max(pressX, hoveredX),
                (float) Math.max(pressY, hoveredY));

        material.setUniform(WorldEditorSetting.UNIFORM_SHOW_BOX, toSwitch(boxing));
        material.setUniform(WorldEditorSetting.UNIFORM_BOX_RECT, boxRect);
    }

    // The block grid's cube as it is shown, turning under a drag, while the Cube layer is shown
    private void aimGlobeCube(MaterialInstance material) {

        Matrix3Double globeFromCube = worldEditorManager.getShownFromCube();

        cubeAxisX.set((float) globeFromCube.getM00(), (float) globeFromCube.getM10(), (float) globeFromCube.getM20());
        cubeAxisY.set((float) globeFromCube.getM01(), (float) globeFromCube.getM11(), (float) globeFromCube.getM21());
        cubeAxisZ.set((float) globeFromCube.getM02(), (float) globeFromCube.getM12(), (float) globeFromCube.getM22());

        material.setUniform(WorldEditorSetting.UNIFORM_SHOW_CUBE,
                toSwitch(worldEditorLayerSystem.isShown(WorldEditorLayer.CUBE)));
        material.setUniform(WorldEditorSetting.UNIFORM_CUBE_AXIS_X, cubeAxisX);
        material.setUniform(WorldEditorSetting.UNIFORM_CUBE_AXIS_Y, cubeAxisY);
        material.setUniform(WorldEditorSetting.UNIFORM_CUBE_AXIS_Z, cubeAxisZ);
    }

    private static float toSwitch(boolean on) {
        return on ? 1f : 0f;
    }

    // Heights \\

    // The overlay's opacity never changes, so it is set once on its own material
    private void fadeHeights() {
        heightModel.getMaterial().setUniform(
                WorldEditorSetting.UNIFORM_OVERLAY_ALPHA, WorldEditorSetting.HEIGHT_OVERLAY_ALPHA);
    }

    // Laid over the whole image with the view rectangle the image was just drawn with
    private void renderHeights(WindowInstance window) {

        if (!worldEditorLayerSystem.isShown(WorldEditorLayer.HEIGHTS))
            return;

        int texture = imageManager.useTexture(worldEditorManager.getHeightImage());

        if (texture == 0)
            return;

        MaterialInstance material = heightModel.getMaterial();
        material.setUniform(WorldEditorSetting.UNIFORM_VIEW_RECT, viewRect);
        material.setUniform(WorldEditorSetting.UNIFORM_UV_RECT, uvRect);
        material.setUniform(WorldEditorSetting.UNIFORM_TILE_TEXTURE, texture);

        renderManager.pushRenderCall(heightModel, sceneFbo, WorldEditorSetting.DEPTH_HEIGHTS, window);
    }

    // Brush \\

    // Drawn in the preset's own shape, turn and squash, so the outline is exactly the tip a stroke stamps
    private void renderBrush(WindowInstance window) {

        WorldEditorLayer layer = worldEditorToolSystem.getLayer();

        if (!worldEditorToolSystem.isHovering() || worldEditorToolSystem.isDropping()
                || !worldEditorManager.isPainting(layer) || !worldEditorManager.getTool(layer).isStroke())
            return;

        int radius = worldEditorManager.getBrushRadius(layer);
        int pixelX = worldEditorToolSystem.getHoveredX();
        int pixelY = worldEditorToolSystem.getHoveredY();
        BrushHandle brush = brushManager.getActiveBrush();
        MaterialInstance material = brushModel.getMaterial();

        setViewRect(window, pixelX - radius, pixelY - radius, pixelX + radius + 1, pixelY + radius + 1);
        material.setUniform(WorldEditorSetting.UNIFORM_VIEW_RECT, viewRect);
        material.setUniform(WorldEditorSetting.UNIFORM_BRUSH_SHAPE, (float) brush.getShape().ordinal());
        material.setUniform(WorldEditorSetting.UNIFORM_BRUSH_RATIO, brush.getValue(BrushField.RATIO));
        material.setUniform(
                WorldEditorSetting.UNIFORM_BRUSH_ANGLE,
                (float) Math.toRadians(brush.getValue(BrushField.ANGLE)));

        renderManager.pushRenderCall(brushModel, sceneFbo, WorldEditorSetting.DEPTH_BRUSH, window);
    }

    // Box \\

    // The box is always square-cornered, so its shape is set once on its own material
    private void outlineBox() {
        boxModel.getMaterial().setUniform(WorldEditorSetting.UNIFORM_BRUSH_SHAPE, (float) BrushShape.SQUARE.ordinal());
    }

    private void renderBox(WindowInstance window) {

        if (!worldEditorToolSystem.isBoxing())
            return;

        double pressX = worldEditorToolSystem.getPressImageX();
        double pressY = worldEditorToolSystem.getPressImageY();
        double hoveredX = worldEditorToolSystem.getHoveredImageX();
        double hoveredY = worldEditorToolSystem.getHoveredImageY();

        setViewRect(
                window,
                Math.min(pressX, hoveredX),
                Math.min(pressY, hoveredY),
                Math.max(pressX, hoveredX),
                Math.max(pressY, hoveredY));
        boxModel.getMaterial().setUniform(WorldEditorSetting.UNIFORM_VIEW_RECT, viewRect);

        renderManager.pushRenderCall(boxModel, sceneFbo, WorldEditorSetting.DEPTH_BRUSH, window);
    }

    // Footprint \\

    // How far the Settlement Planner's design would stand out from the column under the pointer
    private void renderFootprint(WindowInstance window) {

        int radiusBlocks = settlementPlannerManager.getFootprintRadiusBlocks();

        if (!worldEditorToolSystem.isHovering() || !isStanding() || radiusBlocks <= 0)
            return;

        double radius = radiusBlocks / WorldImagePointStruct.resolvePixelBlocks();
        double pixelX = worldEditorToolSystem.getHoveredImageX();
        double pixelY = worldEditorToolSystem.getHoveredImageY();

        setViewRect(window, pixelX - radius, pixelY - radius, pixelX + radius, pixelY + radius);
        footprintModel.getMaterial().setUniform(WorldEditorSetting.UNIFORM_VIEW_RECT, viewRect);

        renderManager.pushRenderCall(footprintModel, sceneFbo, WorldEditorSetting.DEPTH_BRUSH, window);
    }

    // True while a click or a drop would stand the design under the pointer
    private boolean isStanding() {

        WorldEditorLayer layer = worldEditorToolSystem.getLayer();

        return worldEditorToolSystem.isDropping()
                || layer.isPlacing()
                        && worldEditorManager.getMode() == WorldEditorMode.SETTLEMENTS
                        && worldEditorManager.getTool(layer) == EditorTool.PLACE;
    }

    // Markers \\

    private void renderMarkers(WindowInstance window) {

        if (!worldEditorToolSystem.getLayer().isPlacing())
            return;

        WorldHandle worldHandle = worldEditorManager.getWorldHandle();

        renderPlacementIcons(window, worldHandle);

        if (showingPlayers)
            renderPlayerMarkers(window, worldHandle);
    }

    // Every hand placement's icon over the image pixel it stands in, the selection where a drag would stand it; on a
    // globe a placement on the ball's far side shows none
    private void renderPlacementIcons(WindowInstance window, WorldHandle worldHandle) {

        WorldPlacementStruct[] placements = worldEditorManager.getPlacements();
        boolean moving = worldEditorToolSystem.isMoving();
        double moveX = worldEditorToolSystem.getHoveredImageX() - worldEditorToolSystem.getPressImageX();
        double moveY = worldEditorToolSystem.getHoveredImageY() - worldEditorToolSystem.getPressImageY();

        placementIconSystem.begin();

        for (int i = 0; i < placements.length; i++) {

            boolean moved = moving && worldEditorManager.isPlacementSelected(placements[i]);

            imagePoint.locateImage(worldHandle, placements[i].getWorldX(), placements[i].getWorldZ());

            if (!worldEditorViewSystem.project(
                    imagePoint.getImageX() + (moved ? moveX : 0.0),
                    imagePoint.getImageY() + (moved ? moveY : 0.0)))
                continue;

            placementIconSystem.push(
                    window, sceneFbo, WorldEditorSetting.DEPTH_MARKERS,
                    worldEditorViewSystem.getProjectedX(),
                    worldEditorViewSystem.getProjectedY(),
                    placements[i]);
        }
    }

    private void renderPlayerMarkers(WindowInstance window, WorldHandle worldHandle) {

        int count = mapManager.resolveMarkers(worldHandle, markers);
        int drawn = 0;

        for (int i = 0; i < count; i++) {

            MapMarkerStruct marker = markers.get(i);

            if (pushMarker(window, worldHandle, drawn, marker.getX(), marker.getZ(),
                    marker.getHeadingX(), marker.getHeadingZ()))
                drawn++;
        }
    }

    // One arrow at a world position and heading, skipped when it falls off the window
    private boolean pushMarker(
            WindowInstance window,
            WorldHandle worldHandle,
            int index,
            double worldX,
            double worldZ,
            float headingX,
            float headingZ) {

        float width = window.getWidth();
        float height = window.getHeight();
        float size = WorldEditorSetting.MARKER_SIZE_PIXELS;

        imagePoint.locateImage(worldHandle, worldX, worldZ);

        if (!worldEditorViewSystem.project(imagePoint.getImageX(), imagePoint.getImageY()))
            return false;

        float screenX = worldEditorViewSystem.getProjectedX();
        float screenY = worldEditorViewSystem.getProjectedY();

        if (screenX < -size || screenY < -size || screenX > width + size || screenY > height + size)
            return false;

        markerScale.set(size / width * 2f, size / height * 2f);
        markerCenter.set(screenX / width * 2f - 1f, screenY / height * 2f - 1f);
        markerDirection.set(headingX, -headingZ);

        ModelInstance markerModel = acquireMarkerModel(index);
        MaterialInstance material = markerModel.getMaterial();
        material.setUniform(WorldEditorSetting.UNIFORM_MARKER_CENTER, markerCenter);
        material.setUniform(WorldEditorSetting.UNIFORM_MARKER_DIRECTION, markerDirection);
        material.setUniform(WorldEditorSetting.UNIFORM_MARKER_SCALE, markerScale);

        renderManager.pushRenderCall(markerModel, sceneFbo, WorldEditorSetting.DEPTH_MARKERS, window);

        return true;
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
