package editor.runtime.brushes;

import editor.runtime.brushes.menu.BrushesMenuSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class BrushesContext extends ContextPackage {

    /*
     * Editor tab holding the brushes, the way a paint program's brush docker
     * does: every brush tip by name and shape, the one in use highlighted.
     * Choosing one here makes it the tip every stroking tool stamps with;
     * what a stroke does and how strongly is the tool's, set in the Tools
     * tab. Brushes are content, made and changed in the Hierarchy and Info
     * Panel. The shared tool keys work while this tab is focused.
     */

    // Internal
    private BrushesMenuSystem brushesMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.brushesMenuSystem = create(BrushesMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
