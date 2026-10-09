package application.bootstrap.worldpipeline.macrostreammanager;

import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MacroMeshBranch extends BranchPackage {

    /*
     * Turns a sampled macro lattice into its land mesh and its patch of the
     * open water mask. A tile that is one flat surface of one color collapses
     * to a single quad; any other tile becomes one indexed grid with every
     * lattice point shared between its cells. Each outer edge hangs a skirt
     * one cell deep below the tile's lowest point, so neighbours sampled at
     * different resolutions never open a crack between them. Positions are
     * tile-local, placed by the macro's own position UBO, and the top and
     * slope colors ride as two exact packed floats. A mask texel is open
     * wherever any corner of the cell holding it has the sea over it, so the
     * water plane always reaches the shore the land rises out of, and a dry
     * basin below sea level, with no open corner, stays dry. Woods are a
     * second grid laid after the land in the same buffers: every lattice
     * point under a canopy rises to it in the woods' colors and every other
     * one sinks below the ground, and only a cell with a canopy corner is
     * drawn, so a forest's edge breaks off in jagged slopes into the land.
     * Settlements are a third grid laid the same way after the woods, their
     * structures, walls and bridges rising out of the land in their own
     * colors.
     */

    // Settings
    private float tileSizeBlocks;
    private float skirtDepthCells;
    private int vertexFloatCount;
    private int maskTexelsPerTile;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.tileSizeBlocks = EngineSetting.MACRO_TILE_SIZE_BLOCKS;
        this.skirtDepthCells = EngineSetting.MACRO_SKIRT_DEPTH_CELLS;
        this.vertexFloatCount = EngineSetting.MACRO_VERTEX_FLOAT_COUNT;
        this.maskTexelsPerTile = EngineSetting.MACRO_WATER_MASK_TEXELS_PER_TILE;
    }

    // Land \\

    void assembleLand(MacroBuildAsyncContainer scratch, FloatArrayList vertices, ShortArrayList indices) {

        vertices.clear();
        indices.clear();

        int stride = isUniform(scratch) ? scratch.cellsPerSide : 1;

        assembleGrid(scratch, stride, vertices, indices);
    }

    private boolean isUniform(MacroBuildAsyncContainer scratch) {

        float height = scratch.heightBlocks[0];
        float topColor = scratch.topColors[0];
        float sideColor = scratch.sideColors[0];

        for (int i = 1; i < scratch.getSampleCount(); i++)
            if (scratch.heightBlocks[i] != height
                    || scratch.topColors[i] != topColor
                    || scratch.sideColors[i] != sideColor)
                return false;

        return true;
    }

    // Grid \\

    // stride walks the lattice in whole cells of `stride` samples, so a uniform tile keeps only its four corners
    private void assembleGrid(
            MacroBuildAsyncContainer scratch,
            int stride,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int samplesPerSide = scratch.getSamplesPerSide();
        int cellsPerSide = scratch.cellsPerSide / stride;
        int cornersPerSide = cellsPerSide + 1;
        float cellSizeBlocks = tileSizeBlocks / cellsPerSide;
        float skirtBottom = scratch.minHeightBlocks - cellSizeBlocks * skirtDepthCells;

        vertices.ensureCapacity(cornersPerSide * cornersPerSide * vertexFloatCount * 2);
        indices.ensureCapacity(cellsPerSide * cellsPerSide * EngineSetting.QUAD_INDEX_COUNT * 2);

        for (int z = 0; z < cornersPerSide; z++) {
            for (int x = 0; x < cornersPerSide; x++) {
                int sample = (z * stride) * samplesPerSide + x * stride;
                pushVertex(
                        vertices,
                        x * cellSizeBlocks,
                        scratch.heightBlocks[sample],
                        z * cellSizeBlocks,
                        scratch.topColors[sample],
                        scratch.sideColors[sample]);
            }
        }

        for (int z = 0; z < cellsPerSide; z++) {
            for (int x = 0; x < cellsPerSide; x++) {

                int corner = z * cornersPerSide + x;
                pushQuad(indices, corner, corner + 1, corner + cornersPerSide, corner + cornersPerSide + 1);

                if (z == 0)
                    pushSkirt(vertices, indices, corner, corner + 1, skirtBottom);

                if (z == cellsPerSide - 1)
                    pushSkirt(vertices, indices, corner + cornersPerSide + 1, corner + cornersPerSide, skirtBottom);

                if (x == 0)
                    pushSkirt(vertices, indices, corner + cornersPerSide, corner, skirtBottom);

                if (x == cellsPerSide - 1)
                    pushSkirt(vertices, indices, corner + 1, corner + cornersPerSide + 1, skirtBottom);
            }
        }
    }

    // Raised Layers \\

    void assembleCanopy(MacroBuildAsyncContainer scratch, FloatArrayList vertices, ShortArrayList indices) {
        assembleRaised(
                scratch, scratch.canopyHeights, scratch.canopyTopColors, scratch.canopySideColors,
                scratch.canopyCount, EngineSetting.TREE_CANOPY_SINK_BLOCKS, vertices, indices);
    }

    void assembleStructures(MacroBuildAsyncContainer scratch, FloatArrayList vertices, ShortArrayList indices) {
        assembleRaised(
                scratch, scratch.structureHeights, scratch.structureTopColors, scratch.structureSideColors,
                scratch.structureCount, EngineSetting.SETTLEMENT_MACRO_SINK_BLOCKS, vertices, indices);
    }

    // A grid over the land where every raised lattice point stands its height above the ground in its own colors
    // and every other one sinks below it, drawing only cells with a raised corner
    private void assembleRaised(
            MacroBuildAsyncContainer scratch,
            float[] raisedHeights,
            float[] raisedTopColors,
            float[] raisedSideColors,
            int raisedCount,
            float sinkBlocks,
            FloatArrayList vertices,
            ShortArrayList indices) {

        if (raisedCount == 0)
            return;

        int samplesPerSide = scratch.getSamplesPerSide();
        int cellsPerSide = scratch.cellsPerSide;
        float cellSizeBlocks = tileSizeBlocks / cellsPerSide;
        int base = vertices.size() / vertexFloatCount;

        for (int z = 0; z < samplesPerSide; z++) {
            for (int x = 0; x < samplesPerSide; x++) {

                int sample = z * samplesPerSide + x;
                boolean raised = raisedHeights[sample] > 0f;

                pushVertex(
                        vertices,
                        x * cellSizeBlocks,
                        raised
                                ? scratch.heightBlocks[sample] + raisedHeights[sample]
                                : scratch.heightBlocks[sample] - sinkBlocks,
                        z * cellSizeBlocks,
                        raised ? raisedTopColors[sample] : scratch.topColors[sample],
                        raised ? raisedSideColors[sample] : scratch.sideColors[sample]);
            }
        }

        for (int z = 0; z < cellsPerSide; z++) {
            for (int x = 0; x < cellsPerSide; x++) {

                int corner = z * samplesPerSide + x;

                if (raisedHeights[corner] <= 0f
                        && raisedHeights[corner + 1] <= 0f
                        && raisedHeights[corner + samplesPerSide] <= 0f
                        && raisedHeights[corner + samplesPerSide + 1] <= 0f)
                    continue;

                pushQuad(indices, base + corner, base + corner + 1,
                        base + corner + samplesPerSide, base + corner + samplesPerSide + 1);
            }
        }
    }

    // Skirt \\

    private void pushSkirt(FloatArrayList vertices, ShortArrayList indices, int top, int topNext, float bottom) {

        int base = vertices.size() / vertexFloatCount;

        pushDroppedVertex(vertices, top, bottom);
        pushDroppedVertex(vertices, topNext, bottom);
        pushQuad(indices, base, base + 1, top, topNext);
    }

    private void pushDroppedVertex(FloatArrayList vertices, int vertex, float bottom) {

        int offset = vertex * vertexFloatCount;

        pushVertex(
                vertices,
                vertices.getFloat(offset),
                bottom,
                vertices.getFloat(offset + 2),
                vertices.getFloat(offset + 3),
                vertices.getFloat(offset + 4));
    }

    // Vertex \\

    private void pushVertex(FloatArrayList vertices, float x, float y, float z, float topColor, float sideColor) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertices.add(topColor);
        vertices.add(sideColor);
    }

    private void pushQuad(ShortArrayList indices, int low, int lowNext, int high, int highNext) {
        indices.add((short) low);
        indices.add((short) high);
        indices.add((short) lowNext);
        indices.add((short) lowNext);
        indices.add((short) high);
        indices.add((short) highNext);
    }

    // Water Mask \\

    void assembleWaterMask(MacroBuildAsyncContainer scratch, byte[] waterMask) {

        int samplesPerSide = scratch.getSamplesPerSide();
        int lastCell = scratch.cellsPerSide - 1;
        float cellsPerTexel = (float) scratch.cellsPerSide / maskTexelsPerTile;
        boolean anyOpen = scratch.openWaterCount > 0;

        for (int z = 0; z < maskTexelsPerTile; z++) {
            for (int x = 0; x < maskTexelsPerTile; x++) {

                int cellX = Math.min((int) ((x + 0.5f) * cellsPerTexel), lastCell);
                int cellZ = Math.min((int) ((z + 0.5f) * cellsPerTexel), lastCell);
                int corner = cellZ * samplesPerSide + cellX;

                boolean open = anyOpen && (scratch.openWater[corner]
                        || scratch.openWater[corner + 1]
                        || scratch.openWater[corner + samplesPerSide]
                        || scratch.openWater[corner + samplesPerSide + 1]);

                writeMaskTexel(waterMask, z * maskTexelsPerTile + x, open);
            }
        }
    }

    private void writeMaskTexel(byte[] waterMask, int texel, boolean open) {

        int offset = texel * EngineSetting.COLOR_CHANNEL_COUNT;
        byte full = (byte) EngineSetting.PACKED_COLOR_CHANNEL_MASK;

        waterMask[offset] = open ? full : 0;
        waterMask[offset + 1] = 0;
        waterMask[offset + 2] = 0;
        waterMask[offset + 3] = full;
    }
}
