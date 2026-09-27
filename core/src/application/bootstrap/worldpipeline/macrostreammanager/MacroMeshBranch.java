package application.bootstrap.worldpipeline.macrostreammanager;

import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MacroMeshBranch extends BranchPackage {

    /*
     * Turns a sampled macro lattice into its mesh. A tile that is one flat
     * surface of one color, open sea above all, collapses to a single quad;
     * any other tile becomes one indexed grid with every lattice point shared
     * between its cells. Each outer edge hangs a skirt one cell deep below the
     * tile's lowest point, so
     * neighbours sampled at different resolutions never open a crack between
     * them. Positions are tile-local, placed by the macro's own position UBO,
     * and color rides as one exact packed float.
     */

    // Settings
    private float tileSizeBlocks;
    private float skirtDepthCells;
    private int vertexFloatCount;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.tileSizeBlocks = EngineSetting.MACRO_CHUNK_SIZE * EngineSetting.CHUNK_SIZE;
        this.skirtDepthCells = EngineSetting.MACRO_SKIRT_DEPTH_CELLS;
        this.vertexFloatCount = EngineSetting.MACRO_VERTEX_FLOAT_COUNT;
    }

    // Assembly \\

    void assembleMesh(MacroBuildAsyncContainer scratch, FloatArrayList vertices, ShortArrayList indices) {

        vertices.clear();
        indices.clear();

        int stride = isUniform(scratch) ? scratch.cellsPerSide : 1;

        assembleGrid(scratch, stride, vertices, indices);
    }

    private boolean isUniform(MacroBuildAsyncContainer scratch) {

        float height = scratch.heightBlocks[0];
        float color = scratch.packedColors[0];

        for (int i = 1; i < scratch.getSampleCount(); i++)
            if (scratch.heightBlocks[i] != height || scratch.packedColors[i] != color)
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
                        scratch.packedColors[sample]);
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
                vertices.getFloat(offset + 3));
    }

    // Vertex \\

    private void pushVertex(FloatArrayList vertices, float x, float y, float z, float packedColor) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertices.add(packedColor);
    }

    private void pushQuad(ShortArrayList indices, int low, int lowNext, int high, int highNext) {
        indices.add((short) low);
        indices.add((short) high);
        indices.add((short) lowNext);
        indices.add((short) lowNext);
        indices.add((short) high);
        indices.add((short) highNext);
    }
}
