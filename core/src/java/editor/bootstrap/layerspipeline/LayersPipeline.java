package editor.bootstrap.layerspipeline;

import editor.bootstrap.layerspipeline.layersmanager.LayersManager;
import engine.root.PipelinePackage;

public class LayersPipeline extends PipelinePackage {

    /*
     * Registers the editor's one Layers panel's target, shared by every
     * window that works on layers, so whichever window was worked in last
     * fills the Layers tab with its layer stack.
     */

    // Internal \\

    @Override
    protected void create() {
        create(LayersManager.class);
    }
}
