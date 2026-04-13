package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import application.bootstrap.menupipeline.fonts.GlyphMetricStruct;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.core.engine.ManagerPackage;
import application.core.settings.EngineSetting;

public class DynamicGeometryManager extends ManagerPackage {

    /*
     * Entry point for all dynamic geometry assembly. Delegates chunk and
     * subchunk geometry to InternalBuildManager and exposes font glyph
     * assembly for the menu pipeline. Owns the shared async scratch container
     * used during synchronous build calls.
     */

    // Internal
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private InternalBuildManager internalBuildManager;

    // Settings
    private int worldHeight;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.dynamicGeometryAsyncContainer = create(DynamicGeometryAsyncContainer.class);
        this.internalBuildManager = create(InternalBuildManager.class);

        // Settings
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
    }

    // Chunk Geometry \\

    public boolean build(
            DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer,
            ChunkInstance chunkInstance) {

        boolean success = true;
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (int i = 0; i < worldHeight; i++) {
            if (!internalBuildManager.build(dynamicGeometryAsyncContainer, chunkInstance, subChunks[i]))
                success = false;
        }

        return success;
    }

    public boolean buildSubChunk(
            DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer,
            ChunkInstance chunkInstance,
            int subChunkIndex) {

        return internalBuildManager.build(
                dynamicGeometryAsyncContainer,
                chunkInstance,
                chunkInstance.getSubChunks()[subChunkIndex]);
    }

    // Font Geometry \\

    /*
     * Caller creates the DynamicModelHandle with the correct materialID and
     * VAOHandle, then passes it here to be filled with glyph quad verts.
     */
    public void buildGlyphModel(
            DynamicModelHandle model,
            GlyphMetricStruct glyph,
            int atlasPixelSize) {
        internalBuildManager.buildGlyphModel(model, glyph, atlasPixelSize);
    }

    // Accessible \\

    public DynamicGeometryAsyncContainer getDynamicGeometryAsyncInstance() {
        return dynamicGeometryAsyncContainer;
    }
}