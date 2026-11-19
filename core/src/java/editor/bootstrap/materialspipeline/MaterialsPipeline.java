package editor.bootstrap.materialspipeline;

import editor.bootstrap.materialspipeline.materialsmanager.MaterialsManager;
import engine.root.PipelinePackage;

public class MaterialsPipeline extends PipelinePackage {

    /*
     * Registers the editor's one list of materials, every texture of every
     * baked pack, and the material chosen from it, shared by every window
     * that lays a material: the Hair tab's strands and the Item Editor's parts
     * alike.
     */

    // Internal \\

    @Override
    protected void create() {
        create(MaterialsManager.class);
    }
}
