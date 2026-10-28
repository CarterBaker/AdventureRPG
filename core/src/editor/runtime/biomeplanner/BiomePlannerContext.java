package editor.runtime.biomeplanner;

import editor.runtime.biomeplanner.menu.BiomePlannerMenuSystem;
import engine.root.ContextPackage;

public class BiomePlannerContext extends ContextPackage {

    /*
     * Editor tab for making biomes, the way the Settlement Planner is for
     * settlements: every biome the world can paint, the selected one
     * highlighted, and a grid of cells around it whose centre is always the
     * selected biome. Clicking a cell paints the selected biome into it, so
     * a biome can be set beside the ones it is meant to meet; the Biome
     * Viewer flies over the world the grid lays out. New and Duplicate make
     * biomes through the Info Panel, where every field of one is edited.
     */

    // Internal
    private BiomePlannerMenuSystem biomePlannerMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.biomePlannerMenuSystem = create(BiomePlannerMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
