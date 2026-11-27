package editor.bootstrap.layoutplannerpipeline.layoutplanner;

import application.bootstrap.worldpipeline.layout.LayoutRoadKind;
import editor.bootstrap.propertiespipeline.util.PlannerParameter;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;

public enum LayoutPlannerParameter implements PlannerParameter {

    /*
     * Every number and choice of a settlement type the Layout Planner edits,
     * one row of its node's Properties each: the node it belongs to, where it
     * lies in the type's file — its group, empty for the file's own fields,
     * its key, and for one end of a range which end — the name it is shown
     * by, how far one step moves it, the bounds it is held within, whether it
     * is whole, the value it reads as while the file leaves it out, and for a
     * choice the enum whose names it cycles through. The two ends of a range
     * never cross. The settlement's roles and the names it builds with are
     * edited in the Info Panel.
     */

    WEIGHT(LayoutPlannerNode.SETTLEMENT, "", "weight", EngineSetting.INDEX_NOT_FOUND, "Weight",
            0.1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_WEIGHT, false, 1f, null),
    RADIUS(LayoutPlannerNode.SETTLEMENT, "", "radius_blocks", EngineSetting.INDEX_NOT_FOUND, "Radius",
            1f, 1f, EditorSetting.LAYOUT_PLANNER_MAX_REACH_BLOCKS, true,
            EditorSetting.LAYOUT_PLANNER_DEFAULT_RADIUS_BLOCKS, null),
    LINK(LayoutPlannerNode.SETTLEMENT, "", "link", EngineSetting.INDEX_NOT_FOUND, "Link",
            1f, 0f, LayoutRoadKind.values().length - 1, true, LayoutRoadKind.PATH.ordinal(), LayoutRoadKind.class),
    MIN_HEIGHT(LayoutPlannerNode.GROUND, "ground", "min_height_blocks", EngineSetting.INDEX_NOT_FOUND, "Lowest ground",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_HEIGHT_BLOCKS, true,
            EngineSetting.DEFAULT_SETTLEMENT_MIN_GROUND_HEIGHT_BLOCKS, null),
    MAX_HEIGHT(LayoutPlannerNode.GROUND, "ground", "max_height_blocks", EngineSetting.INDEX_NOT_FOUND, "Highest ground",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_HEIGHT_BLOCKS, true,
            EditorSetting.LAYOUT_PLANNER_MAX_HEIGHT_BLOCKS, null),
    MAX_SLOPE(LayoutPlannerNode.GROUND, "ground", "max_slope_blocks", EngineSetting.INDEX_NOT_FOUND, "Steepest slope",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_SLOPE_BLOCKS, true,
            EngineSetting.DEFAULT_SETTLEMENT_MAX_SLOPE_BLOCKS, null),
    SPOKES_LOW(LayoutPlannerNode.STREETS, "streets", "spokes", 0, "Spokes (low)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_SPOKES, true, 1f, null),
    SPOKES_HIGH(LayoutPlannerNode.STREETS, "streets", "spokes", 1, "Spokes (high)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_SPOKES, true, 1f, null),
    SPOKE_LENGTH_LOW(LayoutPlannerNode.STREETS, "streets", "spoke_length_blocks", 0, "Spoke length (low)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_STREET_BLOCKS, true, 0f, null),
    SPOKE_LENGTH_HIGH(LayoutPlannerNode.STREETS, "streets", "spoke_length_blocks", 1, "Spoke length (high)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_STREET_BLOCKS, true, 0f, null),
    BRANCHES_LOW(LayoutPlannerNode.STREETS, "streets", "branches", 0, "Branches (low)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_BRANCHES, true, 0f, null),
    BRANCHES_HIGH(LayoutPlannerNode.STREETS, "streets", "branches", 1, "Branches (high)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_BRANCHES, true, 0f, null),
    BRANCH_LENGTH_LOW(LayoutPlannerNode.STREETS, "streets", "branch_length_blocks", 0, "Branch length (low)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_STREET_BLOCKS, true, 0f, null),
    BRANCH_LENGTH_HIGH(LayoutPlannerNode.STREETS, "streets", "branch_length_blocks", 1, "Branch length (high)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_STREET_BLOCKS, true, 0f, null),
    BEND(LayoutPlannerNode.STREETS, "streets", "bend", EngineSetting.INDEX_NOT_FOUND, "Bend",
            0.05f, 0f, 1f, false, EngineSetting.DEFAULT_SETTLEMENT_STREET_BEND, null),
    SETBACK(LayoutPlannerNode.LOTS, "lots", "setback_blocks", EngineSetting.INDEX_NOT_FOUND, "Setback",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_SETBACK_BLOCKS, true,
            EngineSetting.DEFAULT_SETTLEMENT_SETBACK_BLOCKS, null),
    MAX_LOTS(LayoutPlannerNode.LOTS, "lots", "max_lots", EngineSetting.INDEX_NOT_FOUND, "Most buildings",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_LOTS, true, 0f, null),
    OVERPASSES_LOW(LayoutPlannerNode.OVERPASSES, "", "overpasses", 0, "Overpasses (low)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_OVERPASSES, true, 0f, null),
    OVERPASSES_HIGH(LayoutPlannerNode.OVERPASSES, "", "overpasses", 1, "Overpasses (high)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_OVERPASSES, true, 0f, null),
    WALL_RADIUS(LayoutPlannerNode.WALL, "wall", "radius_blocks", EngineSetting.INDEX_NOT_FOUND, "Ring radius",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_REACH_BLOCKS, true, 0f, null),
    TOWER_SPACING(LayoutPlannerNode.WALL, "wall", "tower_spacing_blocks", EngineSetting.INDEX_NOT_FOUND,
            "Tower spacing",
            1f, 1f, EditorSetting.LAYOUT_PLANNER_MAX_TOWER_SPACING_BLOCKS, true,
            EngineSetting.DEFAULT_SETTLEMENT_TOWER_SPACING_BLOCKS, null),
    OUTPOSTS_LOW(LayoutPlannerNode.OUTPOSTS, "outposts", "count", 0, "Outposts (low)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_OUTPOSTS, true, 0f, null),
    OUTPOSTS_HIGH(LayoutPlannerNode.OUTPOSTS, "outposts", "count", 1, "Outposts (high)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_OUTPOSTS, true, 0f, null),
    TRAIL_LOW(LayoutPlannerNode.OUTPOSTS, "outposts", "trail_length_blocks", 0, "Trail length (low)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_STREET_BLOCKS, true, 0f, null),
    TRAIL_HIGH(LayoutPlannerNode.OUTPOSTS, "outposts", "trail_length_blocks", 1, "Trail length (high)",
            1f, 0f, EditorSetting.LAYOUT_PLANNER_MAX_STREET_BLOCKS, true, 0f, null),
    CHANCE(LayoutPlannerNode.OUTPOSTS, "outposts", "chance", EngineSetting.INDEX_NOT_FOUND, "Chance",
            0.05f, 0f, 1f, false, EngineSetting.DEFAULT_SETTLEMENT_OUTPOST_CHANCE, null);

    public final LayoutPlannerNode node;
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

    LayoutPlannerParameter(
            LayoutPlannerNode node,
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


    // Planner Parameter \\

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public int getEnd() {
        return end;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public float getStep() {
        return step;
    }

    @Override
    public float getMin() {
        return min;
    }

    @Override
    public float getMax() {
        return max;
    }

    @Override
    public boolean isWhole() {
        return whole;
    }

    @Override
    public float getFallback() {
        return fallback;
    }

    @Override
    public Class<? extends Enum<?>> getChoices() {
        return choices;
    }

    // Whether it is one end of a range held as a two-number array
    @Override
    public boolean isRangeEnd() {
        return end != EngineSetting.INDEX_NOT_FOUND;
    }
}
