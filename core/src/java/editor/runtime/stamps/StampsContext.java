package editor.runtime.stamps;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.stamps.menu.StampsMenuSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class StampsContext extends ContextPackage implements PropertiesProvider {

    /*
     * Editor tab holding the stamps, the way a creature kit's parts bin does
     * and as the Tools and Brushes tabs lay themselves out: a tile for every
     * body part with its emblem and name, sorted by kind, the one in use
     * highlighted, then the stamp's details below. Choosing one makes it the
     * stamp in use, the part the Character Stamp tab places next, and shows
     * it in the Info Panel. Stamps are content, made in the character tool
     * tabs (Save Selection As Stamp) and changed in the Hierarchy and Info
     * Panel. The shared tool keys work while this tab is focused.
     */

    // Internal
    private StampsMenuSystem stampsMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.stampsMenuSystem = create(StampsMenuSystem.class);
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
