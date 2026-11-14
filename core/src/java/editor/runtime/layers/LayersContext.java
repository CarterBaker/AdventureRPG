package editor.runtime.layers;

import editor.runtime.layers.panel.LayersPanelSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class LayersContext extends ContextPackage {

    /*
     * The editor's one Layers tab, shared by every window that works on
     * layers, as a painting tool's layers docker is: it lists the layer
     * stack of the window worked in last, top first, groups folding what
     * they hold. Picking a layer makes it the one that window's tools work
     * on, so one window holds every task a stack of layers splits it into;
     * its eye shows or hides it there. The shared tool keys work while this
     * tab is focused.
     */

    // Internal
    private LayersPanelSystem layersPanelSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.layersPanelSystem = create(LayersPanelSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
