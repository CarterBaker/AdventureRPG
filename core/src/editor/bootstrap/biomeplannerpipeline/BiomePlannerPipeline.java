package editor.bootstrap.biomeplannerpipeline;

import editor.bootstrap.biomeplannerpipeline.biomeplannermanager.BiomePlannerManager;
import engine.root.PipelinePackage;

public class BiomePlannerPipeline extends PipelinePackage {

    /*
     * Registers the biome planner, whose small world of biomes laid in a grid
     * the Biome Viewer flies over, so a biome is seen generated, beside the
     * biomes it is meant to meet, while it is made.
     */

    // Internal \\

    @Override
    protected void create() {
        create(BiomePlannerManager.class);
    }
}
