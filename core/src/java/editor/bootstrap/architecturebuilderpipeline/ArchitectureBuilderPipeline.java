package editor.bootstrap.architecturebuilderpipeline;

import editor.bootstrap.architecturebuilderpipeline.architecturebuildermanager.ArchitectureBuilderManager;
import engine.root.PipelinePackage;

public class ArchitectureBuilderPipeline extends PipelinePackage {

    /*
     * Registers the architecture the Architecture Builder works on and the
     * slot picked in it, so every builder tab, the Layers tab and the Palette
     * tab show the same style blueprint.
     */

    // Internal \\

    @Override
    protected void create() {
        create(ArchitectureBuilderManager.class);
    }
}
