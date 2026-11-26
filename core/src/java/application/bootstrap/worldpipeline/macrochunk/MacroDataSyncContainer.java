package application.bootstrap.worldpipeline.macrochunk;

import engine.root.EngineSetting;
import engine.root.SyncContainerPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MacroDataSyncContainer extends SyncContainerPackage {

    /*
     * Lock guarding one macro chunk's CPU geometry between its build and its
     * upload: the land mesh, the tree mesh its canopy and stand-ins are laid
     * in apart from the land, so the tile's trees can fade from one look to
     * the next while its ground does not, the bark and leaves of the tree
     * models a tile near the grid draws, and the tile's patch of the grid's open water
     * mask, one RGBA texel per mask cell. A reserved build keeps the macro
     * from being uploaded or recycled until endWork() clears it, and built
     * marks geometry waiting for the GPU together with the resolution it was
     * built at. The buffers are reused for the pooled macro's lifetime —
     * callers must hold the lock.
     */

    // Geometry
    private FloatArrayList vertices;
    private ShortArrayList indices;
    private FloatArrayList treeVertices;
    private ShortArrayList treeIndices;
    private byte[] waterMask;
    private FloatArrayList treeBark;
    private FloatArrayList treeLeaves;
    private FloatArrayList treeImpostors;
    private boolean built;

    // Build Record
    private int builtCellsPerSide;
    private boolean builtTreeModels;

    // Work
    private boolean building;

    // Internal \\

    @Override
    protected void create() {
        this.vertices = new FloatArrayList();
        this.indices = new ShortArrayList();
        this.treeVertices = new FloatArrayList();
        this.treeIndices = new ShortArrayList();
        this.treeBark = new FloatArrayList();
        this.treeLeaves = new FloatArrayList();
        this.treeImpostors = new FloatArrayList();
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

    public void markBuilt(int cellsPerSide, boolean treeModels) {
        this.built = true;
        this.builtCellsPerSide = cellsPerSide;
        this.builtTreeModels = treeModels;
    }

    public void clearGeometry() {
        vertices.clear();
        indices.clear();
        treeVertices.clear();
        treeIndices.clear();
        treeBark.clear();
        treeLeaves.clear();
        treeImpostors.clear();
        this.built = false;
    }

    // Accessible \\

    public FloatArrayList getVertices() {
        return vertices;
    }

    public ShortArrayList getIndices() {
        return indices;
    }

    public FloatArrayList getTreeVertices() {
        return treeVertices;
    }

    public ShortArrayList getTreeIndices() {
        return treeIndices;
    }

    public byte[] getWaterMask() {
        return waterMask;
    }

    public FloatArrayList getTreeBark() {
        return treeBark;
    }

    public FloatArrayList getTreeLeaves() {
        return treeLeaves;
    }

    // The impostor instances of the trees its tree models leave to TreeImpostorRenderSystem
    public FloatArrayList getTreeImpostors() {
        return treeImpostors;
    }

    public boolean isBuiltTreeModels() {
        return builtTreeModels;
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
