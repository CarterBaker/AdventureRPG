package application.bootstrap.worldpipeline.worldrendermanager;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.macrochunk.MacroChunkInstance;
import application.bootstrap.worldpipeline.macrochunk.MacroDataSyncContainer;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4Int;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

class MacroRenderSystem extends SystemPackage {

    /*
     * Owns the GPU side of every macro chunk: the position UBO placing it
     * against its grid's active chunk, the coverage UBO marking which of its
     * chunks the grid already draws, its one land mesh and model, and its
     * draw submission through the shared surface push, so it takes the same
     * cloud shadows as the chunks beside it. A tile is drawn whenever it
     * faces the camera, and its coverage is re-resolved before drawing only
     * when the grid's anchor or drawn chunks changed, so macro terrain fills
     * exactly the ground the chunk grid leaves open and meets it with no hole
     * and no overlap. A tile built again keeps drawing what it drew until its
     * new land and canopy are uploaded, then takes them at once, written into
     * the macro's existing buffers, never dithered: two broad surfaces
     * interleaved pixel by pixel read to the lighting pass as edges
     * everywhere and darken the whole tile. A hidden macro keeps its buffers
     * for its next tenant, and only disposal frees them. A
     * tile's trees are models of their own beside its land: its canopy and
     * stand-ins in the land's layout and material, one model swapped whole,
     * and near the grid the
     * models of the trees themselves in the grid's own bark and leaf
     * materials, placed by the same position UBO and marked far, each
     * yielding while the grid draws the chunk its tree roots in, as the
     * land does. A tile built again keeps the trees it drew before for
     * TREE_FADE_SECONDS, falling away through that dither as its new ones
     * rise through it, as a chunk's or mega's trees do, so no tree ever
     * changes its look at once; its trees are made again only when it is
     * built again, and given up whenever it is hidden. Its trees at impostor
     * levels it hands to TreeManager as instances, taken back when it is
     * hidden or disposed.
     */

    // Internal
    private WorldRenderManager worldRenderManager;
    private UBOManager uboManager;
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private VAOManager vaoManager;
    private TreeManager treeManager;
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

    // Scratch
    private Vector2 gridPositionScratch;
    private ObjectArrayList<MeshInstance> treeMeshScratch;

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

        // Scratch
        this.gridPositionScratch = new Vector2();
        this.treeMeshScratch = new ObjectArrayList<>();
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
        this.treeManager = get(TreeManager.class);
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

