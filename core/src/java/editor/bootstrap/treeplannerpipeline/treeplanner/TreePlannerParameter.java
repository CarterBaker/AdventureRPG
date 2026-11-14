package editor.bootstrap.treeplannerpipeline.treeplanner;

import application.bootstrap.worldpipeline.tree.TreeForm;
import engine.root.EngineSetting;

public enum TreePlannerParameter {

    /*
     * Every field of a tree species the Tree Planner steps, one row of its
     * node's panel each: the node it belongs to, where it lies in the species
     * file — its group, empty for the file's own fields, its key, and for one
     * end of a range which end — the name it is shown by, how far one step
     * moves it, the bounds it is held within, whether it is whole, the value
     * it reads as while the file leaves it out, and for a choice the enum
     * whose names it cycles through. The two ends of a range never cross.
     */

    FORM(TreePlannerNode.TREE, "", "form", EngineSetting.INDEX_NOT_FOUND, "Form",
            1f, 0f, TreeForm.values().length - 1, true, 0f, TreeForm.class),

    HEIGHT_LOW(TreePlannerNode.TRUNK, "trunk", "height_blocks", 0, "Height (low)",
            1f, 1f, EngineSetting.TREE_MAX_HEIGHT_BLOCKS, false, EngineSetting.TREE_DEFAULT_MIN_HEIGHT_BLOCKS, null),
    HEIGHT_HIGH(TreePlannerNode.TRUNK, "trunk", "height_blocks", 1, "Height (high)",
            1f, 1f, EngineSetting.TREE_MAX_HEIGHT_BLOCKS, false, EngineSetting.TREE_DEFAULT_MAX_HEIGHT_BLOCKS, null),
    TRUNK_RADIUS(TreePlannerNode.TRUNK, "trunk", "radius_blocks", EngineSetting.INDEX_NOT_FOUND, "Radius",
            0.05f, 0.05f, EngineSetting.TREE_MAX_TRUNK_RADIUS_BLOCKS, false,
            EngineSetting.TREE_DEFAULT_TRUNK_RADIUS_BLOCKS, null),
    TAPER(TreePlannerNode.TRUNK, "trunk", "taper", EngineSetting.INDEX_NOT_FOUND, "Taper",
            0.05f, 0f, 1f, false, EngineSetting.TREE_DEFAULT_TRUNK_TAPER, null),
    FLARE(TreePlannerNode.TRUNK, "trunk", "flare", EngineSetting.INDEX_NOT_FOUND, "Flare",
            0.05f, 0f, 2f, false, EngineSetting.TREE_DEFAULT_TRUNK_FLARE, null),
    LEADER(TreePlannerNode.TRUNK, "trunk", "leader", EngineSetting.INDEX_NOT_FOUND, "Leader",
            0.05f, 0f, 1f, false, EngineSetting.TREE_DEFAULT_TRUNK_LEADER, null),
    LEAN(TreePlannerNode.TRUNK, "trunk", "lean_degrees", EngineSetting.INDEX_NOT_FOUND, "Lean (deg)",
            1f, 0f, 45f, false, EngineSetting.TREE_DEFAULT_TRUNK_LEAN_DEGREES, null),
    WOBBLE(TreePlannerNode.TRUNK, "trunk", "wobble", EngineSetting.INDEX_NOT_FOUND, "Wobble",
            0.01f, 0f, 0.5f, false, EngineSetting.TREE_DEFAULT_TRUNK_WOBBLE, null),
    STEMS(TreePlannerNode.TRUNK, "trunk", "stems", EngineSetting.INDEX_NOT_FOUND, "Stems",
            1f, 1f, EngineSetting.TREE_MAX_STEMS, true, EngineSetting.TREE_DEFAULT_TRUNK_STEMS, null),
    STEM_SPREAD(TreePlannerNode.TRUNK, "trunk", "stem_spread_blocks", EngineSetting.INDEX_NOT_FOUND, "Stem spread",
            0.25f, 0f, 8f, false, EngineSetting.TREE_DEFAULT_TRUNK_STEM_SPREAD_BLOCKS, null),

