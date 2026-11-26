package editor.bootstrap.propertiespipeline.util;

public enum PaletteContent {

    /*
     * What the Palette tab offers to pick from for the window worked in last
     * that has a palette: Image, the colour wheel over the maps of the image
     * it paints; Biomes, the biomes the World Editor paints as a grid of
     * swatches; or None, for a window with nothing to pick from in its
     * current layer. The ordinal indexes the tab's views, so the order is
     * fixed.
     */

    IMAGE,
    BIOMES,
    NONE;

    // Values
    public static final PaletteContent[] VALUES = values();
}
