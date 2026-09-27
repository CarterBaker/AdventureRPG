package application.bootstrap.worldpipeline.worldrendermanager;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.macrochunk.MacroChunkInstance;
import application.bootstrap.worldpipeline.macrochunk.MacroDataSyncContainer;
import application.bootstrap.worldpipeline.util.MacroTerrainUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

class MacroRenderSystem extends SystemPackage {

    /*
     * Owns the GPU side of every macro chunk: the position UBO placing it
     * against its grid's active chunk, its one mesh and model, and its draw
     * submission. A tile is drawn only while its highest ground still clears
     * the horizon from the current eye height and it faces the camera. A
     * reupload writes into the macro's existing buffers, a hidden macro keeps
     * them for its next tenant, and only disposal frees them, so streaming
     * macros in and out never churns GPU objects.
     */

    // Internal
    private UBOManager uboManager;
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private VAOManager vaoManager;
    private RenderManager renderManager;
    private FrustumCullingSystem frustumCullingSystem;

    // Handles
    private UBOHandle gridCoordinateBase;
    private VAOHandle macroVAO;
    private int macroMaterialID;

    // Settings
    private int chunkSize;
    private float halfMacroChunks;
    private float halfDiagonalChunks;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.halfMacroChunks = EngineSetting.MACRO_CHUNK_SIZE / 2f;
        this.halfDiagonalChunks = (float) Math.sqrt(2.0) * halfMacroChunks;
    }

    @Override
    protected void get() {

        // Internal
        this.uboManager = get(UBOManager.class);
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.vaoManager = get(VAOManager.class);
        this.renderManager = get(RenderManager.class);
        this.frustumCullingSystem = get(FrustumCullingSystem.class);
    }

    @Override
    protected void awake() {
        this.gridCoordinateBase = uboManager.getUBOHandleFromUBOName(EngineSetting.GRID_COORDINATE_UBO);
    }

    @Override
    protected void start() {
        this.macroVAO = vaoManager.getVAOHandleFromVAOName(EngineSetting.MACRO_VAO);
        this.macroMaterialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.MACRO_MATERIAL);
    }

    // Placement \\

    void placeMacro(MacroChunkInstance macro, GridInstance grid) {

        UBOInstance positionUBO = macro.getPositionUBO();

        if (positionUBO == null) {
            positionUBO = uboManager.createUBOInstance(gridCoordinateBase);
            macro.setPositionUBO(positionUBO);
        }

        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                grid.getWorldHandle(),
                grid.getActiveChunkCoordinate(),
                macro.getCoordinate());

        int deltaX = Coordinate2Long.unpackX(delta);
        int deltaZ = Coordinate2Long.unpackY(delta);

        float centerX = deltaX + halfMacroChunks;
        float centerZ = deltaZ + halfMacroChunks;
        float distanceSq = centerX * centerX + centerZ * centerZ;
        float distance = (float) Math.sqrt(distanceSq);

        float angularRadius = distance > halfDiagonalChunks
                ? (float) Math.atan(halfDiagonalChunks / distance)
                : EngineSetting.FRUSTUM_PI;

        macro.setPlacement((float) Math.atan2(centerZ, centerX), angularRadius);

        positionUBO.updateUniform(EngineSetting.UNIFORM_GRID_POSITION,
                new Vector2(deltaX * chunkSize, deltaZ * chunkSize));
        positionUBO.updateUniform(EngineSetting.UNIFORM_DISTANCE_FROM_CENTER, distanceSq);
        uboManager.push(positionUBO);
    }

    // Upload \\

    void uploadMacro(MacroChunkInstance macro) {

        MacroDataSyncContainer sync = macro.getMacroDataSyncContainer();
        MeshInstance meshInstance = macro.getMeshInstance();

        macro.setRendered(true);
        macro.setHasGeometry(!sync.getIndices().isEmpty());

        if (!macro.hasGeometry())
            return;

        if (meshInstance == null) {

            meshInstance = meshManager.createMesh(macroVAO, sync.getVertices(), sync.getIndices());

            MaterialInstance material = materialManager.cloneMaterial(macroMaterialID);
            material.setUBO(macro.getPositionUBO());

            macro.setModel(meshInstance, modelManager.createModel(meshInstance, material));
        } else {
            meshManager.updateMesh(meshInstance, sync.getVertices(), sync.getIndices());
            macro.getModelInstance().updateMeshData(meshInstance.getMeshData());
        }
    }

    // Removal \\

    void hideMacro(MacroChunkInstance macro) {
        macro.setRendered(false);
        macro.setHasGeometry(false);
    }

    void disposeMacro(MacroChunkInstance macro) {

        ModelInstance modelInstance = macro.getModelInstance();

        if (modelInstance != null) {
            modelManager.removeMesh(modelInstance);
            macro.clearModel();
        }

        UBOInstance positionUBO = macro.getPositionUBO();

        if (positionUBO != null) {
            uboManager.destroyInstance(positionUBO);
            macro.setPositionUBO(null);
        }

        macro.setRendered(false);
        macro.setHasGeometry(false);
    }

    // Render \\

    void renderGridMacros(GridInstance grid, WindowInstance window, FBOInstance worldFbo) {

        Long2ObjectLinkedOpenHashMap<MacroChunkInstance> activeMacroChunks = grid.getActiveMacroChunks();
        ObjectIterator<Long2ObjectMap.Entry<MacroChunkInstance>> iterator = activeMacroChunks
                .long2ObjectEntrySet()
                .fastIterator();

        float eyeReachBlocks = MacroTerrainUtility.resolveEyeReachBlocks(grid);

        while (iterator.hasNext()) {

            MacroChunkInstance macro = iterator.next().getValue();

            if (!macro.hasGeometry())
                continue;

            if (macro.getNearestDistanceBlocks() > eyeReachBlocks + macro.getHorizonReachBlocks())
                continue;

            if (!frustumCullingSystem.isMacroVisible(macro.getAngleFromCenter(), macro.getAngularRadius()))
                continue;

            renderManager.pushRenderCall(
                    macro.getModelInstance(), worldFbo, EngineSetting.DEFAULT_RENDER_DEPTH, window);
        }
    }
}
