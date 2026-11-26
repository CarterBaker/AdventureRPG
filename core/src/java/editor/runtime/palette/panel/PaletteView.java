package editor.runtime.palette.panel;

import editor.bootstrap.propertiespipeline.properties.PaletteProvider;

public interface PaletteView {

    /*
     * One kind of thing the Palette tab offers to pick from. Refreshing fills
     * the tab when what the view shows has moved since, or whenever forced,
     * as when it has just taken the tab over, and says whether it filled it.
     * Clearing takes everything the view put there away, so the next view
     * starts from nothing.
     */

    boolean refresh(PaletteProvider provider, boolean force);

    void clear();
}
