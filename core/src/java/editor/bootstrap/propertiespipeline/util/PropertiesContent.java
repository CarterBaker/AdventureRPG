package editor.bootstrap.propertiespipeline.util;

public enum PropertiesContent {

    /*
     * What the Properties tab shows for the window worked in last: Rows, the
     * rows of a tool window, what it picks from listed first and each number
     * of what is picked on a slider; Info, the content entry selected in the
     * Hierarchy with every field to edit; or Biomes, the biomes the World
     * Editor paints as a palette of swatches with the Info rows below it.
     * The ordinal indexes the tab's views, so the order is fixed.
     */

    ROWS,
    INFO,
    BIOMES;

    // Values
    public static final PropertiesContent[] VALUES = values();
}
