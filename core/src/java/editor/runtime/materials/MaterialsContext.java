package editor.runtime.materials;

import editor.runtime.materials.panel.MaterialsPanelSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class MaterialsContext extends ContextPackage {

    /*
     * The Materials tab, the editor's one list of materials: every texture of
     * every baked pack, pack by pack, the one chosen marked and described. A
     * material pressed is chosen, the one the Hair tab's new strands grow in
     * and its Brush paints; carried out of the tab and let go over an Item
     * Editor, it colours the part under the pointer. All state lives in
     * MaterialsManager; this tab only shows it and hands presses and drags
     * to MaterialsBranch. The shared tool keys work while it is focused.
     */

    // Internal
    private MaterialsPanelSystem materialsPanelSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.materialsPanelSystem = create(MaterialsPanelSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
