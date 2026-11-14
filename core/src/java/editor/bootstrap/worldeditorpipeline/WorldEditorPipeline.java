package editor.bootstrap.worldeditorpipeline;

import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import engine.root.PipelinePackage;

public class WorldEditorPipeline extends PipelinePackage {

    /*
     * Registers the world editor's shared state, so every World Editor tab
     * paints the same world image with the same brush, and biome edits go
     * live for every map and window at once.
     */

    // Internal \\

    @Override
    protected void create() {
        create(WorldEditorManager.class);
    }
}
