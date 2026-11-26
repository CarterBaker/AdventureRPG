package editor.bootstrap.propertiespipeline.properties;

import editor.bootstrap.propertiespipeline.util.PaletteContent;

public interface PaletteProvider {

    /*
     * Names what the shared Palette tab offers to pick from while this window
     * is the one worked in last among those that have a palette, which may
     * change as the window changes layer. What is picked there is the
     * window's own shared choice, so every tab sees it at once.
     */

    PaletteContent getPaletteContent();
}
