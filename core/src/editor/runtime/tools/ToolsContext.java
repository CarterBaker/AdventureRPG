package editor.runtime.tools;

import editor.runtime.tools.menu.ToolsMenuSystem;
import engine.root.ContextPackage;

public class ToolsContext extends ContextPackage {

    /*
     * Editor tab holding the tools other tabs work with, the way a toolbox
     * does: every World Editor tool listed under its mode, Paint or
     * Settlements, the one in use highlighted, and the brush size. Choosing a
     * tool here chooses it, and its mode, for every World Editor at once; the
     * tools themselves live in the editor.
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