        // A fresh buffer holds undefined contents until written, so it starts from the words it mirrors
        if (positionUBO == null) {
            positionUBO = uboManager.createUBOInstance(gridCoordinateBase);
            macro.setPositionUBO(positionUBO);
            macro.setCoverageUBO(uboManager.createUBOInstance(macroCoverageBase));
            pushCoverage(macro);
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
                gridPositionScratch.set(deltaX * chunkSize, deltaZ * chunkSize));
        positionUBO.updateUniform(EngineSetting.UNIFORM_DISTANCE_FROM_CENTER, distanceSq);
        uboManager.push(positionUBO);
    }

    // Upload \\

    void uploadMacro(MacroChunkInstance macro) {

        MacroDataSyncContainer sync = macro.getMacroDataSyncContainer();
        MeshInstance meshInstance = macro.getMeshInstance();

        boolean drawn = macro.hasGeometry();

        macro.setRendered(true);
        macro.setHasGeometry(!sync.getIndices().isEmpty());
        uploadTreeModels(macro, sync, drawn);
        treeManager.setMacroImpostorSites(macro.getCoordinate(), sync.getTreeImpostors());

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

    // The canopy and stand-ins a tile drew until now given up
    private void disposeCanopy(MacroChunkInstance macro) {

        ModelInstance canopy = macro.getCanopyModelInstance();

        if (canopy == null)
            return;

        modelManager.removeMesh(canopy);
        macro.setCanopyModelInstance(null);
    }

    // Tree Models \\

    // The tile's new trees rise over the ones it drew until now, which fall away; a tile not drawn until now has none
    // to fall away. Its canopy and stand-ins, which cover the ground wide, take their new place at once: dithered over
    // their old ones they would interleave two surfaces pixel by pixel, which the lighting pass inks as edges and so
    // darkens across the whole tile
    private void uploadTreeModels(MacroChunkInstance macro, MacroDataSyncContainer sync, boolean drawn) {

        disposeModels(macro.getFadingTreeModels());

        if (drawn)
            macro.getFadingTreeModels().addAll(macro.getTreeModels());
        else
            disposeModels(macro.getTreeModels());

        macro.getTreeModels().clear();
        macro.setTreeFadeAge(0f);

        disposeCanopy(macro);
        addCanopyModel(macro, sync);
        addTreeModels(macro, sync.getTreeBark(), treeManager.getBarkMaterialID());
        addTreeModels(macro, sync.getTreeLeaves(), treeManager.getLeafMaterialID());
    }

    // The canopy and stand-ins in the land's layout and material, under the tile's own coverage
    private void addCanopyModel(MacroChunkInstance macro, MacroDataSyncContainer sync) {

        if (sync.getTreeIndices().isEmpty())
            return;

        MeshInstance meshInstance = meshManager.createMesh(macroVAO, sync.getTreeVertices(), sync.getTreeIndices());
        MaterialInstance material = materialManager.cloneMaterial(macroMaterialID);

        material.setUBO(macro.getPositionUBO());
        material.setUBO(macro.getCoverageUBO());
        material.setUniform(EngineSetting.UNIFORM_TREE_FADE, EngineSetting.TREE_FADE_SHOWN);

        macro.setCanopyModelInstance(modelManager.createModel(meshInstance, material));
    }

    private void addTreeModels(MacroChunkInstance macro, FloatArrayList vertices, int materialID) {

        if (vertices.isEmpty())
            return;

        treeManager.uploadTreeMeshes(vertices, treeMeshScratch);

        ObjectArrayList<ModelInstance> treeModels = macro.getTreeModels();

        for (int i = 0; i < treeMeshScratch.size(); i++) {

            MaterialInstance material = materialManager.cloneMaterial(materialID);
            material.setUBO(macro.getPositionUBO());
            material.setUBO(macro.getCoverageUBO());
            material.setUniform(EngineSetting.UNIFORM_TREE_FADE, EngineSetting.TREE_FADE_SHOWN);
            material.setUniform(EngineSetting.UNIFORM_TREE_FAR, EngineSetting.TREE_FAR_ON);

            treeModels.add(modelManager.createModel(treeMeshScratch.get(i), material));
        }

        treeMeshScratch.clear();
    }

    private void disposeTreeModels(MacroChunkInstance macro) {
        disposeModels(macro.getTreeModels());
        disposeModels(macro.getFadingTreeModels());
    }

    private void disposeModels(ObjectArrayList<ModelInstance> models) {

        for (int i = 0; i < models.size(); i++)
            modelManager.removeMesh(models.get(i));

        models.clear();
    }

    // The fade between a tile's old trees and its new ones run on by a frame, the old ones given up once it ends
    private void advanceTreeFade(MacroChunkInstance macro) {

        int step = worldRenderManager.resolveTreeFadeStep(macro.getTreeFadeAge());

        if (step == EngineSetting.TREE_FADE_STEPS)
            disposeModels(macro.getFadingTreeModels());
        else
            macro.setTreeFadeAge(macro.getTreeFadeAge() + internal.getDeltaTime());

        applyTreeFade(macro.getTreeModels(), worldRenderManager.getTreeFade(step, true));
        applyTreeFade(macro.getFadingTreeModels(), worldRenderManager.getTreeFade(step, false));
    }

    private void applyTreeFade(ObjectArrayList<ModelInstance> models, Float fade) {
        for (int i = 0; i < models.size(); i++)
            models.get(i).getMaterial().setUniform(EngineSetting.UNIFORM_TREE_FADE, fade);
    }

    private void pushModels(
            ObjectArrayList<ModelInstance> models,
            GridInstance grid,
            WindowInstance window,
            FBOInstance worldFbo) {

        for (int i = 0; i < models.size(); i++)
            worldRenderManager.pushSurfaceModel(models.get(i), grid, worldFbo, window);
    }

    // Removal \\

    void hideMacro(MacroChunkInstance macro) {
        disposeTreeModels(macro);
        disposeCanopy(macro);
        treeManager.removeMacroImpostorSites(macro.getCoordinate());
        macro.setRendered(false);
        macro.setHasGeometry(false);
    }

    void disposeMacro(MacroChunkInstance macro) {

        ModelInstance modelInstance = macro.getModelInstance();

        if (modelInstance != null) {
            modelManager.removeMesh(modelInstance);
            macro.clearModel();
        }

        disposeTreeModels(macro);
        disposeCanopy(macro);
        treeManager.removeMacroImpostorSites(macro.getCoordinate());

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

            advanceTreeFade(macro);

            if (!frustumCullingSystem.isMacroVisible(macro.getAngleFromCenter(), macro.getAngularRadius()))
                continue;

            refreshCoverage(macro, grid);
            worldRenderManager.pushSurfaceModel(macro.getModelInstance(), grid, worldFbo, window);

            if (macro.getCanopyModelInstance() != null)
                worldRenderManager.pushSurfaceModel(macro.getCanopyModelInstance(), grid, worldFbo, window);

            pushModels(macro.getTreeModels(), grid, window, worldFbo);
            pushModels(macro.getFadingTreeModels(), grid, window, worldFbo);
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

    // Only the stretch of the tile inside the grid's square footprint can hold a drawn chunk: the first mask marks
    // every chunk whose ground the grid draws, the second every chunk whose trees the grid shows whole
    private void resolveCoverage(MacroChunkInstance macro, GridInstance grid) {

        Arrays.fill(coverageScratch, 0);

        long coordinate = macro.getCoordinate();
        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                grid.getWorldHandle(),
                grid.getActiveChunkCoordinate(),
                coordinate);

        int gridHalf = grid.getRenderDistance() / 2;
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

                long chunkCoordinate = Coordinate2Long.pack(originX + x, originZ + z);
                int index = z * macroChunkSize + x;

                if (worldRenderManager.isChunkDrawn(grid, chunkCoordinate))
                    markCoverage(index);

                if (worldRenderManager.isChunkTreesShown(grid, chunkCoordinate))
                    markCoverage(index + EngineSetting.MACRO_COVERAGE_TREE_BIT_OFFSET);
            }
        }
    }

    private void markCoverage(int bit) {
        coverageScratch[bit / coverageBitsPerWord] |= 1 << (bit % coverageBitsPerWord);
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
