package editor.runtime.brushes;

import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.runtime.brushes.menu.BrushesMenuSystem;
import engine.root.ContextPackage;

public class BrushesContext extends ContextPackage {

    /*
     * Editor tab holding the brush presets, the way a paint program's preset
     * docker does: every preset with its shape and mode, the one in use
     * highlighted, and below it that preset's shape, mode and numbers to
     * change at a click, with the brush size of the canvas the tools target.
     * Every change here is an edit of the preset, left unsaved in the Info
     * Panel until it is saved, and Edit opens it there. The shared tool keys
     * work while this tab is focused; the presets live in the editor.
     */

    // Internal
    private ToolManager toolManager;
    private BrushesMenuSystem brushesMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.brushesMenuSystem = create(BrushesMenuSystem.class);
    }

    @Override
    protected void get() {
        this.toolManager = get(ToolManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
        toolManager.openToolWindow(getWindow());
    }

    @Override
    protected void dispose() {
        toolManager.closeToolWindow(getWindow());
    }
}
