package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.worldpipeline.tree.TreeCarveStruct;
import application.bootstrap.worldpipeline.tree.TreeCastStruct;
import application.bootstrap.worldpipeline.tree.TreeGrowthStruct;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeNodeStruct;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import application.bootstrap.worldpipeline.tree.TreeWoodStruct;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.TreeRasterUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class TreeChopBranch extends BranchPackage {

    /*
     * Main thread — a landed swing against a tree. Anything knocks a leaf
     * cluster off, now and then shaking a seed loose. Wood only gives to the
     * tool its species names, at its tier or better, and the swing is level:
     * each stroke bites a wedge flat into the wood where it struck, along the
     * swing's heading, as wide as the wood is there and as deep as the
     * tool's tier drives it, so strokes landing on one spot cut one notch
     * ever deeper, sub-voxel by sub-voxel. When the notch leaves less than a
     * share of the wood's cross-section standing, the wood there is cut
     * through: everything beyond the cut falls free — an upright trunk
     * toppling away from the one who cut it, a limb dropping straight down —
     * and the stump stays behind with the notch's lower half in its top.
     */

    // Internal
    private TreeManager treeManager;
    private TreeRebuildBranch treeRebuildBranch;
    private TreeFallBranch treeFallBranch;

    // Settings
    private int resolution;
    private int center;

    // Scratch
    private SubVoxelGridStruct grid;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        this.center = EngineSetting.SUB_VOXEL_RESOLUTION / 2;

        // Scratch
        this.grid = new SubVoxelGridStruct();
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.treeRebuildBranch = get(TreeRebuildBranch.class);
        this.treeFallBranch = get(TreeFallBranch.class);
    }

    // Strike \\

    // True when the strike counted — wood the held tool cannot cut turns it aside
    boolean strike(EntityInstance entity, TreeCastStruct cast, Vector3 direction) {

        if (!cast.isWood()) {
            knockLeaf(entity, cast);
            return true;
        }

        TreeInstance tree = cast.getTree();

        if (!canCut(entity, tree.getTreeHandle().getWood()))
            return false;

        notch(entity, tree, cast, direction);
        return true;
    }

    private boolean canCut(EntityInstance entity, TreeWoodStruct wood) {

        ItemInstance held = entity.getInventoryHandle().getMainHand();

        if (held == null)
            return false;

        ItemDefinitionHandle tool = held.getItemDefinitionHandle();

        return tool.getToolTypeID() == wood.getToolTypeID() && tool.getToolTier() >= wood.getToolTier();
    }

    // Leaves \\

    // The struck cluster knocked off for good, sometimes letting a seed fall
    private void knockLeaf(EntityInstance entity, TreeCastStruct cast) {

        TreeInstance tree = cast.getTree();
        TreeShapeStruct shape = tree.getShape();
        int leaf = cast.getLeaf();
        int source = shape.getLeafSource(leaf);
        float centerX = shape.getLeafX(leaf);
        float centerY = shape.getLeafY(leaf);
        float centerZ = shape.getLeafZ(leaf);
        TreeGrowthStruct growth = tree.getTreeHandle().getGrowth();

        tree.breakLeaf(source, treeManager.getCurrentDay());

        if (growth.hasSeed() && BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(
                shape.getSeed() ^ EngineSetting.TREE_SEED_DROP_SALT, source, 0)) < growth.getSeedChance())
            treeFallBranch.throwDrop(tree, entity, growth.getSeedItemName(), centerX, centerY, centerZ, source);

        treeRebuildBranch.rebuildTree(tree, true);
    }

    // Wood \\

    // One stroke's wedge struck in, and the wood cut through when too little of it is left
    private void notch(EntityInstance entity, TreeInstance tree, TreeCastStruct cast, Vector3 direction) {

        TreeShapeStruct shape = tree.getShape();
        TreeNodeStruct nodes = shape.getNodes();
        int node = cast.getNode();
        int radius = node != EngineSetting.INDEX_NOT_FOUND ? nodes.getRadius(node) : EngineSetting.TREE_MIN_NODE_RADIUS;
        float[] heading = resolveHeading(cast, direction);
        int tier = entity.getInventoryHandle().getMainHand().getItemDefinitionHandle().getToolTier();
        float depth = EngineSetting.TREE_CHOP_BITE_SUB_VOXELS + tier * EngineSetting.TREE_CHOP_TIER_BITE_SUB_VOXELS;

        // Sub-voxels from the centre of the root: the struck cell's middle, drawn back to the wood's surface
        float mouthX = cast.getCellX() + 0.5f - center - heading[0] * EngineSetting.TREE_CHOP_MOUTH_SUB_VOXELS;
        float mouthY = cast.getCellY() + 0.5f;
        float mouthZ = cast.getCellZ() + 0.5f - center - heading[1] * EngineSetting.TREE_CHOP_MOUTH_SUB_VOXELS;

        TreeCarveStruct carve = new TreeCarveStruct(
                mouthX, mouthY, mouthZ, heading[0], heading[1],
                radius + EngineSetting.TREE_CHOP_WIDTH_MARGIN_SUB_VOXELS,
                EngineSetting.TREE_CHOP_HALF_HEIGHT_SUB_VOXELS,
                depth);

        tree.addCarve(carve, treeManager.getCurrentDay());

        if (node != EngineSetting.INDEX_NOT_FOUND && isCutThrough(tree, node, carve)) {
            sever(entity, tree, shape, nodes.getSegment(node), carve, heading);
            return;
        }

        treeRebuildBranch.rebuildTree(tree, true);
    }

    // Level and unit length: the swing's own heading, or straight into the face struck when it swings plumb
    private float[] resolveHeading(TreeCastStruct cast, Vector3 direction) {

        float length = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        if (length > EngineSetting.TREE_CHOP_LEVEL_EPSILON)
            return new float[] { direction.x / length, direction.z / length };

        float inward = cast.isFacePositive() ? -1f : 1f;

        if (cast.getFaceAxis() == EngineSetting.AXIS_X)
            return new float[] { inward, 0f };

        if (cast.getFaceAxis() == EngineSetting.AXIS_Z)
            return new float[] { 0f, inward };

        return new float[] { 1f, 0f };
    }

    // Cut Through \\

    // True when, across the wood's main axis through the notch's middle, less than a share of what the node held
    // still stands
    private boolean isCutThrough(TreeInstance tree, int node, TreeCarveStruct carve) {

        TreeShapeStruct shape = tree.getShape();
        int segment = shape.getNodes().getSegment(node);
        int axis = resolveMainAxis(shape, segment);
        float[] mouth = { carve.getCenterX() + center, carve.getCenterY(), carve.getCenterZ() + center };
        int plane = (int) Math.floor(mouth[axis]);

        int standing = countSlice(shape, node, axis, plane);
        int whole = countSlice(shape.withCarves(new TreeCarveStruct[0]), node, axis, plane);

        return whole > 0 && standing <= whole * EngineSetting.TREE_SEVER_REMAINING_SHARE;
    }

    // The filled sub-voxels in one plane across an axis, around the node, in sub-voxels from the root block's corner
    private int countSlice(TreeShapeStruct shape, int node, int axis, int plane) {

        TreeNodeStruct nodes = shape.getNodes();
        int reach = (nodes.getRadius(node) + resolution - 1) / resolution + 1;
        int[] nodeBlock = { nodes.getX(node), nodes.getY(node), nodes.getZ(node) };
        int[] minBlock = new int[EngineSetting.AXIS_COUNT];

        for (int other = 0; other < EngineSetting.AXIS_COUNT; other++)
            minBlock[other] = other == axis ? Math.floorDiv(plane, resolution) : nodeBlock[other] - reach;

        int span = (reach * 2 + 1) * resolution;
        int[] size = { span, span, span };

        size[axis] = resolution;

        grid.clear();
        TreeRasterUtility.rasterize(
                shape, -minBlock[0], -minBlock[1], -minBlock[2], 0, 0, 0, size[0], size[1], size[2], 0, grid);

        int slice = plane - minBlock[axis] * resolution;
        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;
        int[] cell = new int[EngineSetting.AXIS_COUNT];
        int count = 0;

        cell[axis] = slice;

        for (int u = 0; u < size[uAxis]; u++)
            for (int v = 0; v < size[vAxis]; v++) {

                cell[uAxis] = u;
                cell[vAxis] = v;

                if (grid.isFilled(cell[0], cell[1], cell[2]))
                    count++;
            }

        return count;
    }

    private int resolveMainAxis(TreeShapeStruct shape, int segment) {

        float dx = Math.abs(shape.getEndX(segment) - shape.getStartX(segment));
        float dy = Math.abs(shape.getEndY(segment) - shape.getStartY(segment));
        float dz = Math.abs(shape.getEndZ(segment) - shape.getStartZ(segment));

        if (dy >= dx && dy >= dz)
            return EngineSetting.AXIS_Y;

        return dx >= dz ? EngineSetting.AXIS_X : EngineSetting.AXIS_Z;
    }

    // Sever \\

    // Everything beyond the notch let go — the cut lies where the notch's middle meets the segment's axis
    private void sever(
            EntityInstance entity,
            TreeInstance tree,
            TreeShapeStruct before,
            int segment,
            TreeCarveStruct carve,
            float[] heading) {

        float startX = before.getStartX(segment);
        float startY = before.getStartY(segment);
        float startZ = before.getStartZ(segment);
        float axisX = before.getEndX(segment) - startX;
        float axisY = before.getEndY(segment) - startY;
        float axisZ = before.getEndZ(segment) - startZ;
        float lengthSquared = axisX * axisX + axisY * axisY + axisZ * axisZ;
        float cutX = carve.getCenterX() / resolution;
        float cutY = carve.getCenterY() / resolution;
        float cutZ = carve.getCenterZ() / resolution;
        float along = lengthSquared > 0f
                ? Math.max(0f, Math.min(1f,
                        ((cutX - startX) * axisX + (cutY - startY) * axisY + (cutZ - startZ) * axisZ) / lengthSquared))
                : 0f;
        float share = before.getSourceStart(segment)
                + (before.getSourceEnd(segment) - before.getSourceStart(segment)) * along;
        boolean toppling = resolveMainAxis(before, segment) == EngineSetting.AXIS_Y;

        TreeShapeStruct piece = tree.sever(before.getSourceSegment(segment), share, treeManager.getCurrentDay());

        treeFallBranch.fell(tree, piece, entity, cutX, cutY, cutZ, heading[0], heading[1], toppling);
    }
}
