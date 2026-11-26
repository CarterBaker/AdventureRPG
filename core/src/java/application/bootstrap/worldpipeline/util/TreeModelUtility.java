package application.bootstrap.worldpipeline.util;

import java.util.Arrays;

import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeModelStruct;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public final class TreeModelUtility extends EngineUtility {

    /*
     * Draws a tree at a coarse level of the LOD ladder, in the very bark and
     * leaf layouts and materials the grown tree uses, so it keeps its place,
     * its lean and its crown as it passes from one level to the next. A level
     * of its own reads what stands of the tree: its own shape when an axe or
     * a hand altered it, so a stump stands as a stump, or else the skeleton
     * its seed grows, evaluated at its age in a thread's scratch and never
     * kept. Its leaf clusters merge by the cells of a lattice laid over the
     * crown, coarser each level, each cell drawn as one cluster reaching over
     * the clusters it holds. Its wood is judged against the same cells: a
     * limb is drawn while it is as thick as its level's girth or while any
     * cluster it carries lies past half a cell from where it sets out, and
     * the limb it sets out from is drawn whenever it is, so every cluster
     * drawn lies within the reach of drawn wood and none ever floats, on a
     * sapling and a giant alike. The trunk is laid as boxes of bark, one box
     * running on up the trunk while the trunk keeps within its width and
     * girth, and every limb drawn as short boxes stepping along it. From the
     * shared level on a tree nothing has altered whose species has an
     * impostor is drawn by TreeImpostorRenderSystem instead, so nothing is
     * laid for it here. Positions are in blocks of the caller's frame.
     */

    // Emit \\

    // One tree whose root centre stands at a point of the frame, its height the world's own, drawn at a LOD; none
    // at all while its impostor draws it
    public static void emit(
            TreeInstance tree,
            float rootX,
            float rootZ,
            int lod,
            TreeModelStruct model,
            FloatArrayList bark,
            FloatArrayList leaves) {

        if (TreeLodUtility.isImpostor(tree.getTreeHandle(), lod, tree.isAltered()))
            return;

        int level = TreeLodUtility.toLevel(lod);

        model.clear();

        if (tree.isAltered())
            gatherShape(tree.getShape(), model);
        else
            gatherSeed(tree.getTreeHandle(), tree.getSeed(), tree.getAge(), model);

        emitLevel(tree.getTreeHandle(), model, rootX, tree.getBaseY(), rootZ, level, bark, leaves);
    }

    // A tree known only by its species, seed and age, never registered, drawn at a LOD: what distant terrain
    // draws each tree it stands in as, none at all while its impostor draws it
    public static void emitSite(
            TreeHandle treeHandle,
            long seed,
            float age,
            float rootX,
            float rootY,
            float rootZ,
            int lod,
            TreeModelStruct model,
            FloatArrayList bark,
            FloatArrayList leaves) {

        if (TreeLodUtility.isImpostor(treeHandle, lod, false))
            return;

        model.clear();
        gatherSeed(treeHandle, seed, age, model);
        emitLevel(treeHandle, model, rootX, rootY, rootZ, TreeLodUtility.toLevel(lod), bark, leaves);
    }

    // A shape alone at the finest coarse level, its root centre at a point of the frame — a piece of tree too great
    // to draw sub-voxel by sub-voxel
    public static void emitShape(
            TreeShapeStruct shape,
            float rootX,
            float rootY,
            float rootZ,
            TreeModelStruct model,
            FloatArrayList bark,
            FloatArrayList leaves) {

        model.clear();
        gatherShape(shape, model);
        emitLevel(shape.getTreeHandle(), model, rootX, rootY, rootZ, EngineSetting.TREE_LOD_FULL + 1, bark, leaves);
    }

    // What was gathered of a tree drawn at one of its own coarse levels
    private static void emitLevel(
            TreeHandle treeHandle,
            TreeModelStruct model,
            float rootX,
            float rootY,
            float rootZ,
            int level,
            FloatArrayList bark,
            FloatArrayList leaves) {

        float cellSize = resolveCellSize(model, level);

        judgeWood(model, level, cellSize);
        emitTrunk(treeHandle, model, rootX, rootY, rootZ, bark);
        emitLimbs(treeHandle, model, rootX, rootY, rootZ, bark);
        emitCrown(treeHandle, model, rootX, rootY, rootZ, level, cellSize, leaves);
    }

    // Gather \\

    // The skeleton a seed grows, evaluated at an age
    private static void gatherSeed(TreeHandle treeHandle, long seed, float age, TreeModelStruct model) {
        TreeSkeletonUtility.grow(treeHandle, seed, model.getSkeleton());
        TreeShapeUtility.evaluateModel(treeHandle, seed, model.getSkeleton(), age, model);
    }

    // What stands of an altered tree, each piece linked through the skeleton its seed grows to the piece before it
    // on the same limb or to the limb it sets out from
    private static void gatherShape(TreeShapeStruct shape, TreeModelStruct model) {

        TreeSkeletonUtility.grow(shape.getTreeHandle(), shape.getSeed(), model.getSkeleton());

        for (int segment = 0; segment < shape.getSegmentCount(); segment++) {

            int source = shape.getSourceSegment(segment);
            int previous = model.getSegmentOfSource(source);
            int parent = previous != EngineSetting.INDEX_NOT_FOUND
                    ? previous
                    : model.getSegmentOfSource(model.getSkeleton().getParent(source));

            model.setSegmentOfSource(source, model.getSegmentCount());
            model.addSegment(
                    shape.getStartX(segment), shape.getStartY(segment), shape.getStartZ(segment),
                    shape.getEndX(segment), shape.getEndY(segment), shape.getEndZ(segment),
                    shape.getStartRadius(segment), shape.getEndRadius(segment), shape.getDepth(segment), parent);
        }

        for (int leaf = 0; leaf < shape.getLeafCount(); leaf++)
            model.addLeaf(
                    shape.getLeafX(leaf), shape.getLeafY(leaf), shape.getLeafZ(leaf),
                    shape.getLeafRadiusH(leaf), shape.getLeafRadiusV(leaf),
                    shape.getLeafSeed(leaf), shape.isLeafAccent(leaf),
                    model.getSegmentOfSource(model.getSkeleton().getLeafSegment(shape.getLeafSource(leaf))));
    }

    // Wood \\

    // Every segment marked drawn or hidden at a level: the trunk always, a limb at its level's girth or carrying a
    // cluster past half a cell, and every limb a drawn one sets out from
    private static void judgeWood(TreeModelStruct model, int level, float cellSize) {

        model.resetCarry();

        for (int leaf = 0; leaf < model.getLeafCount(); leaf++)
            carryLeaf(model, leaf);

        float girth = TreeLodUtility.resolveGirth(level);
        float cover = cellSize * EngineSetting.TREE_LOD_COVER_SHARE;

        for (int segment = 0; segment < model.getSegmentCount(); segment++)
            model.setKept(segment, model.getDepth(segment) == 0
                    || model.getStartRadius(segment) >= girth
                    || model.getReach(segment) > cover);

        for (int segment = model.getSegmentCount() - 1; segment >= 0; segment--) {

            int parent = model.getParent(segment);

            if (model.isKept(segment) && parent != EngineSetting.INDEX_NOT_FOUND)
                model.setKept(parent, true);
        }
    }

    // A cluster's reach past its rim taken into every segment carrying it, from where each sets out
    private static void carryLeaf(TreeModelStruct model, int leaf) {

        float leafX = model.getLeafX(leaf);
        float leafY = model.getLeafY(leaf);
        float leafZ = model.getLeafZ(leaf);
        float rim = model.getLeafRadiusH(leaf);

        for (int segment = model.getLeafSegment(leaf); segment != EngineSetting.INDEX_NOT_FOUND;
                segment = model.getParent(segment)) {

            float dx = leafX - model.getStartX(segment);
            float dy = leafY - model.getStartY(segment);
            float dz = leafZ - model.getStartZ(segment);
            float reach = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) - rim;

            if (reach > model.getReach(segment))
                model.setReach(segment, reach);
        }
    }

    // Trunk \\

    // The trunk's segments as boxes of bark, a box running on while the next segment carries on from its top
    // within its width and girth; the top of a stem shows, so its last box is capped
    private static void emitTrunk(
            TreeHandle treeHandle,
            TreeModelStruct model,
            float rootX,
            float rootY,
            float rootZ,
            FloatArrayList bark) {

        int first = EngineSetting.INDEX_NOT_FOUND;
        int last = EngineSetting.INDEX_NOT_FOUND;

        for (int segment = 0; segment < model.getSegmentCount(); segment++) {

            if (model.getDepth(segment) != 0)
                continue;

            if (first != EngineSetting.INDEX_NOT_FOUND) {

                boolean joins = joins(model, last, segment);

                if (joins && keepsRun(model, first, segment)) {
                    last = segment;
                    continue;
                }

                emitRun(treeHandle, model, first, last, rootX, rootY, rootZ, !joins, bark);
            }

            first = segment;
            last = segment;
        }

        if (first != EngineSetting.INDEX_NOT_FOUND)
            emitRun(treeHandle, model, first, last, rootX, rootY, rootZ, true, bark);
    }

    // True when a segment sets out from where another ends
    private static boolean joins(TreeModelStruct model, int segment, int next) {

        float dx = model.getStartX(next) - model.getEndX(segment);
        float dy = model.getStartY(next) - model.getEndY(segment);
        float dz = model.getStartZ(next) - model.getEndZ(segment);
        float reach = EngineSetting.TREE_MODEL_JOIN_BLOCKS;

        return dx * dx + dy * dy + dz * dz <= reach * reach;
    }

    // True while a segment's end stays over the run's foot and its girth near the foot's
    private static boolean keepsRun(TreeModelStruct model, int first, int segment) {

        float radius = model.getStartRadius(first);
        float driftX = model.getEndX(segment) - model.getStartX(first);
        float driftZ = model.getEndZ(segment) - model.getStartZ(first);
        float drift = radius * EngineSetting.TREE_MODEL_RUN_DRIFT_SHARE;

        return driftX * driftX + driftZ * driftZ <= drift * drift
                && model.getEndRadius(segment) >= radius * EngineSetting.TREE_MODEL_RUN_GIRTH_SHARE;
    }

    // One box from a run's foot to its top, centred between them, sunk into the ground where it stands on it
    private static void emitRun(
            TreeHandle treeHandle,
            TreeModelStruct model,
            int first,
            int last,
            float rootX,
            float rootY,
            float rootZ,
            boolean capped,
            FloatArrayList bark) {

        float centerX = rootX + (model.getStartX(first) + model.getEndX(last)) * 0.5f;
        float centerZ = rootZ + (model.getStartZ(first) + model.getEndZ(last)) * 0.5f;
        float halfWidth = TreeImpostorUtility.resolveHalfWidth(
                (model.getStartRadius(first) + model.getEndRadius(last)) * 0.5f);
        float bottom = model.getStartY(first) <= EngineSetting.TREE_SHARE_EPSILON
                ? -EngineSetting.TREE_TRUNK_SINK_BLOCKS
                : Math.min(model.getStartY(first), model.getEndY(last));
        float top = Math.max(model.getStartY(first), model.getEndY(last));

        emitBarkBox(
                treeHandle,
                model,
                centerX - halfWidth, rootY + bottom, centerZ - halfWidth,
                centerX + halfWidth, rootY + top, centerZ + halfWidth,
                capped, bark);
    }

    // Limbs \\

    // Every limb segment the level draws, as capped boxes stepping along it
    private static void emitLimbs(
            TreeHandle treeHandle,
            TreeModelStruct model,
            float rootX,
            float rootY,
            float rootZ,
            FloatArrayList bark) {

        for (int segment = 0; segment < model.getSegmentCount(); segment++)
            if (model.getDepth(segment) != 0 && model.isKept(segment))
                emitLimbSegment(treeHandle, model, segment, rootX, rootY, rootZ, bark);
    }

    // One segment as capped boxes stepping along it, each no wider than the limb across
    private static void emitLimbSegment(
            TreeHandle treeHandle,
            TreeModelStruct model,
            int segment,
            float rootX,
            float rootY,
            float rootZ,
            FloatArrayList bark) {

        float radius = TreeImpostorUtility.resolveHalfWidth(
                (model.getStartRadius(segment) + model.getEndRadius(segment)) * 0.5f);
        float dx = model.getEndX(segment) - model.getStartX(segment);
        float dy = model.getEndY(segment) - model.getStartY(segment);
        float dz = model.getEndZ(segment) - model.getStartZ(segment);
        float reach = (float) Math.sqrt(dx * dx + dz * dz);
        int pieces = Math.clamp((int) Math.ceil(reach / (radius * 2f)), 1, EngineSetting.TREE_MODEL_LIMB_PIECES);

        for (int piece = 0; piece < pieces; piece++) {

            float from = (float) piece / pieces;
            float to = (float) (piece + 1) / pieces;
            float fromX = rootX + model.getStartX(segment) + dx * from;
            float fromY = rootY + model.getStartY(segment) + dy * from;
            float fromZ = rootZ + model.getStartZ(segment) + dz * from;
            float toX = rootX + model.getStartX(segment) + dx * to;
            float toY = rootY + model.getStartY(segment) + dy * to;
            float toZ = rootZ + model.getStartZ(segment) + dz * to;

            emitBarkBox(
                    treeHandle,
                    model,
                    Math.min(fromX, toX) - radius, Math.min(fromY, toY) - radius, Math.min(fromZ, toZ) - radius,
                    Math.max(fromX, toX) + radius, Math.max(fromY, toY) + radius, Math.max(fromZ, toZ) + radius,
                    true, bark);
        }
    }

    private static void emitBarkBox(
            TreeHandle treeHandle,
            TreeModelStruct model,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            boolean capped,
            FloatArrayList bark) {

        int[] tiles = treeHandle.getPartTiles();

        TreeMeshUtility.emitWoodBox(
                minX, minY, minZ, maxX, maxY, maxZ,
                tiles[EngineSetting.TREE_PART_BARK],
                treeHandle.getPartColors()[EngineSetting.TREE_PART_BARK],
                capped, model.getBox(), bark);
    }

    // Crown \\

    // The side of a level's lattice cells over the clusters' bounds, which it leaves in the model's bounds; none
    // without clusters
    private static float resolveCellSize(TreeModelStruct model, int level) {

        int count = model.getLeafCount();

        if (count == 0)
            return 0f;

        float[] bounds = model.getBounds();

        Arrays.fill(bounds, 0, EngineSetting.AXIS_COUNT, Float.MAX_VALUE);
        Arrays.fill(bounds, EngineSetting.AXIS_COUNT, EngineSetting.AXIS_COUNT * 2, -Float.MAX_VALUE);

        for (int leaf = 0; leaf < count; leaf++) {
            bounds[EngineSetting.AXIS_X] = Math.min(bounds[EngineSetting.AXIS_X], model.getLeafX(leaf));
            bounds[EngineSetting.AXIS_Y] = Math.min(bounds[EngineSetting.AXIS_Y], model.getLeafY(leaf));
            bounds[EngineSetting.AXIS_Z] = Math.min(bounds[EngineSetting.AXIS_Z], model.getLeafZ(leaf));
            bounds[EngineSetting.AXIS_COUNT + EngineSetting.AXIS_X] = Math.max(
                    bounds[EngineSetting.AXIS_COUNT + EngineSetting.AXIS_X], model.getLeafX(leaf));
            bounds[EngineSetting.AXIS_COUNT + EngineSetting.AXIS_Y] = Math.max(
                    bounds[EngineSetting.AXIS_COUNT + EngineSetting.AXIS_Y], model.getLeafY(leaf));
            bounds[EngineSetting.AXIS_COUNT + EngineSetting.AXIS_Z] = Math.max(
                    bounds[EngineSetting.AXIS_COUNT + EngineSetting.AXIS_Z], model.getLeafZ(leaf));
        }

        float spanX = bounds[EngineSetting.AXIS_COUNT + EngineSetting.AXIS_X] - bounds[EngineSetting.AXIS_X];
        float spanY = bounds[EngineSetting.AXIS_COUNT + EngineSetting.AXIS_Y] - bounds[EngineSetting.AXIS_Y];
        float spanZ = bounds[EngineSetting.AXIS_COUNT + EngineSetting.AXIS_Z] - bounds[EngineSetting.AXIS_Z];

        return Math.max(
                Math.max(
                        Math.max(spanX, spanZ) / EngineSetting.TREE_LOD_CELLS_PER_AXIS[level],
                        spanY / EngineSetting.TREE_LOD_MAX_TIERS[level]),
                EngineSetting.TREE_MODEL_MIN_CELL_BLOCKS);
    }

    // The clusters merged by a lattice of cube cells over their centres, as many across and as many tiers up as
    // the level allows, each cell drawn as one cluster over the bounds of those it holds
    private static void emitCrown(
            TreeHandle treeHandle,
            TreeModelStruct model,
            float rootX,
            float rootY,
            float rootZ,
            int level,
            float cellSize,
            FloatArrayList leaves) {

        int count = model.getLeafCount();

        if (count == 0)
            return;

        float[] bounds = model.getBounds();
        float minX = bounds[EngineSetting.AXIS_X];
        float minY = bounds[EngineSetting.AXIS_Y];
        float minZ = bounds[EngineSetting.AXIS_Z];
        int cellsX = resolveCells(bounds[EngineSetting.AXIS_COUNT + EngineSetting.AXIS_X] - minX, cellSize,
                EngineSetting.TREE_LOD_CELLS_PER_AXIS[level]);
        int cellsY = resolveCells(bounds[EngineSetting.AXIS_COUNT + EngineSetting.AXIS_Y] - minY, cellSize,
                EngineSetting.TREE_LOD_MAX_TIERS[level]);
        int cellsZ = resolveCells(bounds[EngineSetting.AXIS_COUNT + EngineSetting.AXIS_Z] - minZ, cellSize,
                EngineSetting.TREE_LOD_CELLS_PER_AXIS[level]);
        int cellCount = cellsX * cellsY * cellsZ;
        float[] cells = model.getCells();

        Arrays.fill(cells, 0, cellCount * EngineSetting.TREE_MODEL_CELL_FLOATS, 0f);

        for (int leaf = 0; leaf < count; leaf++) {

            int cellX = Math.min((int) ((model.getLeafX(leaf) - minX) / cellSize), cellsX - 1);
            int cellY = Math.min((int) ((model.getLeafY(leaf) - minY) / cellSize), cellsY - 1);
            int cellZ = Math.min((int) ((model.getLeafZ(leaf) - minZ) / cellSize), cellsZ - 1);

            addToCell(model, leaf, ((cellY * cellsZ + cellZ) * cellsX + cellX) * EngineSetting.TREE_MODEL_CELL_FLOATS);
        }

        for (int cell = 0; cell < cellCount; cell++)
            emitCell(treeHandle, cells, cell * EngineSetting.TREE_MODEL_CELL_FLOATS, rootX, rootY, rootZ, leaves);
    }

    private static int resolveCells(float span, float cellSize, int most) {
        return Math.clamp((int) Math.ceil(span / cellSize), 1, most);
    }

    // One cluster's bounds taken into its cell, the cell keeping the first cluster's seed
    private static void addToCell(TreeModelStruct model, int leaf, int cell) {

        float[] cells = model.getCells();
        float radiusH = model.getLeafRadiusH(leaf);
        float radiusV = model.getLeafRadiusV(leaf);
        float lowX = model.getLeafX(leaf) - radiusH;
        float lowY = model.getLeafY(leaf) - radiusV;
        float lowZ = model.getLeafZ(leaf) - radiusH;
        float highX = model.getLeafX(leaf) + radiusH;
        float highY = model.getLeafY(leaf) + radiusV;
        float highZ = model.getLeafZ(leaf) + radiusH;
        boolean empty = cells[cell + EngineSetting.TREE_MODEL_CELL_COUNT] == 0f;

        cells[cell + EngineSetting.TREE_MODEL_CELL_MIN_X] = empty
                ? lowX : Math.min(cells[cell + EngineSetting.TREE_MODEL_CELL_MIN_X], lowX);
        cells[cell + EngineSetting.TREE_MODEL_CELL_MIN_Y] = empty
                ? lowY : Math.min(cells[cell + EngineSetting.TREE_MODEL_CELL_MIN_Y], lowY);
        cells[cell + EngineSetting.TREE_MODEL_CELL_MIN_Z] = empty
                ? lowZ : Math.min(cells[cell + EngineSetting.TREE_MODEL_CELL_MIN_Z], lowZ);
        cells[cell + EngineSetting.TREE_MODEL_CELL_MAX_X] = empty
                ? highX : Math.max(cells[cell + EngineSetting.TREE_MODEL_CELL_MAX_X], highX);
        cells[cell + EngineSetting.TREE_MODEL_CELL_MAX_Y] = empty
                ? highY : Math.max(cells[cell + EngineSetting.TREE_MODEL_CELL_MAX_Y], highY);
        cells[cell + EngineSetting.TREE_MODEL_CELL_MAX_Z] = empty
                ? highZ : Math.max(cells[cell + EngineSetting.TREE_MODEL_CELL_MAX_Z], highZ);

        if (empty)
            cells[cell + EngineSetting.TREE_MODEL_CELL_SEED] = model.getLeafSeed(leaf);

        if (model.isLeafAccent(leaf))
            cells[cell + EngineSetting.TREE_MODEL_CELL_ACCENTS]++;

        cells[cell + EngineSetting.TREE_MODEL_CELL_COUNT]++;
    }

    // A cell holding clusters drawn as one, pulled in from its bounds when it merges several so it never swells
    // past what they fill, and wearing the accent when most of them do
    private static void emitCell(
            TreeHandle treeHandle,
            float[] cells,
            int cell,
            float rootX,
            float rootY,
            float rootZ,
            FloatArrayList leaves) {

        float count = cells[cell + EngineSetting.TREE_MODEL_CELL_COUNT];

        if (count == 0f)
            return;

        float minX = cells[cell + EngineSetting.TREE_MODEL_CELL_MIN_X];
        float minY = cells[cell + EngineSetting.TREE_MODEL_CELL_MIN_Y];
        float minZ = cells[cell + EngineSetting.TREE_MODEL_CELL_MIN_Z];
        float maxX = cells[cell + EngineSetting.TREE_MODEL_CELL_MAX_X];
        float maxY = cells[cell + EngineSetting.TREE_MODEL_CELL_MAX_Y];
        float maxZ = cells[cell + EngineSetting.TREE_MODEL_CELL_MAX_Z];
        float reach = count > 1f ? EngineSetting.TREE_MODEL_MERGE_REACH_SHARE : 1f;
        int part = cells[cell + EngineSetting.TREE_MODEL_CELL_ACCENTS] * 2f > count
                ? EngineSetting.TREE_PART_ACCENT
                : EngineSetting.TREE_PART_LEAF;
        int[] tiles = treeHandle.getPartTiles();

        TreeMeshUtility.emitCluster(
                rootX + (minX + maxX) * 0.5f,
                rootY + (minY + maxY) * 0.5f,
                rootZ + (minZ + maxZ) * 0.5f,
                Math.max(maxX - minX, maxZ - minZ) * 0.5f * reach,
                (maxY - minY) * 0.5f * reach,
                tiles[part],
                treeHandle.getPartColors()[part],
                cells[cell + EngineSetting.TREE_MODEL_CELL_SEED],
                leaves);
    }
}
