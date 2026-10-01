package application.bootstrap.worldpipeline.worldrendermanager;

import java.util.Arrays;

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
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4Int;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

class MacroRenderSystem extends SystemPackage {

    /*
     * Owns the GPU side of every macro chunk: the position UBO placing it
     * against its grid's active chunk, the coverage UBO marking which of its
     * chunks the grid already draws, its one mesh and model, and its draw
     * submission. A tile is drawn whenever it faces the camera, and its
     * coverage is re-resolved before drawing only when the grid's anchor or
     * drawn chunks changed, so macro terrain fills exactly the ground the
     * chunk grid leaves open and meets it with no hole and no overlap. A
     * reupload writes into the macro's existing buffers, a hidden macro keeps
     * them for its next tenant, and only disposal frees them, so streaming
     * macros in and out never churns GPU objects.
     */

    // Internal
    private WorldRenderManager worldRenderManager;
    private UBOManager uboManager;
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private VAOManager vaoManager;
    private RenderManager renderManager;
    private FrustumCullingSystem frustumCullingSystem;

    // Handles
    private UBOHandle gridCoordinateBase;
    private UBOHandle macroCoverageBase;
    private VAOHandle macroVAO;
    private int macroMaterialID;

    // Settings
    private int chunkSize;
    private int macroChunkSize;
    private int coverageBitsPerWord;
    private float halfMacroChunks;
    private float halfDiagonalChunks;

    // Coverage
    private int[] coverageScratch;
    private Vector4Int[] coverageVectors;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.macroChunkSize = EngineSetting.MACRO_CHUNK_SIZE;
        this.coverageBitsPerWord = EngineSetting.MACRO_COVERAGE_BITS_PER_WORD;
        this.halfMacroChunks = macroChunkSize / 2f;
        this.halfDiagonalChunks = (float) Math.sqrt(2.0) * halfMacroChunks;

        // Coverage
        this.coverageScratch = new int[EngineSetting.MACRO_COVERAGE_WORD_COUNT];
        this.coverageVectors = new Vector4Int[EngineSetting.MACRO_COVERAGE_VECTOR_COUNT];

        for (int i = 0; i < coverageVectors.length; i++)
            coverageVectors[i] = new Vector4Int();
    }

    @Override
    protected void get() {

        // Internal
        this.worldRenderManager = get(WorldRenderManager.class);
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
        this.macroCoverageBase = uboManager.getUBOHandleFromUBOName(EngineSetting.MACRO_COVERAGE_UBO);
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
            macro.setCoverageUBO(uboManager.createUBOInstance(macroCoverageBase));
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
            material.setUBO(macro.getCoverageUBO());

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
            uboManager.destroyInstance(macro.getCoverageUBO());
            macro.setPositionUBO(null);
            macro.setCoverageUBO(null);
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

        while (iterator.hasNext()) {

            MacroChunkInstance macro = iterator.next().getValue();

            if (!macro.hasGeometry())
                continue;

            if (!frustumCullingSystem.isMacroVisible(macro.getAngleFromCenter(), macro.getAngularRadius()))
                continue;

            refreshCoverage(macro, grid);
            renderManager.pushRenderCall(
                    macro.getModelInstance(), worldFbo, EngineSetting.DEFAULT_RENDER_DEPTH, window);
        }
    }

    // Coverage \\

    private void refreshCoverage(MacroChunkInstance macro, GridInstance grid) {

        long anchorCoordinate = grid.getActiveChunkCoordinate();
        int drawnRevision = worldRenderManager.getDrawnRevision();

        if (macro.isCoverageCurrent(anchorCoordinate, drawnRevision))
            return;

        resolveCoverage(macro, grid);
        macro.setCoverageCurrent(anchorCoordinate, drawnRevision);

        int[] coverageWords = macro.getCoverageWords();

        if (Arrays.equals(coverageWords, coverageScratch))
            return;

        System.arraycopy(coverageScratch, 0, coverageWords, 0, coverageWords.length);
        pushCoverage(macro);
    }

    // Only the stretch of the tile inside the grid's square footprint can hold a drawn chunk
    private void resolveCoverage(MacroChunkInstance macro, GridInstance grid) {

        Arrays.fill(coverageScratch, 0);

        long coordinate = macro.getCoordinate();
        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                grid.getWorldHandle(),
                grid.getActiveChunkCoordinate(),
                coordinate);

        int gridHalf = settings.maxRenderDistance / 2;
        int deltaX = Coordinate2Long.unpackX(delta);
        int deltaZ = Coordinate2Long.unpackY(delta);
        int firstX = Math.max(-gridHalf - deltaX, 0);
        int firstZ = Math.max(-gridHalf - deltaZ, 0);
        int endX = Math.min(gridHalf - deltaX, macroChunkSize);
        int endZ = Math.min(gridHalf - deltaZ, macroChunkSize);

        int originX = Coordinate2Long.unpackX(coordinate);
        int originZ = Coordinate2Long.unpackY(coordinate);

        for (int z = firstZ; z < endZ; z++) {
            for (int x = firstX; x < endX; x++) {

                if (!worldRenderManager.isChunkDrawn(grid, Coordinate2Long.pack(originX + x, originZ + z)))
                    continue;

                int index = z * macroChunkSize + x;
                coverageScratch[index / coverageBitsPerWord] |= 1 << (index % coverageBitsPerWord);
            }
        }
    }

    private void pushCoverage(MacroChunkInstance macro) {

        int[] coverageWords = macro.getCoverageWords();
        int componentCount = EngineSetting.VECTOR4_COMPONENT_COUNT;

        for (int i = 0; i < coverageVectors.length; i++) {
            int word = i * componentCount;
            coverageVectors[i].set(
                    coverageWords[word],
                    coverageWords[word + 1],
                    coverageWords[word + 2],
                    coverageWords[word + 3]);
        }

        UBOInstance coverageUBO = macro.getCoverageUBO();

        coverageUBO.updateUniform(EngineSetting.UNIFORM_MACRO_COVERAGE, coverageVectors);
        uboManager.push(coverageUBO);
    }
}
