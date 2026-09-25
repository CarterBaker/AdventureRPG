package editor.infopanel;

import editor.infopanel.panel.InfoPanelSystem;
import engine.root.ContextPackage;

public class InfoPanelContext extends ContextPackage {

    /*
     * Editor tab showing the JSON entry selected in the Hierarchy. The editor's
     * InfoManager holds the content and performs every edit; this context only
     * hosts the panel that lists and routes them.
     */

    // Internal
    private InfoPanelSystem infoPanelSystem;

    // Internal \\

    @Override
    protected void create() {
        this.infoPanelSystem = create(InfoPanelSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
