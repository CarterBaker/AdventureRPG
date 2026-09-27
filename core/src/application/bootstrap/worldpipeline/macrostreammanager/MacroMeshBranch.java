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
     * between its cells. Positions are tile-local, placed by the macro's own
     * position UBO, and color rides as one exact packed float.
     */

    // Settings
    private int cellsPerSide;
    private int samplesPerSide;
    private float cellSizeBlocks;
    private float tileSizeBlocks;
    private int vertexFloatCount;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.cellsPerSide = EngineSetting.MACRO_CELLS_PER_SIDE;
        this.samplesPerSide = MacroBuildAsyncContainer.SAMPLES_PER_SIDE;
        this.tileSizeBlocks = EngineSetting.MACRO_CHUNK_SIZE * EngineSetting.CHUNK_SIZE;
        this.cellSizeBlocks = tileSizeBlocks / cellsPerSide;
        this.vertexFloatCount = EngineSetting.MACRO_VERTEX_FLOAT_COUNT;
    }

    // Assembly \\

    void assembleMesh(MacroBuildAsyncContainer scratch, FloatArrayList vertices, ShortArrayList indices) {

        vertices.clear();
        indices.clear();

        if (isUniform(scratch))
            assembleFlatQuad(scratch, vertices, indices);
        else
            assembleGrid(scratch, vertices, indices);
    }

    private boolean isUniform(MacroBuildAsyncContainer scratch) {

        float height = scratch.heightBlocks[0];
        float color = scratch.packedColors[0];

        for (int i = 1; i < MacroBuildAsyncContainer.SAMPLE_COUNT; i++)
            if (scratch.heightBlocks[i] != height || scratch.packedColors[i] != color)
                return false;

        return true;
    }

    // Flat \\

    private void assembleFlatQuad(MacroBuildAsyncContainer scratch, FloatArrayList vertices, ShortArrayList indices) {

        float height = scratch.heightBlocks[0];
        float color = scratch.packedColors[0];

        vertices.ensureCapacity(vertexFloatCount * EngineSetting.QUAD_VERTEX_COUNT);

        pushVertex(vertices, 0f, height, 0f, color);
        pushVertex(vertices, tileSizeBlocks, height, 0f, color);
        pushVertex(vertices, tileSizeBlocks, height, tileSizeBlocks, color);
        pushVertex(vertices, 0f, height, tileSizeBlocks, color);

        pushQuad(indices, 0, 1, 3, 2);
    }

    // Grid \\

    private void assembleGrid(MacroBuildAsyncContainer scratch, FloatArrayList vertices, ShortArrayList indices) {

        vertices.ensureCapacity(MacroBuildAsyncContainer.SAMPLE_COUNT * vertexFloatCount);
        indices.ensureCapacity(cellsPerSide * cellsPerSide * EngineSetting.QUAD_INDEX_COUNT);

        for (int z = 0; z < samplesPerSide; z++) {
            for (int x = 0; x < samplesPerSide; x++) {
                int index = z * samplesPerSide + x;
                pushVertex(
                        vertices,
                        x * cellSizeBlocks,
                        scratch.heightBlocks[index],
                        z * cellSizeBlocks,
                        scratch.packedColors[index]);
            }
        }

        for (int z = 0; z < cellsPerSide; z++) {
            for (int x = 0; x < cellsPerSide; x++) {
                int corner = z * samplesPerSide + x;
                pushQuad(indices, corner, corner + 1, corner + samplesPerSide, corner + samplesPerSide + 1);
            }
        }
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
