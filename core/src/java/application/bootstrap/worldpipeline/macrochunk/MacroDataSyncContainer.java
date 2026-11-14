package application.bootstrap.worldpipeline.macrochunk;

import engine.root.EngineSetting;
import engine.root.SyncContainerPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MacroDataSyncContainer extends SyncContainerPackage {

    /*
     * Lock guarding one macro chunk's CPU geometry between its build and its
     * upload: the land mesh, and the tile's patch of the grid's open water
     * mask, one RGBA texel per mask cell. A reserved build keeps the macro
     * from being uploaded or recycled until endWork() clears it, and built
     * marks geometry waiting for the GPU together with the resolution it was
     * built at. The buffers are reused for the pooled macro's lifetime —
     * callers must hold the lock.
     */

    // Geometry
    private FloatArrayList vertices;
    private ShortArrayList indices;
    private byte[] waterMask;
    private boolean built;

    // Build Record
    private int builtCellsPerSide;

    // Work
    private boolean building;

    // Internal \\

    @Override
    protected void create() {
        this.vertices = new FloatArrayList();
        this.indices = new ShortArrayList();
        this.waterMask = new byte[EngineSetting.MACRO_WATER_MASK_TEXELS_PER_TILE
                * EngineSetting.MACRO_WATER_MASK_TEXELS_PER_TILE * EngineSetting.COLOR_CHANNEL_COUNT];
    }

    // Reset \\

    public void resetData() {
        clearGeometry();
        this.building = false;
    }

    // Work \\

    public boolean beginWorkLocked() {

        if (building)
            return false;

        this.building = true;
        return true;
    }

    public void endWork() {
        acquire();
        try {
            this.building = false;
        } finally {
            release();
        }
    }

    public void acquireIdle() {

        acquire();

        while (building) {
            release();
            acquire();
        }
    }

    // Geometry \\

    public void markBuilt(int cellsPerSide) {
        this.built = true;
        this.builtCellsPerSide = cellsPerSide;
    }

    public void clearGeometry() {
        vertices.clear();
        indices.clear();
        this.built = false;
    }

    // Accessible \\

    public FloatArrayList getVertices() {
        return vertices;
    }

    public ShortArrayList getIndices() {
        return indices;
    }

    public byte[] getWaterMask() {
        return waterMask;
    }

    public boolean isBuilt() {
        return built;
    }

    public boolean isBuilding() {
        return building;
    }

    public int getBuiltCellsPerSide() {
        return builtCellsPerSide;
    }
}
