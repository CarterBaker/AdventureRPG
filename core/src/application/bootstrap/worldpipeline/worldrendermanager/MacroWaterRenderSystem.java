package application.bootstrap.worldpipeline.worldrendermanager;

import java.nio.ByteBuffer;
import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.macrochunk.MacroChunkInstance;
import application.bootstrap.worldpipeline.macrochunk.MacroWaterInstance;
import application.bootstrap.worldpipeline.util.MacroTerrainUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.memory.BufferUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class MacroWaterRenderSystem extends SystemPackage {

    /*
     * Owns every grid's distant sea: one flat plane drawn with the water
     * shader in a single call, lifted to the live tide on the GPU, so it rises
     * and falls with no work here. Where it may show is settled per pixel by
     * two textures. Each macro tile writes its patch of the open water mask
     * when it uploads and clears it when it hides, so the sea stops where the
     * terrain says it stops and a dry basin below sea level stays dry; the
     * coverage texture marks every chunk the grid draws itself, re-resolved
     * only when the anchor or the drawn chunks changed, so chunk water takes
     * over chunk by chunk exactly where the land does. The plane leaves out
     * the cells the grid always draws, the same cells the macro ring never
     * builds, so the water nearest the camera costs nothing here.
     */

    // Internal
    private WorldRenderManager worldRenderManager;
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private TextureManager textureManager;
    private VAOManager vaoManager;

    // Handles
    private VAOHandle macroWaterVAO;
    private int macroWaterMaterialID;

    // Settings
    private int chunkSize;
    private int macroChunkSize;
    private int maskTiles;
    private int maskTexelsPerTile;
    private int maskSpanChunks;
    private int coverageSize;

    // Scratch
    private FloatArrayList planeVertices;
    private ShortArrayList planeIndices;
    private ByteBuffer maskBuffer;
    private ByteBuffer emptyMaskBuffer;
    private byte[] coverageBytes;
    private ByteBuffer coverageBuffer;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.macroChunkSize = EngineSetting.MACRO_CHUNK_SIZE;
        this.maskTiles = EngineSetting.MACRO_WATER_MASK_TILES;
        this.maskTexelsPerTile = EngineSetting.MACRO_WATER_MASK_TEXELS_PER_TILE;
        this.maskSpanChunks = EngineSetting.MACRO_WATER_MASK_SPAN_CHUNKS;
        this.coverageSize = EngineSetting.MACRO_WATER_COVERAGE_SIZE;

        // Scratch
        int maskPatchBytes = maskTexelsPerTile * maskTexelsPerTile * EngineSetting.COLOR_CHANNEL_COUNT;
        int coverageBytesCount = coverageSize * coverageSize * EngineSetting.COLOR_CHANNEL_COUNT;

        this.planeVertices = new FloatArrayList();
        this.planeIndices = new ShortArrayList();
        this.maskBuffer = BufferUtility.newByteBuffer(maskPatchBytes);
        this.emptyMaskBuffer = BufferUtility.newByteBuffer(maskPatchBytes);
        this.coverageBytes = new byte[coverageBytesCount];
        this.coverageBuffer = BufferUtility.newByteBuffer(coverageBytesCount);
    }

    @Override
    protected void get() {

        // Internal
        this.worldRenderManager = get(WorldRenderManager.class);
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.textureManager = get(TextureManager.class);
        this.vaoManager = get(VAOManager.class);
    }

    @Override
    protected void start() {
        this.macroWaterVAO = vaoManager.getVAOHandleFromVAOName(EngineSetting.MACRO_WATER_VAO);
        this.macroWaterMaterialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.MACRO_WATER_MATERIAL);
    }

    // Lifecycle \\

    private MacroWaterInstance resolveWater(GridInstance grid) {

        MacroWaterInstance water = grid.getMacroWaterInstance();

        if (water != null)
            return water;

        int renderDistance = settings.maxRenderDistance;

        buildPlane(renderDistance);

        MeshInstance meshInstance = meshManager.createMesh(macroWaterVAO, planeVertices, planeIndices);
        MaterialInstance material = materialManager.cloneMaterial(macroWaterMaterialID);
        int maskTexture = textureManager.createTexture2D(
                EngineSetting.MACRO_WATER_MASK_SIZE,
                EngineSetting.MACRO_WATER_MASK_SIZE,
                EngineSetting.GL_REPEAT,
                EngineSetting.GL_NEAREST);
        int coverageTexture = textureManager.createTexture2D(
                coverageSize,
                coverageSize,
                EngineSetting.GL_CLAMP_TO_EDGE,
                EngineSetting.GL_NEAREST);

        material.setUniform(EngineSetting.UNIFORM_MACRO_WATER_MASK, maskTexture);
        material.setUniform(EngineSetting.UNIFORM_MACRO_WATER_COVERAGE, coverageTexture);

        water = create(MacroWaterInstance.class);
        water.constructor(
                meshInstance,
                modelManager.createModel(meshInstance, material),
                maskTexture,
                coverageTexture);
        water.setPlaneRenderDistance(renderDistance);

        grid.setMacroWaterInstance(water);

        return water;
    }

    void disposeWater(GridInstance grid) {

        MacroWaterInstance water = grid.getMacroWaterInstance();

        if (water == null)
            return;

        modelManager.removeMesh(water.getModelInstance());
        textureManager.deleteTexture2D(water.getMaskTexture());
        textureManager.deleteTexture2D(water.getCoverageTexture());

        grid.setMacroWaterInstance(null);
    }

    // Plane \\

    // One cell per macro tile span, laid around the active chunk; a cell the grid always draws is left out
    private void buildPlane(int renderDistance) {

        int reachChunks = EngineSetting.MACRO_RING_REACH_TILES * macroChunkSize;
        int cornersPerSide = maskTiles + 1;
        float cellSizeBlocks = EngineSetting.MACRO_TILE_SIZE_BLOCKS;
        float originBlocks = -reachChunks * chunkSize;
        int settledHalf = MacroTerrainUtility.resolveSettledHalf(renderDistance);
        float settledRadiusSq = MacroTerrainUtility.resolveSettledRadiusSq(renderDistance);

        planeVertices.clear();
        planeIndices.clear();

        for (int z = 0; z < cornersPerSide; z++) {
            for (int x = 0; x < cornersPerSide; x++) {
                planeVertices.add(originBlocks + x * cellSizeBlocks);
                planeVertices.add(originBlocks + z * cellSizeBlocks);
            }
        }

        for (int z = 0; z < maskTiles; z++) {
            for (int x = 0; x < maskTiles; x++) {

                if (MacroTerrainUtility.isCoveredByChunkGrid(
                        x * macroChunkSize - reachChunks,
                        z * macroChunkSize - reachChunks,
                        macroChunkSize,
                        settledHalf,
                        settledRadiusSq))
                    continue;

                int corner = z * cornersPerSide + x;
                pushQuad(corner, corner + 1, corner + cornersPerSide, corner + cornersPerSide + 1);
            }
        }
    }

    private void pushQuad(int low, int lowNext, int high, int highNext) {
        planeIndices.add((short) low);
        planeIndices.add((short) high);
        planeIndices.add((short) lowNext);
        planeIndices.add((short) lowNext);
        planeIndices.add((short) high);
        planeIndices.add((short) highNext);
    }

    private void refreshPlane(MacroWaterInstance water) {

        int renderDistance = settings.maxRenderDistance;

        if (water.getPlaneRenderDistance() == renderDistance)
            return;

        buildPlane(renderDistance);

        ModelInstance modelInstance = water.getModelInstance();

        meshManager.updateMesh(water.getMeshInstance(), planeVertices, planeIndices);
        modelInstance.updateMeshData(water.getMeshInstance().getMeshData());
        water.setPlaneRenderDistance(renderDistance);
    }

    // Ring \\

    // The origin follows each step of the active chunk, never the world's wrap, held modulo the ring's span
    private void advanceOrigin(MacroWaterInstance water, GridInstance grid) {

        long activeChunkCoordinate = grid.getActiveChunkCoordinate();

        if (!water.isAnchored()) {
            water.anchor(
                    activeChunkCoordinate,
                    Math.floorMod(Coordinate2Long.unpackX(activeChunkCoordinate), maskSpanChunks),
                    Math.floorMod(Coordinate2Long.unpackY(activeChunkCoordinate), maskSpanChunks));
            return;
        }

        long lastActiveChunkCoordinate = water.getLastActiveChunkCoordinate();

        if (activeChunkCoordinate == lastActiveChunkCoordinate)
            return;

        long step = WorldWrapUtility.unwrapToGridCoordinate(
                grid.getWorldHandle(),
                lastActiveChunkCoordinate,
                activeChunkCoordinate);

        water.anchor(
                activeChunkCoordinate,
                Math.floorMod(water.getOriginChunkX() + Coordinate2Long.unpackX(step), maskSpanChunks),
                Math.floorMod(water.getOriginChunkZ() + Coordinate2Long.unpackY(step), maskSpanChunks));
    }

    private int resolveMaskSlot(MacroWaterInstance water, MacroChunkInstance macro, GridInstance grid) {

        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                grid.getWorldHandle(),
                grid.getActiveChunkCoordinate(),
                macro.getCoordinate());

        int slotX = Math.floorMod(water.getOriginChunkX() + Coordinate2Long.unpackX(delta), maskSpanChunks)
                / macroChunkSize;
        int slotZ = Math.floorMod(water.getOriginChunkZ() + Coordinate2Long.unpackY(delta), maskSpanChunks)
                / macroChunkSize;

        return slotZ * maskTiles + slotX;
    }

    // Mask \\

    void writeMask(MacroChunkInstance macro, GridInstance grid) {

        MacroWaterInstance water = resolveWater(grid);
        advanceOrigin(water, grid);

        int slot = resolveMaskSlot(water, macro, grid);

        maskBuffer.clear();
        maskBuffer.put(macro.getMacroDataSyncContainer().getWaterMask());
        uploadMaskPatch(water, slot, maskBuffer);

        water.setSlotOwner(slot, macro);
        macro.setWaterMaskSlot(slot);
    }

    void clearMask(MacroChunkInstance macro, GridInstance grid) {

        MacroWaterInstance water = grid.getMacroWaterInstance();

        if (water == null || !macro.hasWaterMaskSlot())
            return;

        int slot = macro.getWaterMaskSlot();

        if (water.getSlotOwner(slot) == macro) {
            uploadMaskPatch(water, slot, emptyMaskBuffer);
            water.setSlotOwner(slot, null);
        }

        macro.setWaterMaskSlot(EngineSetting.INDEX_NOT_FOUND);
    }

    private void uploadMaskPatch(MacroWaterInstance water, int slot, ByteBuffer pixels) {
        textureManager.updateTexture2D(
                water.getMaskTexture(),
                (slot % maskTiles) * maskTexelsPerTile,
                (slot / maskTiles) * maskTexelsPerTile,
                maskTexelsPerTile,
                maskTexelsPerTile,
                pixels);
    }

    // Coverage \\

    private void refreshCoverage(MacroWaterInstance water, GridInstance grid) {

        long anchorCoordinate = grid.getActiveChunkCoordinate();
        int drawnRevision = worldRenderManager.getDrawnRevision();

        if (water.isCoverageCurrent(anchorCoordinate, drawnRevision))
            return;

        resolveCoverage(grid);
        water.setCoverageCurrent(anchorCoordinate, drawnRevision);

        coverageBuffer.clear();
        coverageBuffer.put(coverageBytes);
        textureManager.updateTexture2D(water.getCoverageTexture(), 0, 0, coverageSize, coverageSize, coverageBuffer);
    }

    // Texel (x, z) is the chunk x - center, z - center from the active chunk, the frame the plane is drawn in
    private void resolveCoverage(GridInstance grid) {

        Arrays.fill(coverageBytes, (byte) 0);

        WorldHandle worldHandle = grid.getWorldHandle();
        long activeChunkCoordinate = grid.getActiveChunkCoordinate();
        int activeX = Coordinate2Long.unpackX(activeChunkCoordinate);
        int activeZ = Coordinate2Long.unpackY(activeChunkCoordinate);
        int gridHalf = settings.maxRenderDistance / 2;
        int center = coverageSize / 2;
        byte drawn = (byte) EngineSetting.PACKED_COLOR_CHANNEL_MASK;

        for (int z = -gridHalf; z < gridHalf; z++) {
            for (int x = -gridHalf; x < gridHalf; x++) {

                long chunkCoordinate = WorldWrapUtility.wrapAroundWorld(
                        worldHandle,
                        Coordinate2Long.pack(activeX + x, activeZ + z));

                if (!worldRenderManager.isChunkDrawn(grid, chunkCoordinate))
                    continue;

                coverageBytes[((z + center) * coverageSize + x + center) * EngineSetting.COLOR_CHANNEL_COUNT] = drawn;
            }
        }
    }

    // Render \\

    void renderGridWater(GridInstance grid, WindowInstance window) {

        MacroWaterInstance water = resolveWater(grid);

        advanceOrigin(water, grid);
        refreshPlane(water);
        refreshCoverage(water, grid);

        ModelInstance modelInstance = water.getModelInstance();
        modelInstance.getMaterial().setUniform(EngineSetting.UNIFORM_MACRO_WATER_ANCHOR, water.getAnchorUniform());

        worldRenderManager.pushWaterModel(modelInstance, grid, window);
    }
}
