package editor.runtime.brushes;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.brushes.menu.BrushesMenuSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class BrushesContext extends ContextPackage implements PropertiesProvider {

    /*
     * Editor tab holding the brushes, the way a paint program's brush docker
     * does: every brush tip with a thumbnail of the stroke it makes, its name
     * and shape, the one in use highlighted. Choosing one here makes it the
     * tip every stroking tool stamps with, a soft one fading its edge and a
     * hard one keeping it, and shows it in the Info Panel; what a stroke does
     * and how strongly is the tool's, set in the Tools tab. Brushes are
     * content, made and changed in the Hierarchy and Info Panel. The shared
     * tool keys work while this tab is focused.
     */

    // Internal
    private BrushesMenuSystem brushesMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.brushesMenuSystem = create(BrushesMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Properties \\

    // What this tool works on is content selected in the Hierarchy, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }
}