    CROWN_START(TreePlannerNode.LIMBS, "branches", "crown_start", EngineSetting.INDEX_NOT_FOUND, "Crown start",
            0.05f, 0f, 1f, false, EngineSetting.TREE_DEFAULT_CROWN_START, null),
    COUNT_LOW(TreePlannerNode.LIMBS, "branches", "count", 0, "Limbs (low)",
            1f, 0f, EngineSetting.TREE_MAX_BRANCH_COUNT, true, EngineSetting.TREE_DEFAULT_MIN_BRANCH_COUNT, null),
    COUNT_HIGH(TreePlannerNode.LIMBS, "branches", "count", 1, "Limbs (high)",
            1f, 0f, EngineSetting.TREE_MAX_BRANCH_COUNT, true, EngineSetting.TREE_DEFAULT_MAX_BRANCH_COUNT, null),
    ANGLE(TreePlannerNode.LIMBS, "branches", "angle_degrees", EngineSetting.INDEX_NOT_FOUND, "Angle (deg)",
            5f, 0f, 180f, false, EngineSetting.TREE_DEFAULT_BRANCH_ANGLE_DEGREES, null),
    LENGTH(TreePlannerNode.LIMBS, "branches", "length", EngineSetting.INDEX_NOT_FOUND, "Length",
            0.05f, 0f, 2f, false, EngineSetting.TREE_DEFAULT_BRANCH_LENGTH, null),
    RADIUS_RATIO(TreePlannerNode.LIMBS, "branches", "radius_ratio", EngineSetting.INDEX_NOT_FOUND, "Girth ratio",
            0.05f, 0f, 1f, false, EngineSetting.TREE_DEFAULT_BRANCH_RADIUS_RATIO, null),
    GRAVITY(TreePlannerNode.LIMBS, "branches", "gravity", EngineSetting.INDEX_NOT_FOUND, "Gravity",
            0.01f, -1f, 1f, false, EngineSetting.TREE_DEFAULT_BRANCH_GRAVITY, null),
    TWIST(TreePlannerNode.LIMBS, "branches", "twist_degrees", EngineSetting.INDEX_NOT_FOUND, "Twist (deg)",
            2.5f, 0f, 360f, false, EngineSetting.TREE_DEFAULT_BRANCH_TWIST_DEGREES, null),
    SEGMENTS(TreePlannerNode.LIMBS, "branches", "segments", EngineSetting.INDEX_NOT_FOUND, "Segments",
            1f, 1f, EngineSetting.TREE_MAX_BRANCH_SEGMENTS, true, EngineSetting.TREE_DEFAULT_BRANCH_SEGMENTS, null),

    LEVELS(TreePlannerNode.TWIGS, "branches", "levels", EngineSetting.INDEX_NOT_FOUND, "Levels",
            1f, 0f, EngineSetting.TREE_MAX_BRANCH_LEVELS, true, EngineSetting.TREE_DEFAULT_BRANCH_LEVELS, null),
    CHILDREN(TreePlannerNode.TWIGS, "branches", "children", EngineSetting.INDEX_NOT_FOUND, "Twigs per level",
            1f, 0f, EngineSetting.TREE_MAX_BRANCH_CHILDREN, true, EngineSetting.TREE_DEFAULT_BRANCH_CHILDREN, null),
    CHILD_LENGTH(TreePlannerNode.TWIGS, "branches", "child_length", EngineSetting.INDEX_NOT_FOUND, "Twig length",
            0.05f, 0f, 1f, false, EngineSetting.TREE_DEFAULT_BRANCH_CHILD_LENGTH, null),
    CHILD_ANGLE(TreePlannerNode.TWIGS, "branches", "child_angle_degrees", EngineSetting.INDEX_NOT_FOUND,
            "Twig angle (deg)", 5f, 0f, 180f, false, EngineSetting.TREE_DEFAULT_BRANCH_CHILD_ANGLE_DEGREES, null),

