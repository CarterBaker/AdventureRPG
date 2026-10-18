package editor.arpgviewer;

import editor.arpgviewer.panel.ArpgViewerPanelSystem;
import engine.root.ContextPackage;

public class ArpgViewerContext extends ContextPackage {

    /*
     * Editor tab showing the exact decoded contents of the ARPG file selected
     * in the Hierarchy, read from disk so unsaved edits never mask what the
     * file actually holds.
     */

    // Internal
    private ArpgViewerPanelSystem arpgViewerPanelSystem;

    // Internal \\

    @Override
    protected void create() {
        this.arpgViewerPanelSystem = create(ArpgViewerPanelSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
