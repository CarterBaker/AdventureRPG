package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.TreeGeometryAsyncContainer;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.tree.TreeDetail;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.TreeMeshUtility;
import application.bootstrap.worldpipeline.util.TreeModelUtility;
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
     * Draws the trees a chunk holds, apart from its terrain, one vertex list
     * per material, each tree as finely as its detail calls for. The wood of
     * every tree drawn in full that reaches the chunk is laid on one grid a
     * block wider than the chunk on every side and as tall as its trees
     * stand, each species with its own bark and heartwood parts, then merged
     * into quads for the chunk's own columns alone: the margin only tells the
     * mesher and the bevel what lies beyond, so wood crossing a border meets
     * itself without a seam or a hidden face, and a trunk merges into long
     * faces from its root to its crown. Any wood a leaf cluster wholly hides
     * is left out, and each leaf cluster is drawn by the chunk its centre
     * lies in. A tree beyond the tree detail distance is never grown here:
     * the chunk its root stands in draws it whole as a model, and every other
     * chunk leaves it be. Only the chunk's own tree palette is ever read,
     * never a neighbor, so a build needs no lock but the chunk's own. Wood
     * goes to the bark material and leaves to the leaf material.
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

    // Build \\

    // Every material's vertices for the chunk's trees, in the chunk's frame — read before this thread's next build
    Int2ObjectOpenHashMap<FloatArrayList> assembleTrees(ChunkInstance chunkInstance) {

        TreeGeometryAsyncContainer scratch = treeContainer.getInstance();
        scratch.reset();

        TreeInstance[] trees = chunkInstance.getTreePaletteHandle().getTrees();
        ObjectArrayList<TreeInstance> grownTrees = scratch.getGrownTrees();
        ObjectArrayList<TreeShapeStruct> shapes = scratch.getShapes();
        WorldHandle worldHandle = chunkInstance.getWorldHandle();
        long chunkCoordinate = chunkInstance.getCoordinate();
        long chunkOriginX = (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize;
        long chunkOriginZ = (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize;
        float lowY = Float.MAX_VALUE;
        float highY = -Float.MAX_VALUE;

        for (int i = 0; i < trees.length; i++) {

            TreeInstance tree = trees[i];

            if (!tree.isDetailed()) {

                if (tree.getRootChunkCoordinate() == chunkCoordinate)
                    emitModel(scratch, tree, worldHandle, chunkOriginX, chunkOriginZ);

                continue;
            }

            TreeShapeStruct shape = tree.getShape();

            if (shape.isEmpty())
                continue;

            grownTrees.add(tree);
            shapes.add(shape);
            lowY = Math.min(lowY, tree.getBaseY() + shape.getMinY());
            highY = Math.max(highY, tree.getBaseY() + shape.getMaxY());
        }

        if (grownTrees.isEmpty())
            return scratch.getVerts();

        int floorY = (int) Math.floor(lowY) - margin;
        int columnHeight = (int) Math.ceil(highY) - floorY + margin;
        int regionAcross = (chunkSize + margin * 2) * resolution;
        int regionUp = columnHeight * resolution;
        FloatArrayList leafVerts = scratch.getVerts(treeManager.getLeafMaterialID());

        for (int i = 0; i < grownTrees.size(); i++) {

            TreeInstance tree = grownTrees.get(i);
            TreeShapeStruct shape = shapes.get(i);
            int rootX = (int) WorldWrapUtility.wrappedBlockDeltaX(worldHandle, tree.getAnchorX(), chunkOriginX);
            int rootY = tree.getBaseY() - floorY;
            int rootZ = (int) WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, tree.getAnchorZ(), chunkOriginZ);

            if (!shape.getNodes().isEmpty()) {

                TreeRasterUtility.rasterize(
                        shape, rootX + margin, rootY, rootZ + margin,
                        0, 0, 0, regionAcross, regionUp, regionAcross,
                        resolvePartBase(scratch.getPartSpecies(), shape.getTreeHandle()),
                        scratch.getGrid());
                TreeMeshUtility.collectHiders(
                        shape,
                        (rootX + margin + EngineSetting.TREE_ROOT_CENTER_BLOCKS) * resolution,
                        rootY * resolution,
                        (rootZ + margin + EngineSetting.TREE_ROOT_CENTER_BLOCKS) * resolution,
                        scratch.getHiders());
            }

            TreeMeshUtility.emitLeaves(
                    shape, rootX + EngineSetting.TREE_ROOT_CENTER_BLOCKS, tree.getBaseY(),
                    rootZ + EngineSetting.TREE_ROOT_CENTER_BLOCKS,
                    0f, -Float.MAX_VALUE, 0f, chunkSize, Float.MAX_VALUE, chunkSize, leafVerts);
        }

        SubVoxelGridStruct grid = scratch.getGrid();

        if (grid.isEmpty())
            return scratch.getVerts();

        subVoxelManager.meshGrid(
                grid, null,
                margin, 0, margin,
                margin + chunkSize - 1, columnHeight - 1, margin + chunkSize - 1,
                scratch.getQuads());

        TreeMeshUtility.emitWood(
                grid, scratch.getQuads(), scratch.getPartSpecies(), scratch.getHiders(), -margin, floorY, -margin,
                scratch.getVerts(treeManager.getBarkMaterialID()));

        return scratch.getVerts();
    }

    // A tree beyond the tree detail distance drawn whole as a model of itself by the chunk its root stands in
    private void emitModel(
            TreeGeometryAsyncContainer scratch,
            TreeInstance tree,
            WorldHandle worldHandle,
            long chunkOriginX,
            long chunkOriginZ) {

        TreeModelUtility.emit(
                tree,
                WorldWrapUtility.wrappedBlockDeltaX(worldHandle, tree.getAnchorX(), chunkOriginX)
                        + EngineSetting.TREE_ROOT_CENTER_BLOCKS,
                WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, tree.getAnchorZ(), chunkOriginZ)
                        + EngineSetting.TREE_ROOT_CENTER_BLOCKS,
                TreeDetail.MODEL,
                scratch.getModel(),
                scratch.getVerts(treeManager.getBarkMaterialID()),
                scratch.getVerts(treeManager.getLeafMaterialID()));
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