    LEAF_RADIUS(TreePlannerNode.LEAVES, "leaves", "radius_blocks", EngineSetting.INDEX_NOT_FOUND, "Cluster radius",
            0.1f, 0f, 16f, false, EngineSetting.TREE_DEFAULT_LEAF_RADIUS_BLOCKS, null),
    SQUASH(TreePlannerNode.LEAVES, "leaves", "squash", EngineSetting.INDEX_NOT_FOUND, "Squash",
            0.05f, 0.05f, 2f, false, EngineSetting.TREE_DEFAULT_LEAF_SQUASH, null),
    DENSITY(TreePlannerNode.LEAVES, "leaves", "density", EngineSetting.INDEX_NOT_FOUND, "Density",
            0.05f, 0f, 1f, false, EngineSetting.TREE_DEFAULT_LEAF_DENSITY, null),
    ACCENT_CHANCE(TreePlannerNode.LEAVES, "leaves", "accent_chance", EngineSetting.INDEX_NOT_FOUND, "Accent chance",
            0.01f, 0f, 1f, false, EngineSetting.TREE_DEFAULT_ACCENT_CHANCE, null),
    HANG(TreePlannerNode.LEAVES, "leaves", "hang_blocks", EngineSetting.INDEX_NOT_FOUND, "Hang",
            0.1f, 0f, 8f, false, EngineSetting.TREE_DEFAULT_HANG_BLOCKS, null),

    BARK_DEPTH(TreePlannerNode.WOOD, "wood", "bark_sub_voxels", EngineSetting.INDEX_NOT_FOUND, "Bark depth",
            1f, 1f, EngineSetting.SUB_VOXEL_RESOLUTION, true, EngineSetting.TREE_DEFAULT_BARK_SUB_VOXELS, null),
    TOOL_TIER(TreePlannerNode.WOOD, "wood", "tool_tier", EngineSetting.INDEX_NOT_FOUND, "Tool tier",
            1f, 0f, 10f, true, EngineSetting.DEFAULT_TOOL_TIER, null),

    DAYS(TreePlannerNode.GROWTH, "growth", "days", EngineSetting.INDEX_NOT_FOUND, "Days to grow",
            1f, 1f, 365f, false, EngineSetting.TREE_DEFAULT_GROWTH_DAYS, null),
    WILD_AGE_LOW(TreePlannerNode.GROWTH, "growth", "wild_age", 0, "Wild age (low)",
            0.05f, 0f, 1f, false, EngineSetting.TREE_DEFAULT_WILD_MIN_AGE, null),
    WILD_AGE_HIGH(TreePlannerNode.GROWTH, "growth", "wild_age", 1, "Wild age (high)",
            0.05f, 0f, 1f, false, EngineSetting.TREE_DEFAULT_WILD_MAX_AGE, null),
    SEED_CHANCE(TreePlannerNode.GROWTH, "growth", "seed_chance", EngineSetting.INDEX_NOT_FOUND, "Seed chance",
            0.05f, 0f, 1f, false, EngineSetting.TREE_DEFAULT_SEED_CHANCE, null);

    public final TreePlannerNode node;
    public final String group;
    public final String key;
    public final int end;
    public final String label;
    public final float step;
    public final float min;
    public final float max;
    public final boolean whole;
    public final float fallback;
    public final Class<? extends Enum<?>> choices;

    TreePlannerParameter(
            TreePlannerNode node,
            String group,
            String key,
            int end,
            String label,
            float step,
            float min,
            float max,
            boolean whole,
            float fallback,
            Class<? extends Enum<?>> choices) {

        this.node = node;
        this.group = group;
        this.key = key;
        this.end = end;
        this.label = label;
        this.step = step;
        this.min = min;
        this.max = max;
        this.whole = whole;
        this.fallback = fallback;
        this.choices = choices;
    }

    // True for one end of a range, the other end of which is its partner
    public boolean isRangeEnd() {
        return end != EngineSetting.INDEX_NOT_FOUND;
    }
}
