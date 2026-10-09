package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.TreeGeometryAsyncContainer;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.TreeMeshUtility;
import application.bootstrap.worldpipeline.util.TreeRasterUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TreeGeometryBranch extends BranchPackage {

    /*
     * Draws the trees a chunk holds into one of its subchunks. The wood of
     * every tree reaching the subchunk is laid on one grid a block wider than
     * the subchunk on every side, each species with its own bark and
     * heartwood parts, then merged into quads for the subchunk's blocks alone:
     * the margin only tells the mesher and the bevel what lies beyond, so wood
     * crossing a border meets itself without a seam or a hidden face, and any
     * wood a leaf cluster wholly hides is left out. Each
     * leaf cluster is drawn by the subchunk its centre lies in. Wood goes to
     * the bark material and leaves to the leaf material, merged with the
     * subchunk's terrain like any other geometry.
     */

    // Internal
    private TreeManager treeManager;
    private SubVoxelManager subVoxelManager;
    private TreeGeometryAsyncContainer treeContainer;

    // Settings
    private int chunkSize;
    private int margin;
    private int resolution;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.treeContainer = create(TreeGeometryAsyncContainer.class);

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.margin = EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS;
        this.resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
    }

    @Override
    protected void get() {

        // Internal
        this.treeManager = get(TreeManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
    }

    // Reach \\

    // True when any tree the chunk holds can reach into the subchunk
    boolean reachesSubChunk(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance) {

        TreeInstance[] trees = chunkInstance.getTreePaletteHandle().getTrees();
        int baseY = (int) subChunkInstance.getCoordinate() * chunkSize;

        for (int i = 0; i < trees.length; i++)
            if (reaches(trees[i].getShape(), trees[i].getBaseY() - baseY, -margin, chunkSize + margin))
                return true;

        return false;
    }

    // Vertically only — the chunk holds a tree because it reaches the chunk's columns
    private boolean reaches(TreeShapeStruct shape, float rootY, float min, float max) {
        return !shape.isEmpty() && rootY + shape.getMinY() < max && rootY + shape.getMaxY() > min;
    }

    // Build \\

    void assembleTrees(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            Int2ObjectOpenHashMap<FloatArrayList> verts) {

        TreeInstance[] trees = chunkInstance.getTreePaletteHandle().getTrees();

        if (trees.length == 0)
            return;

        TreeGeometryAsyncContainer scratch = treeContainer.getInstance();
        scratch.reset();

        WorldHandle worldHandle = chunkInstance.getWorldHandle();
        long chunkCoordinate = chunkInstance.getCoordinate();
        long chunkOriginX = (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize;
        long chunkOriginZ = (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize;
        int baseY = (int) subChunkInstance.getCoordinate() * chunkSize;
        int regionMax = (chunkSize + margin * 2) * resolution;
        FloatArrayList leafVerts = verts.computeIfAbsent(treeManager.getLeafMaterialID(), k -> new FloatArrayList());

        for (int i = 0; i < trees.length; i++) {

            TreeInstance tree = trees[i];
            TreeShapeStruct shape = tree.getShape();
            int rootX = (int) WorldWrapUtility.wrappedBlockDeltaX(worldHandle, tree.getAnchorX(), chunkOriginX);
            int rootY = tree.getBaseY() - baseY;
            int rootZ = (int) WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, tree.getAnchorZ(), chunkOriginZ);

            if (!reaches(shape, rootY, -margin, chunkSize + margin))
                continue;

            if (!shape.getNodes().isEmpty()) {

                TreeRasterUtility.rasterize(
                        shape, rootX + margin, rootY + margin, rootZ + margin,
                        0, 0, 0, regionMax, regionMax, regionMax,
                        resolvePartBase(scratch.getPartSpecies(), shape.getTreeHandle()),
                        scratch.getGrid());
                TreeMeshUtility.collectHiders(
                        shape,
                        (rootX + margin + EngineSetting.TREE_ROOT_CENTER_BLOCKS) * resolution,
                        (rootY + margin) * resolution,
                        (rootZ + margin + EngineSetting.TREE_ROOT_CENTER_BLOCKS) * resolution,
                        scratch.getHiders());
            }

            TreeMeshUtility.emitLeaves(
                    shape, rootX + EngineSetting.TREE_ROOT_CENTER_BLOCKS, rootY,
                    rootZ + EngineSetting.TREE_ROOT_CENTER_BLOCKS,
                    0f, 0f, 0f, chunkSize, chunkSize, chunkSize, leafVerts);
        }

        SubVoxelGridStruct grid = scratch.getGrid();

        if (grid.isEmpty())
            return;

        subVoxelManager.meshGrid(
                grid, null,
                margin, margin, margin,
                margin + chunkSize - 1, margin + chunkSize - 1, margin + chunkSize - 1,
                scratch.getQuads());

        TreeMeshUtility.emitWood(
                grid, scratch.getQuads(), scratch.getPartSpecies(), scratch.getHiders(), -margin, -margin, -margin,
                verts.computeIfAbsent(treeManager.getBarkMaterialID(), k -> new FloatArrayList()));
    }

    // The first of a species' two wood parts, the species given its parts the first time one of its trees is laid
    private int resolvePartBase(ObjectArrayList<TreeHandle> partSpecies, TreeHandle treeHandle) {

        int index = partSpecies.indexOf(treeHandle);

        if (index == EngineSetting.INDEX_NOT_FOUND) {
            index = partSpecies.size();
            partSpecies.add(treeHandle);
        }

        return index * EngineSetting.TREE_WOOD_PART_COUNT;
    }
}
