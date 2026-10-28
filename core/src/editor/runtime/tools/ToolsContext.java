package editor.runtime.tools;

import editor.runtime.tools.menu.ToolsMenuSystem;
import engine.root.ContextPackage;

public class ToolsContext extends ContextPackage {

    /*
     * Editor tab holding the tools and their options, the way a paint
     * program's toolbox and tool options do: the tools of whichever window
     * was worked in last, under the name of what that window works on, the
     * one in use highlighted, and below them that tool's options — its size,
     * strength and whatever else it takes — with the brush it strokes with.
     * Choosing a tool or changing an option here does so for that window's
     * canvas, and the shared tool keys are this tab's shortcuts; the tools
     * themselves live in the editor.
     */

    // Internal
    private ToolsMenuSystem toolsMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.toolsMenuSystem = create(ToolsMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
