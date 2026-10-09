package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.tree.TreeBranchStruct;
import application.bootstrap.worldpipeline.tree.TreeForm;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeLeafStruct;
import application.bootstrap.worldpipeline.tree.TreeRandomStruct;
import application.bootstrap.worldpipeline.tree.TreeSkeletonStruct;
import application.bootstrap.worldpipeline.tree.TreeTrunkStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public final class TreeSkeletonUtility extends EngineUtility {

    /*
     * Grows a tree species to maturity from one seed, as a pure function of
     * the two, so every chunk a tree reaches grows the same tree. Each stem
     * rises from the root as a chain of segments, swelling into a root flare,
     * leaning, meandering and narrowing as it climbs, and a palm's bends ever
     * further toward its lean. The form then sets out the crown: a broadleaf,
     * weeping or shrub sends limbs from the upper trunk that fork level by
     * level into twigs with leaves at their tips, a conifer rings its leader
     * with whorls of boughs that shorten toward the top, a columnar tree
     * spirals short upswept boughs all along it, and a palm arches fronds from
     * its crown. Every limb bends under the species' gravity and wanders a
     * little, and a weeping tree's twigs hang curtains of leaves. A segment is
     * born at the age it first appears, each fork after the one it grows
     * from, so a young tree is the grown one in miniature with its later twigs
     * still to come.
     */

    private static final float TWO_PI = (float) (Math.PI * 2.0);

    // Grow \\

    public static TreeSkeletonStruct grow(TreeHandle treeHandle, long seed) {

        TreeSkeletonStruct skeleton = new TreeSkeletonStruct();
        TreeRandomStruct random = new TreeRandomStruct(seed);
        TreeTrunkStruct trunk = treeHandle.getTrunk();
        float height = random.nextRange(trunk.getMinHeightBlocks(), trunk.getMaxHeightBlocks());
        float baseAzimuth = random.next() * TWO_PI;
        int stems = trunk.getStems();

        skeleton.setMatureHeight(height);

        for (int stem = 0; stem < stems; stem++)
            growStem(treeHandle, skeleton, random, height, baseAzimuth + stem * TWO_PI / stems, stems > 1);

        return skeleton;
    }

    // Stem \\

    private static void growStem(
            TreeHandle treeHandle,
            TreeSkeletonStruct skeleton,
            TreeRandomStruct random,
            float height,
            float stemAzimuth,
            boolean multiStem) {

        TreeTrunkStruct trunk = treeHandle.getTrunk();
        TreeLeafStruct leaves = treeHandle.getLeaves();

        float spread = multiStem
                ? trunk.getStemSpreadBlocks() * random.nextRange(EngineSetting.TREE_STEM_MIN_SPREAD_SHARE, 1f)
                : 0f;
        float leanAzimuth = multiStem ? stemAzimuth : random.next() * TWO_PI;
        float lean = (float) Math.toRadians(trunk.getLeanDegrees())
                * random.nextRange(EngineSetting.TREE_LEAN_MIN_SHARE, 1f);
        float stemHeight = multiStem
                ? height * random.nextRange(EngineSetting.TREE_STEM_MIN_HEIGHT_SHARE, 1f)
                : height;
        float leaderHeight = stemHeight * trunk.getLeader();
        float radius = trunk.getRadiusBlocks();

        float[] position = {
                (float) Math.cos(stemAzimuth) * spread,
                -EngineSetting.TREE_TRUNK_SINK_BLOCKS,
                (float) Math.sin(stemAzimuth) * spread };
        float[] direction = {
                (float) (Math.sin(lean) * Math.cos(leanAzimuth)),
                (float) Math.cos(lean),
                (float) (Math.sin(lean) * Math.sin(leanAzimuth)) };

        IntArrayList trunkSegments = new IntArrayList();
        FloatArrayList trunkStarts = new FloatArrayList();

        float flareLength = Math.min(
                radius * EngineSetting.TREE_FLARE_HEIGHT_RADII + EngineSetting.TREE_TRUNK_SINK_BLOCKS,
                leaderHeight * EngineSetting.TREE_FLARE_MAX_SHARE);

        int segment = addAlong(
                skeleton, position, direction, flareLength,
                radius * (1f + trunk.getFlare()), radius,
                EngineSetting.INDEX_NOT_FOUND, 0f, 0, 0f);

        trunkSegments.add(segment);
        trunkStarts.add(0f);

        float travelled = flareLength;
        float remaining = Math.max(leaderHeight - flareLength, EngineSetting.TREE_MIN_SEGMENT_BLOCKS);
        int count = Math.max(
                EngineSetting.TREE_MIN_TRUNK_SEGMENTS,
                Math.min(EngineSetting.TREE_MAX_TRUNK_SEGMENTS,
                        (int) Math.ceil(remaining / EngineSetting.TREE_TRUNK_SEGMENT_BLOCKS)));
        float segmentLength = remaining / count;

        for (int k = 0; k < count && segment != EngineSetting.INDEX_NOT_FOUND; k++) {

            direction[0] += random.nextSigned() * trunk.getWobble();
            direction[2] += random.nextSigned() * trunk.getWobble();

            if (treeHandle.getForm() == TreeForm.PALM) {
                direction[0] += (float) Math.cos(leanAzimuth) * EngineSetting.TREE_PALM_BEND;
                direction[2] += (float) Math.sin(leanAzimuth) * EngineSetting.TREE_PALM_BEND;
            }

            normalize(direction);

            float fromShare = travelled / stemHeight;
            float toShare = (travelled + segmentLength) / stemHeight;

            trunkStarts.add(travelled);
            segment = addAlong(
                    skeleton, position, direction, segmentLength,
                    radius * lerp(1f, trunk.getTaper(), fromShare),
                    radius * lerp(1f, trunk.getTaper(), toShare),
                    segment, 1f, 0, 0f);
            trunkSegments.add(segment);
            travelled += segmentLength;
        }

        if (segment == EngineSetting.INDEX_NOT_FOUND)
            return;

        growCrown(treeHandle, skeleton, random, stemHeight, travelled, trunkSegments, trunkStarts, position);

        float tuft = leaves.getRadiusBlocks() * EngineSetting.TREE_TUFT_RADIUS_SHARE;
        skeleton.addLeaf(position[0], position[1], position[2], tuft, tuft * leaves.getSquash(), segment);
    }

    // Crown \\

    private static void growCrown(
            TreeHandle treeHandle,
            TreeSkeletonStruct skeleton,
            TreeRandomStruct random,
            float stemHeight,
            float crownTop,
            IntArrayList trunkSegments,
            FloatArrayList trunkStarts,
            float[] top) {

        TreeBranchStruct branches = treeHandle.getBranches();
        TreeForm form = treeHandle.getForm();
        float crownBottom = Math.min(stemHeight * branches.getCrownStart(), crownTop);
        float twist = (float) Math.toRadians(branches.getTwistDegrees());
        float azimuth = random.next() * TWO_PI;
        int count = random.nextInt(branches.getMinCount(), branches.getMaxCount());

        if (form == TreeForm.PALM) {
            growFronds(treeHandle, skeleton, random, stemHeight, trunkSegments, top, count, azimuth);
            return;
        }

        for (int j = 0; j < count; j++) {

            float share = (j + random.nextRange(
                    EngineSetting.TREE_LIMB_JITTER_MIN, EngineSetting.TREE_LIMB_JITTER_MAX)) / count;

            if (form == TreeForm.CONIFER)
                growWhorl(treeHandle, skeleton, random, stemHeight, share,
                        lerp(crownBottom, crownTop * EngineSetting.TREE_WHORL_TOP_SHARE, share),
                        trunkSegments, trunkStarts, azimuth + j * twist);
            else
                growLimb(treeHandle, skeleton, random, stemHeight, share,
                        lerp(crownBottom, crownTop, share),
                        trunkSegments, trunkStarts, azimuth + j * twist);
        }
    }

    // One limb from the trunk, its length and angle shaped by where in the crown it sets out
    private static void growLimb(
            TreeHandle treeHandle,
            TreeSkeletonStruct skeleton,
            TreeRandomStruct random,
            float stemHeight,
            float share,
            float along,
            IntArrayList trunkSegments,
            FloatArrayList trunkStarts,
            float azimuth) {

        TreeBranchStruct branches = treeHandle.getBranches();
        boolean columnar = treeHandle.getForm() == TreeForm.COLUMNAR;
        boolean steep = !columnar && share > 1f - EngineSetting.TREE_LIMB_TOP_SHARE;
        float angle = (float) Math.toRadians(branches.getAngleDegrees()
                * (steep ? EngineSetting.TREE_LIMB_TOP_ANGLE_SHARE : 1f)
                * random.nextRange(EngineSetting.TREE_ANGLE_JITTER_MIN, EngineSetting.TREE_ANGLE_JITTER_MAX));
        float profile = columnar
                ? EngineSetting.TREE_SPINDLE_BASE_SHARE
                        + (1f - EngineSetting.TREE_SPINDLE_BASE_SHARE) * (float) Math.sin(Math.PI * share)
                : 1f - EngineSetting.TREE_LIMB_LENGTH_FALLOFF * share;
        float length = branches.getLength() * stemHeight * profile
                * random.nextRange(EngineSetting.TREE_LENGTH_JITTER_MIN, EngineSetting.TREE_LENGTH_JITTER_MAX);

        setOut(treeHandle, skeleton, random, along, trunkSegments, trunkStarts,
                azimuth + random.nextSigned() * EngineSetting.TREE_LIMB_AZIMUTH_JITTER, angle, length);
    }

    // A conifer's whorl — a ring of near-level boughs, longest low on the leader and shortest at its tip
    private static void growWhorl(
            TreeHandle treeHandle,
            TreeSkeletonStruct skeleton,
            TreeRandomStruct random,
            float stemHeight,
            float share,
            float along,
            IntArrayList trunkSegments,
            FloatArrayList trunkStarts,
            float azimuth) {

        TreeBranchStruct branches = treeHandle.getBranches();
        int boughs = random.nextInt(EngineSetting.TREE_WHORL_MIN_BOUGHS, EngineSetting.TREE_WHORL_MAX_BOUGHS);
        float profile = EngineSetting.TREE_CONE_TIP_SHARE + (1f - EngineSetting.TREE_CONE_TIP_SHARE) * (1f - share);

        for (int b = 0; b < boughs; b++) {

            float angle = (float) Math.toRadians(branches.getAngleDegrees()
                    * random.nextRange(EngineSetting.TREE_ANGLE_JITTER_MIN, EngineSetting.TREE_ANGLE_JITTER_MAX));
            float length = branches.getLength() * stemHeight * profile
                    * random.nextRange(EngineSetting.TREE_LENGTH_JITTER_MIN, EngineSetting.TREE_LENGTH_JITTER_MAX);

            setOut(treeHandle, skeleton, random, along, trunkSegments, trunkStarts,
                    azimuth + b * TWO_PI / boughs + random.nextSigned() * EngineSetting.TREE_LIMB_AZIMUTH_JITTER,
                    angle, length);
        }
    }

    // A palm's fronds — one arching blade per frond from the crown, and no forks
    private static void growFronds(
            TreeHandle treeHandle,
            TreeSkeletonStruct skeleton,
            TreeRandomStruct random,
            float stemHeight,
            IntArrayList trunkSegments,
            float[] top,
            int count,
            float azimuth) {

        TreeBranchStruct branches = treeHandle.getBranches();
        int topSegment = trunkSegments.getInt(trunkSegments.size() - 1);
        float radius = skeleton.getEndRadius(topSegment) * branches.getRadiusRatio();

        for (int j = 0; j < count; j++) {

            float frondAzimuth = azimuth + j * TWO_PI / count
                    + random.nextSigned() * EngineSetting.TREE_LIMB_AZIMUTH_JITTER;
            float angle = (float) Math.toRadians(branches.getAngleDegrees()
                    * random.nextRange(EngineSetting.TREE_ANGLE_JITTER_MIN, EngineSetting.TREE_ANGLE_JITTER_MAX));
            float length = branches.getLength() * stemHeight
                    * random.nextRange(EngineSetting.TREE_LENGTH_JITTER_MIN, EngineSetting.TREE_LENGTH_JITTER_MAX);

            growBranch(
                    treeHandle, skeleton, random, branches.getLevels(),
                    top.clone(), toDirection(angle, frondAzimuth), length, radius, topSegment, 1f,
                    random.nextRange(EngineSetting.TREE_LIMB_BIRTH_MIN, EngineSetting.TREE_LIMB_BIRTH_MAX));
        }
    }

    // A limb set out from the trunk at a path length along it, leaving at an angle from upright
    private static void setOut(
            TreeHandle treeHandle,
            TreeSkeletonStruct skeleton,
            TreeRandomStruct random,
            float along,
            IntArrayList trunkSegments,
            FloatArrayList trunkStarts,
            float azimuth,
            float angle,
            float length) {

        int index = 0;

        while (index + 1 < trunkStarts.size() && trunkStarts.getFloat(index + 1) <= along)
            index++;

        int segment = trunkSegments.getInt(index);
        float segmentLength = Math.max(skeleton.getLength(segment), EngineSetting.TREE_MIN_SEGMENT_BLOCKS);
        float share = clamp01((along - trunkStarts.getFloat(index)) / segmentLength);
        float[] point = pointAlong(skeleton, segment, share);
        float radius = Math.max(
                EngineSetting.TREE_MIN_RADIUS_BLOCKS,
                lerp(skeleton.getStartRadius(segment), skeleton.getEndRadius(segment), share)
                        * treeHandle.getBranches().getRadiusRatio());

        growBranch(
                treeHandle, skeleton, random, 1, point, toDirection(angle, azimuth), length, radius, segment, share,
                random.nextRange(EngineSetting.TREE_LIMB_BIRTH_MIN, EngineSetting.TREE_LIMB_BIRTH_MAX));
    }

    // Branch \\

    // One branch at a fork level, bending and wandering segment by segment, then forking or leafing out
    private static void growBranch(
            TreeHandle treeHandle,
            TreeSkeletonStruct skeleton,
            TreeRandomStruct random,
            int level,
            float[] position,
            float[] direction,
            float length,
            float radius,
            int parent,
            float attach,
            float birth) {

        TreeBranchStruct branches = treeHandle.getBranches();
        TreeLeafStruct leaves = treeHandle.getLeaves();
        TreeForm form = treeHandle.getForm();
        int segments = branches.getSegments();
        float segmentLength = Math.max(length / segments, EngineSetting.TREE_MIN_SEGMENT_BLOCKS);
        float tipRadius = Math.max(EngineSetting.TREE_MIN_RADIUS_BLOCKS,
                radius * EngineSetting.TREE_BRANCH_TIP_RADIUS_SHARE);
        boolean terminal = level >= branches.getLevels() || branches.getChildren() == 0;
        float gravity = branches.getGravity()
                - (form == TreeForm.WEEPING && terminal ? EngineSetting.TREE_WEEPING_DROOP : 0f);
        IntArrayList chain = new IntArrayList(segments);
        int segment = parent;
        float share = attach;

        for (int i = 0; i < segments; i++) {

            direction[1] += gravity;
            direction[0] += random.nextSigned() * EngineSetting.TREE_BRANCH_WANDER;
            direction[2] += random.nextSigned() * EngineSetting.TREE_BRANCH_WANDER;
            normalize(direction);

            segment = addAlong(
                    skeleton, position, direction, segmentLength,
                    lerp(radius, tipRadius, (float) i / segments),
                    lerp(radius, tipRadius, (float) (i + 1) / segments),
                    segment, share, level, birth + i * EngineSetting.TREE_BRANCH_GROW_SPAN / segments);

            if (segment == EngineSetting.INDEX_NOT_FOUND)
                return;

            share = 1f;
            chain.add(segment);

            if (leavesAlong(form)) {

                float remaining = 1f - (float) (i + 1) / segments;
                float leafRadius = leaves.getRadiusBlocks()
                        * lerp(EngineSetting.TREE_ALONG_LEAF_TIP_SHARE, 1f, remaining);

                skeleton.addLeaf(position[0], position[1], position[2],
                        leafRadius, leafRadius * leaves.getSquash(), segment);
            }
        }

        if (!terminal)
            growChildren(treeHandle, skeleton, random, level, chain, length, birth);

        if (terminal)
            growTipLeaves(treeHandle, skeleton, random, chain, position);

        if (terminal && form == TreeForm.WEEPING && leaves.getHangBlocks() > 0f)
            growStrand(treeHandle, skeleton, random, position, segment, birth);
    }

    private static void growChildren(
            TreeHandle treeHandle,
            TreeSkeletonStruct skeleton,
            TreeRandomStruct random,
            int level,
            IntArrayList chain,
            float length,
            float birth) {

        TreeBranchStruct branches = treeHandle.getBranches();
        boolean pinnate = treeHandle.getForm() == TreeForm.CONIFER || treeHandle.getForm() == TreeForm.COLUMNAR;
        float twist = (float) Math.toRadians(branches.getTwistDegrees());
        float firstShare = pinnate ? EngineSetting.TREE_PINNATE_FIRST_SHARE : EngineSetting.TREE_CHILD_FIRST_SHARE;
        int children = Math.max(1, branches.getChildren() + random.nextInt(-1, 1));
        int segments = chain.size();

        for (int k = 0; k < children; k++) {

            float share = lerp(firstShare, 1f, (k + random.nextRange(
                    EngineSetting.TREE_LIMB_JITTER_MIN, EngineSetting.TREE_LIMB_JITTER_MAX)) / children);
            int index = Math.min((int) (share * segments), segments - 1);
            float local = clamp01(share * segments - index);
            int segment = chain.getInt(index);
            float[] point = pointAlong(skeleton, segment, local);
            float[] axis = directionOf(skeleton, segment);
            float angle = (float) Math.toRadians(branches.getChildAngleDegrees()
                    * random.nextRange(EngineSetting.TREE_ANGLE_JITTER_MIN, EngineSetting.TREE_ANGLE_JITTER_MAX));
            float roll = pinnate
                    ? (k % 2 == 0 ? 0f : (float) Math.PI)
                    : k * twist + random.nextSigned() * EngineSetting.TREE_LIMB_AZIMUTH_JITTER;
            float childLength = length * branches.getChildLength()
                    * random.nextRange(EngineSetting.TREE_LENGTH_JITTER_MIN, EngineSetting.TREE_LENGTH_JITTER_MAX)
                    * (1f - EngineSetting.TREE_CHILD_LENGTH_FALLOFF * share);
            float childRadius = Math.max(
                    EngineSetting.TREE_MIN_RADIUS_BLOCKS,
                    lerp(skeleton.getStartRadius(segment), skeleton.getEndRadius(segment), local)
                            * branches.getRadiusRatio());
            float childBirth = Math.max(
                    skeleton.getBirth(segment) + EngineSetting.TREE_BRANCH_GROW_SPAN,
                    birth + EngineSetting.TREE_LEVEL_BIRTH_STEP * random.nextRange(
                            EngineSetting.TREE_LENGTH_JITTER_MIN, EngineSetting.TREE_LENGTH_JITTER_MAX));

            growBranch(
                    treeHandle, skeleton, random, level + 1, point,
                    turn(axis, angle, roll, pinnate), childLength, childRadius, segment, local, childBirth);
        }
    }

    // Leaves \\

    // A cluster at the twig tip, and a smaller one halfway along a long twig to fill the crown out
    private static void growTipLeaves(
            TreeHandle treeHandle,
            TreeSkeletonStruct skeleton,
            TreeRandomStruct random,
            IntArrayList chain,
            float[] tip) {

        TreeLeafStruct leaves = treeHandle.getLeaves();
        int last = chain.getInt(chain.size() - 1);
        float radius = leaves.getRadiusBlocks()
                * random.nextRange(EngineSetting.TREE_LENGTH_JITTER_MIN, EngineSetting.TREE_LENGTH_JITTER_MAX);

        skeleton.addLeaf(tip[0], tip[1], tip[2], radius, radius * leaves.getSquash(), last);

        if (chain.size() < EngineSetting.TREE_MID_LEAF_MIN_SEGMENTS)
            return;

        int middle = chain.getInt(chain.size() / 2);
        float midRadius = radius * EngineSetting.TREE_MID_LEAF_SHARE;

        skeleton.addLeaf(
                skeleton.getEndX(middle), skeleton.getEndY(middle), skeleton.getEndZ(middle),
                midRadius, midRadius * leaves.getSquash(), middle);
    }

    // A weeping twig's hanging strand — thin, almost straight down, leafed all along like a curtain
    private static void growStrand(
            TreeHandle treeHandle,
            TreeSkeletonStruct skeleton,
            TreeRandomStruct random,
            float[] tip,
            int parent,
            float birth) {

        TreeLeafStruct leaves = treeHandle.getLeaves();
        float[] position = tip.clone();
        float[] direction = {
                random.nextSigned() * EngineSetting.TREE_STRAND_SWAY,
                -1f,
                random.nextSigned() * EngineSetting.TREE_STRAND_SWAY };
        float length = leaves.getHangBlocks()
                * random.nextRange(EngineSetting.TREE_LENGTH_JITTER_MIN, EngineSetting.TREE_LENGTH_JITTER_MAX);
        float segmentLength = length / EngineSetting.TREE_STRAND_SEGMENTS;
        float leafRadius = leaves.getRadiusBlocks() * EngineSetting.TREE_STRAND_LEAF_SHARE;
        int segment = parent;

        normalize(direction);

        for (int i = 0; i < EngineSetting.TREE_STRAND_SEGMENTS; i++) {

            segment = addAlong(
                    skeleton, position, direction, segmentLength,
                    EngineSetting.TREE_MIN_RADIUS_BLOCKS, EngineSetting.TREE_MIN_RADIUS_BLOCKS,
                    segment, 1f, EngineSetting.TREE_STRAND_DEPTH,
                    birth + EngineSetting.TREE_BRANCH_GROW_SPAN);

            if (segment == EngineSetting.INDEX_NOT_FOUND)
                return;

            skeleton.addLeaf(position[0], position[1], position[2],
                    leafRadius, leafRadius * EngineSetting.TREE_STRAND_STRETCH, segment);
        }
    }

    private static boolean leavesAlong(TreeForm form) {
        return form == TreeForm.CONIFER || form == TreeForm.COLUMNAR || form == TreeForm.SHRUB
                || form == TreeForm.PALM;
    }

    // Geometry \\

    // A segment from the position along the direction, the position moved to its end
    private static int addAlong(
            TreeSkeletonStruct skeleton,
            float[] position,
            float[] direction,
            float length,
            float fromRadius,
            float toRadius,
            int parent,
            float attach,
            int depth,
            float birth) {

        float toX = position[0] + direction[0] * length;
        float toY = position[1] + direction[1] * length;
        float toZ = position[2] + direction[2] * length;

        int segment = skeleton.addSegment(
                position[0], position[1], position[2], toX, toY, toZ,
                fromRadius, toRadius, parent, attach, depth, birth);

        position[0] = toX;
        position[1] = toY;
        position[2] = toZ;

        return segment;
    }

    private static float[] pointAlong(TreeSkeletonStruct skeleton, int segment, float share) {
        return new float[] {
                lerp(skeleton.getStartX(segment), skeleton.getEndX(segment), share),
                lerp(skeleton.getStartY(segment), skeleton.getEndY(segment), share),
                lerp(skeleton.getStartZ(segment), skeleton.getEndZ(segment), share) };
    }

    private static float[] directionOf(TreeSkeletonStruct skeleton, int segment) {

        float[] direction = {
                skeleton.getEndX(segment) - skeleton.getStartX(segment),
                skeleton.getEndY(segment) - skeleton.getStartY(segment),
                skeleton.getEndZ(segment) - skeleton.getStartZ(segment) };

        normalize(direction);

        return direction;
    }

    // A direction at an angle from upright, turned to an azimuth around it
    private static float[] toDirection(float angle, float azimuth) {
        return new float[] {
                (float) (Math.sin(angle) * Math.cos(azimuth)),
                (float) Math.cos(angle),
                (float) (Math.sin(angle) * Math.sin(azimuth)) };
    }

    // The axis turned away from itself by an angle, toward a side chosen by the roll around it — a pinnate fork
    // keeps to the level side, so boughs lie flat in rows
    private static float[] turn(float[] axis, float angle, float roll, boolean pinnate) {

        float[] side = Math.abs(axis[1]) < EngineSetting.TREE_UPRIGHT_LIMIT
                ? cross(axis, 0f, 1f, 0f)
                : cross(axis, 1f, 0f, 0f);

        normalize(side);

        float[] up = cross(axis, side[0], side[1], side[2]);
        float rollCos = (float) Math.cos(roll);
        float rollSin = pinnate ? 0f : (float) Math.sin(roll);
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float[] turned = {
                axis[0] * cos + (side[0] * rollCos + up[0] * rollSin) * sin,
                axis[1] * cos + (side[1] * rollCos + up[1] * rollSin) * sin,
                axis[2] * cos + (side[2] * rollCos + up[2] * rollSin) * sin };

        normalize(turned);

        return turned;
    }

    private static float[] cross(float[] a, float bx, float by, float bz) {
        return new float[] {
                a[1] * bz - a[2] * by,
                a[2] * bx - a[0] * bz,
                a[0] * by - a[1] * bx };
    }

    private static void normalize(float[] vector) {

        float length = (float) Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);

        if (length <= 0f) {
            vector[0] = 0f;
            vector[1] = 1f;
            vector[2] = 0f;
            return;
        }

        vector[0] /= length;
        vector[1] /= length;
        vector[2] /= length;
    }

    private static float lerp(float from, float to, float share) {
        return from + (to - from) * share;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
