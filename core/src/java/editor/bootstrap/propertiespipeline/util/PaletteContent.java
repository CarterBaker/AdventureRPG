package editor.bootstrap.propertiespipeline.util;

public enum PaletteContent {

    /*
     * What the Palette tab offers to pick from for the window worked in last
     * that has a palette: Image, the colour wheel over the rows of a window
     * that paints (the maps of its image, and what else it picks from); Rows,
     * the list of what a tool picks from (parts, bones, gear, nodes); Biomes,
     * the biomes the World Editor paints as a grid of swatches; or None, for
     * a window with nothing to pick from in what it works on now. The ordinal indexes the tab's views, so the order is
     * fixed.
     */

    IMAGE,
    ROWS,
    BIOMES,
    NONE;

    // Values
    public static final PaletteContent[] VALUES = values();
}
