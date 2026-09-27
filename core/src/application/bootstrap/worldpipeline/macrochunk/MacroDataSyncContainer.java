package application.bootstrap.worldpipeline.macrochunk;

import engine.root.SyncContainerPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MacroDataSyncContainer extends SyncContainerPackage {

    /*
     * Lock guarding one macro chunk's CPU geometry between its build and its
     * upload. A reserved build keeps the macro from being uploaded or recycled
     * until endWork() clears it, and built marks geometry waiting for the GPU.
     * The lists are reused for the pooled macro's lifetime — callers must hold
     * the lock.
     */

    // Geometry
    private FloatArrayList vertices;
    private ShortArrayList indices;
    private boolean built;

    // Work
    private boolean building;

    // Internal \\

    @Override
    protected void create() {
        this.vertices = new FloatArrayList();
        this.indices = new ShortArrayList();
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

    public void markBuilt() {
        this.built = true;
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

    public boolean isBuilt() {
        return built;
    }

    public boolean isBuilding() {
        return building;
    }
}
