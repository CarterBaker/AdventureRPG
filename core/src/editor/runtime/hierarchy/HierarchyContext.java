package editor.runtime.hierarchy;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.hierarchy.panel.HierarchyPanelSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import engine.root.ContextPackage;

public class HierarchyContext extends ContextPackage implements PropertiesProvider {

    /*
     * Editor tab showing a hierarchy panel. The engine's HierarchyManager lays
     * out and drives every tab; this context only hosts the panel.
     */

    // Internal
    private HierarchyPanelSystem hierarchyPanelSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.hierarchyPanelSystem = create(HierarchyPanelSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Properties \\

    // The entry selected here, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }
}
