package editor.bootstrap.propertiespipeline.util;

public enum PropertiesContent {

    /*
     * What the Properties tab shows for the window worked in last: Rows, the
     * rows of a tool window, what it picks from listed first and each number
     * of what is picked on a slider; Info, the content entry selected in the
     * Hierarchy with every field to edit. What a window picks from lives in
     * the Palette tab (PaletteContent), never here. The ordinal indexes the
     * tab's views, so the order is fixed.
     */

    ROWS,
    INFO;

    // Values
    public static final PropertiesContent[] VALUES = values();
}
