package application.bootstrap.geometrypipeline.dynamicgeometrymanager.util;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelQuadListStruct;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import engine.root.AsyncContainerPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeGeometryAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for drawing the trees of one subchunk: the grid the
     * wood is laid on, the quads it merges into, the species in the order
     * their wood parts were given out, and the leaf clusters that hide wood. Kept warm between builds, so a build
     * allocates nothing once the grid's cells are pooled.
     */

    // Wood
    private SubVoxelGridStruct grid;
    private SubVoxelQuadListStruct quads;
    private ObjectArrayList<TreeHandle> partSpecies;
    private FloatArrayList hiders;

    @Override
    protected void create() {

        // Wood
        this.grid = new SubVoxelGridStruct();
        this.quads = new SubVoxelQuadListStruct();
        this.partSpecies = new ObjectArrayList<>();
        this.hiders = new FloatArrayList();
    }

    // Reset \\

    @Override
    public void reset() {
        grid.clear();
        quads.clear();
        partSpecies.clear();
        hiders.clear();
    }

    // Accessible \\

    public SubVoxelGridStruct getGrid() {
        return grid;
    }

    public SubVoxelQuadListStruct getQuads() {
        return quads;
    }

    public ObjectArrayList<TreeHandle> getPartSpecies() {
        return partSpecies;
    }

    public FloatArrayList getHiders() {
        return hiders;
    }
}
