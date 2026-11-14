package editor.bootstrap.toolpipeline;

import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import engine.root.PipelinePackage;

public class ToolPipeline extends PipelinePackage {

    /*
     * Registers the editor's one set of tools, shared by every window that
     * takes them, so a tool is chosen, sized and keyed the same way whichever
     * editor it works in.
     */

    // Internal \\

    @Override
    protected void create() {
        create(ToolManager.class);
    }
}
