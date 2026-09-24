package editor.bootstrap.itemeditorpipeline;

import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import engine.root.PipelinePackage;

public class ItemEditorPipeline extends PipelinePackage {

    /*
     * Registers the item editor's shared state, so every Item Editor tab and
     * hierarchy panel works on the same items.
     */

    // Internal \\

    @Override
    protected void create() {
        create(ItemEditorManager.class);
    }
}
