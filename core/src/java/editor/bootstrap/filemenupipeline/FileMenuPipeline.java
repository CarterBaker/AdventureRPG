package editor.bootstrap.filemenupipeline;

import editor.bootstrap.filemenupipeline.filemenumanager.FileMenuManager;
import engine.root.PipelinePackage;

public class FileMenuPipeline extends PipelinePackage {

    /*
     * Registers the target of the editor's File and Edit menus, shared by
     * every tool, so New, Save, Revert, Undo and the rest always act on the
     * window worked in last.
     */

    // Internal \\

    @Override
    protected void create() {
        create(FileMenuManager.class);
    }
}
