package editor.bootstrap.toolpipeline.util;

public enum ToolOptionKind {

    /*
     * How the Tools panel shows and sets an option, as a paint program shows
     * each kind of setting: a number on a slider between step buttons with a
     * box that opens for typing, a switch flipped on or off by one click, and
     * a choice of named settings picked from a drop-down list under its row.
     * Each kind has its own row template in ToolsSetting.TEMPLATE_OPTION_ROWS,
     * in this order.
     */

    NUMBER,
    SWITCH,
    CHOICE;

    // Values
    public static final ToolOptionKind[] VALUES = values();
}
