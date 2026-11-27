package editor.bootstrap.layoutplannerpipeline.layoutplanner;

import editor.runtime.EditorSetting;

public enum LayoutPlannerRole {

    /*
     * Every role a settlement type names that the Layout Planner edits, a
     * choice row of its node's Properties each: the node it belongs to, its
     * group in the type's file, empty for the file's own fields, its key and
     * the name it is shown by. A role is chosen from every role the
     * architectures fill, so the type only ever names one they build. Each
     * constant's name opens with the role key prefix, which tells its rows
     * from the numbers'.
     */

    ROLE_CENTER(LayoutPlannerNode.SETTLEMENT, "", "center", EditorSetting.LAYOUT_PLANNER_LABEL_CENTER),
    ROLE_FILL(LayoutPlannerNode.LOTS, "lots", "fill", EditorSetting.LAYOUT_PLANNER_LABEL_FILL);

    public final LayoutPlannerNode node;
    public final String group;
    public final String key;
    public final String label;

    LayoutPlannerRole(LayoutPlannerNode node, String group, String key, String label) {
        this.node = node;
        this.group = group;
        this.key = key;
        this.label = label;
    }
}
