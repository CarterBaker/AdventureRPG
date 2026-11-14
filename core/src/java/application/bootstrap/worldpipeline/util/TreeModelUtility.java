package application.bootstrap.worldpipeline.util;

import java.util.Arrays;

import application.bootstrap.worldpipeline.tree.TreeDetail;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeModelStruct;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public final class TreeModelUtility extends EngineUtility {

    /*
     * Draws a tree coarser than in full, in the very bark and leaf layouts
     * and materials the grown tree uses, so it keeps its place, its lean and
     * its crown as it passes from one tier to the next. MODEL and SKETCH read
     * what stands of the tree: its own shape when an axe or a hand altered
     * it, so a stump stands as a stump, or else the skeleton its seed grows,
     * evaluated at its age in a thread's scratch and never kept. Its trunk is
     * laid as boxes of bark, one box running on up the trunk while the trunk
     * keeps within its width and girth, and MODEL adds its thick limbs as
     * short boxes stepping along them. Its leaf clusters merge by the cells
     * of a lattice laid over the crown, finer for MODEL than for SKETCH, each
     * cell drawn as one cluster reaching over the clusters it holds. OUTLINE
     * grows nothing: a trunk box under the lumps its form lays its crown out
     * as, sized from its mature height and age. Positions are in blocks of
     * the caller's frame.
     */

    // Emit \\

    // One tree whose root centre stands at a point of the frame, its height the world's own
    public static void emit(
            TreeInstance tree,
            float rootX,
            float rootZ,
            TreeDetail detail,
            TreeModelStruct model,
            FloatArrayList bark,
            FloatArrayList leaves) {

        if (detail == TreeDetail.OUTLINE && !tree.isAltered()) {
            emitOutline(tree, rootX, rootZ, model, bark, leaves);
            return;
        }

        gather(tree, model);

        TreeHandle treeHandle = tree.getTreeHandle();
        float rootY = tree.getBaseY();
        boolean modelled = detail == TreeDetail.MODEL;

        emitTrunk(treeHandle, model, rootX, rootY, rootZ, modelled, bark);

        if (modelled)
            emitLimbs(treeHandle, model, rootX, rootY, rootZ, bark);

        emitCrown(
                treeHandle, model, rootX, rootY, rootZ,
                modelled ? EngineSetting.TREE_MODEL_CELLS_PER_AXIS : EngineSetting.TREE_SKETCH_CELLS_PER_AXIS,
                modelled ? EngineSetting.TREE_MODEL_MAX_TIERS : EngineSetting.TREE_SKETCH_MAX_TIERS,
                leaves);
    }

    // Gather \\

    // What stands of the tree, from its own shape once altered and from its seed's skeleton otherwise
    private static void gather(TreeInstance tree, TreeModelStruct model) {

        model.clear();

        if (tree.isAltered()) {
            gatherShape(tree.getShape(), model);
            return;
        }

        TreeSkeletonUtility.grow(tree.getTreeHandle(), tree.getSeed(), model.getSkeleton());
        TreeShapeUtility.evaluateModel(
                tree.getTreeHandle(), tree.getSeed(), model.getSkeleton(), tree.getAge(), model);
    }

    private static void gatherShape(TreeShapeStruct shape, TreeModelStruct model) {

        for (int segment = 0; segment < shape.getSegmentCount(); segment++)
            model.addSegment(
                    shape.getStartX(segment), shape.getStartY(segment), shape.getStartZ(segment),
                    shape.getEndX(segment), shape.getEndY(segment), shape.getEndZ(segment),
                    shape.getStartRadius(segment), shape.getEndRadius(segment), shape.getDepth(segment));

        for (int leaf = 0; leaf < shape.getLeafCount(); leaf++)
            model.addLeaf(
                    shape.getLeafX(leaf), shape.getLeafY(leaf), shape.getLeafZ(leaf),
                    shape.getLeafRadiusH(leaf), shape.getLeafRadiusV(leaf),
                    shape.getLeafSeed(leaf), shape.isLeafAccent(leaf));
    }

    // Trunk \\

    // The trunk's segments as boxes of bark, a box running on while the next segment carries on from its top
    // within its width and girth; the top of a stem shows, so its last box is capped when modelled
    private static void emitTrunk(
            TreeHandle treeHandle,
            TreeModelStruct model,
            float rootX,
            float rootY,
            float rootZ,
            boolean modelled,
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

                emitRun(treeHandle, model, first, last, rootX, rootY, rootZ, modelled && !joins, bark);
            }

            first = segment;
            last = segment;
        }

        if (first != EngineSetting.INDEX_NOT_FOUND)
            emitRun(treeHandle, model, first, last, rootX, rootY, rootZ, modelled, bark);
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

    // Every limb thick enough to show past the crown, as a few capped boxes stepping along each of its segments:
    // first where every limb sets out, then on along them while the model has boxes to spare
    private static void emitLimbs(
            TreeHandle treeHandle,
            TreeModelStruct model,
            float rootX,
            float rootY,
            float rootZ,
            FloatArrayList bark) {

        int boxes = emitLimbSegments(treeHandle, model, rootX, rootY, rootZ, true, 0, bark);

        emitLimbSegments(treeHandle, model, rootX, rootY, rootZ, false, boxes, bark);
    }

    // The limb segments that set out from their parent, or those carrying a limb on, up to the model's boxes — the
    // boxes spent by the end
    private static int emitLimbSegments(
            TreeHandle treeHandle,
            TreeModelStruct model,
            float rootX,
            float rootY,
            float rootZ,
            boolean settingOut,
            int boxes,
            FloatArrayList bark) {

        int previous = EngineSetting.INDEX_NOT_FOUND;

        for (int segment = 0; segment < model.getSegmentCount(); segment++) {

            if (model.getDepth(segment) != EngineSetting.TREE_MODEL_LIMB_DEPTH)
                continue;

            boolean carriesOn = previous != EngineSetting.INDEX_NOT_FOUND && joins(model, previous, segment);

            previous = segment;

            if (carriesOn == settingOut
                    || model.getStartRadius(segment) < EngineSetting.TREE_MODEL_LIMB_MIN_RADIUS_BLOCKS)
                continue;

            boxes = emitLimbSegment(treeHandle, model, segment, rootX, rootY, rootZ, boxes, bark);

            if (boxes >= EngineSetting.TREE_MODEL_MAX_LIMB_BOXES)
                break;
        }

        return boxes;
    }

    // One segment as capped boxes stepping along it, each no wider than the limb across — the boxes spent by the end
    private static int emitLimbSegment(
            TreeHandle treeHandle,
            TreeModelStruct model,
            int segment,
            float rootX,
            float rootY,
            float rootZ,
            int boxes,
            FloatArrayList bark) {

        float radius = TreeImpostorUtility.resolveHalfWidth(
                (model.getStartRadius(segment) + model.getEndRadius(segment)) * 0.5f);
        float dx = model.getEndX(segment) - model.getStartX(segment);
        float dy = model.getEndY(segment) - model.getStartY(segment);
        float dz = model.getEndZ(segment) - model.getStartZ(segment);
        float reach = (float) Math.sqrt(dx * dx + dz * dz);
        int pieces = Math.clamp((int) Math.ceil(reach / (radius * 2f)), 1, EngineSetting.TREE_MODEL_LIMB_PIECES);

        for (int piece = 0; piece < pieces; piece++) {

            if (boxes >= EngineSetting.TREE_MODEL_MAX_LIMB_BOXES)
                break;

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
            boxes++;
        }

        return boxes;
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

        int barkCorner = EngineSetting.TREE_PART_BARK * EngineSetting.TREE_PART_CORNER_FLOATS;
        float[] corners = treeHandle.getPartCorners();

        TreeMeshUtility.emitWoodBox(
                minX, minY, minZ, maxX, maxY, maxZ,
                corners[barkCorner], corners[barkCorner + 1],
                treeHandle.getPartColors()[EngineSetting.TREE_PART_BARK],
                capped, model.getBox(), bark);
    }

    // Crown \\

    // The clusters merged by a lattice of cube cells over their centres, as many across as given and as many tiers
    // as fit up to the most given, each cell drawn as one cluster over the bounds of those it holds
    private static void emitCrown(
            TreeHandle treeHandle,
            TreeModelStruct model,
            float rootX,
            float rootY,
            float rootZ,
            int cellsPerAxis,
            int maxTiers,
            FloatArrayList leaves) {

        int count = model.getLeafCount();

        if (count == 0)
            return;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;

        for (int leaf = 0; leaf < count; leaf++) {
            minX = Math.min(minX, model.getLeafX(leaf));
            minY = Math.min(minY, model.getLeafY(leaf));
            minZ = Math.min(minZ, model.getLeafZ(leaf));
            maxX = Math.max(maxX, model.getLeafX(leaf));
            maxY = Math.max(maxY, model.getLeafY(leaf));
            maxZ = Math.max(maxZ, model.getLeafZ(leaf));
        }

        float cellSize = Math.max(
                Math.max(Math.max(maxX - minX, maxZ - minZ) / cellsPerAxis, (maxY - minY) / maxTiers),
                EngineSetting.TREE_MODEL_MIN_CELL_BLOCKS);
        int cellsX = resolveCells(maxX - minX, cellSize, cellsPerAxis);
        int cellsY = resolveCells(maxY - minY, cellSize, maxTiers);
        int cellsZ = resolveCells(maxZ - minZ, cellSize, cellsPerAxis);
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
        float[] corners = treeHandle.getPartCorners();

        TreeMeshUtility.emitCluster(
                rootX + (minX + maxX) * 0.5f,
                rootY + (minY + maxY) * 0.5f,
                rootZ + (minZ + maxZ) * 0.5f,
                Math.max(maxX - minX, maxZ - minZ) * 0.5f * reach,
                (maxY - minY) * 0.5f * reach,
                corners[part * EngineSetting.TREE_PART_CORNER_FLOATS],
                corners[part * EngineSetting.TREE_PART_CORNER_FLOATS + 1],
                treeHandle.getPartColors()[part],
                cells[cell + EngineSetting.TREE_MODEL_CELL_SEED],
                leaves);
    }

    // Outline \\

    // A tree nothing has altered, sized from its species, mature height and age: a trunk box rising into the lumps
    // its form lays its crown out as, each lump's seed rolled from the tree's
    private static void emitOutline(
            TreeInstance tree,
            float rootX,
            float rootZ,
            TreeModelStruct model,
            FloatArrayList bark,
            FloatArrayList leaves) {

        TreeHandle treeHandle = tree.getTreeHandle();
        float[] crown = model.getCrown();
        float[] lumps = model.getLumps();
        float rootY = tree.getBaseY();

        TreeImpostorUtility.estimateCrown(treeHandle, tree.getMatureHeight(), tree.getAge(), crown);

        float halfWidth = crown[EngineSetting.TREE_IMPOSTOR_TRUNK_HALF_WIDTH];
        float trunkTop = crown[EngineSetting.TREE_IMPOSTOR_TRUNK_TOP];

        if (trunkTop > 0f)
            emitBarkBox(
                    treeHandle,
                    model,
                    rootX - halfWidth, rootY - EngineSetting.TREE_TRUNK_SINK_BLOCKS, rootZ - halfWidth,
                    rootX + halfWidth, rootY + trunkTop, rootZ + halfWidth,
                    false, bark);

        if (crown[EngineSetting.TREE_IMPOSTOR_CROWN_RADIUS] <= 0f)
            return;

        int count = TreeImpostorUtility.layoutCrown(treeHandle.getForm(), crown, lumps);
        int leafCorner = EngineSetting.TREE_PART_LEAF * EngineSetting.TREE_PART_CORNER_FLOATS;
        float[] corners = treeHandle.getPartCorners();

        for (int lump = 0; lump < count; lump++) {

            int offset = lump * EngineSetting.TREE_IMPOSTOR_LUMP_FLOATS;

            TreeMeshUtility.emitCluster(
                    rootX,
                    rootY + lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_CENTER_Y],
                    rootZ,
                    lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_RADIUS_H],
                    lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_RADIUS_V],
                    corners[leafCorner], corners[leafCorner + 1],
                    treeHandle.getPartColors()[EngineSetting.TREE_PART_LEAF],
                    BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(
                            tree.getSeed() ^ EngineSetting.TREE_IMPOSTOR_SEED_SALT, lump, 0)),
                    leaves);
        }
    }
}
