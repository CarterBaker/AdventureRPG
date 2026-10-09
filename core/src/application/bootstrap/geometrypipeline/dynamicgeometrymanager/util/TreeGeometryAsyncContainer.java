package application.bootstrap.geometrypipeline.dynamicgeometrymanager.util;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelQuadListStruct;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import engine.root.AsyncContainerPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeGeometryAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for drawing the trees of one chunk: the shape each
     * tree stood in when the build began, the grid the wood is laid on, the
     * quads it merges into, the species in the order their wood parts were
     * given out, the leaf clusters that hide wood, and the vertices each
     * material collects. Kept warm between builds, so a build allocates
     * nothing once the grid's cells are pooled.
     */

    // Shapes — read once per build, so a tree regrowing meanwhile is drawn whole either way
    private ObjectArrayList<TreeShapeStruct> shapes;

    // Wood
    private SubVoxelGridStruct grid;
    private SubVoxelQuadListStruct quads;
    private ObjectArrayList<TreeHandle> partSpecies;
    private FloatArrayList hiders;

    // Vertices — one list per material
    private Int2ObjectOpenHashMap<FloatArrayList> verts;

    @Override
    protected void create() {

        // Shapes
        this.shapes = new ObjectArrayList<>();

        // Wood
        this.grid = new SubVoxelGridStruct();
        this.quads = new SubVoxelQuadListStruct();
        this.partSpecies = new ObjectArrayList<>();
        this.hiders = new FloatArrayList();

        // Vertices
        this.verts = new Int2ObjectOpenHashMap<>();
    }

    // Reset \\

    @Override
    public void reset() {

        shapes.clear();
        grid.clear();
        quads.clear();
        partSpecies.clear();
        hiders.clear();

        for (FloatArrayList buffer : verts.values())
            buffer.clear();
    }

    // Accessible \\

    public ObjectArrayList<TreeShapeStruct> getShapes() {
        return shapes;
    }

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

    // The vertex list of one material, made the first time the material is drawn
    public FloatArrayList getVerts(int materialID) {

        FloatArrayList buffer = verts.get(materialID);

        if (buffer == null) {
            buffer = new FloatArrayList();
            verts.put(materialID, buffer);
        }

        return buffer;
    }

    public Int2ObjectOpenHashMap<FloatArrayList> getVerts() {
        return verts;
    }
}
