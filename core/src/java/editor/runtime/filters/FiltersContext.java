package editor.runtime.filters;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.filters.panel.FiltersPanelSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import engine.root.ContextPackage;

public class FiltersContext extends ContextPackage implements PropertiesProvider {

    /*
     * Editor tab for the filters, beside the transitions and listed as they
     * are: every look photo mode offers by name, the one picked marked with
     * what it does to the picture. A filter is picked up from its row and let
     * go over a clip on a Recorder tab's timeline to lay it over everything
     * the clip shows, or laid on the selected clip by the Tools tab's Apply
     * Filter. Filters are content, made and changed in the Hierarchy and Info
     * Panel. All state lives in RecorderManager; this tab only shows it and
     * hands drags to RecorderBranch.
     */

    // Internal
    private FiltersPanelSystem filtersPanelSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.filtersPanelSystem = create(FiltersPanelSystem.class);
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
