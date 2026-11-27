package editor.bootstrap.structureeditorpipeline;

import editor.bootstrap.structureeditorpipeline.structureeditormanager.StructureEditorManager;
import engine.root.PipelinePackage;

public class StructureEditorPipeline extends PipelinePackage {

    /*
     * Registers the structure the Structure Editor paints, its layer picked
     * and what its Palette holds, so every structure editor tab, the Layers
     * tab and the Palette tab work on the same structure.
     */

    // Internal \\

    @Override
    protected void create() {
        create(StructureEditorManager.class);
    }
}
