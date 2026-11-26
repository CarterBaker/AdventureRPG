package editor.bootstrap.treeimpostorpipeline;

import editor.bootstrap.treeimpostorpipeline.treeimpostormanager.TreeImpostorManager;
import engine.root.PipelinePackage;

public class TreeImpostorPipeline extends PipelinePackage {

    /*
     * Registers the editor's impostor bake, which keeps every tree species'
     * baked impostor current with its file: a few views a frame for the
     * species whose impostor is missing or stale at start, and at once for a
     * species the moment its file is saved.
     */

    // Internal \\

    @Override
    protected void create() {
        create(TreeImpostorManager.class);
    }
}
