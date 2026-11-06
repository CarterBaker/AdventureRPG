package editor.runtime.biomepalette;

import editor.runtime.biomepalette.menu.BiomePaletteMenuSystem;
import engine.root.ContextPackage;

public class BiomePaletteContext extends ContextPackage {

    /*
     * Editor tab holding the biomes the World Editor paints with: every biome
     * the world image can paint, the selected one highlighted. Choosing a row
     * here chooses the biome every World Editor paints, and shows it in the
     * Hierarchy and Info Panel; the heights are shaped in the Height Editor
     * and never listed here. The palette lives in the editor.
     */

    // Internal
    private BiomePaletteMenuSystem biomePaletteMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.biomePaletteMenuSystem = create(BiomePaletteMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
