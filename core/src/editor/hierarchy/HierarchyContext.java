package editor.hierarchy;

import editor.hierarchy.panel.HierarchyPanelSystem;
import engine.root.ContextPackage;

public class HierarchyContext extends ContextPackage {

    /*
     * Editor tab showing a hierarchy panel. The engine's HierarchyManager lays
     * out and drives every tab; this context only hosts the panel.
     */

    // Internal
    private HierarchyPanelSystem hierarchyPanelSystem;

    // Internal \\

    @Override
    protected void create() {
        this.hierarchyPanelSystem = create(HierarchyPanelSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
