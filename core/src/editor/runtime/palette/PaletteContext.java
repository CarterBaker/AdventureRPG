package editor.runtime.palette;

import editor.runtime.palette.menu.PaletteMenuSystem;
import engine.root.ContextPackage;

public class PaletteContext extends ContextPackage {

    /*
     * Editor tab holding what the World Editor paints with: every biome the
     * world image can paint, or every height level while the height layer is
     * painted, the selected one highlighted. Choosing a row here chooses the
     * brush of every World Editor at once; the palette lives in the editor.
     */

    // Internal
    private PaletteMenuSystem paletteMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.paletteMenuSystem = create(PaletteMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
