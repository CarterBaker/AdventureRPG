package editor.runtime.cameraplanner;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.cameraplanner.menu.CameraPlannerMenuSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import engine.root.ContextPackage;

public class CameraPlannerContext extends ContextPackage implements PropertiesProvider {

    /*
     * Editor tab for the cameras the main menu looks out from: every camera
     * placed on the active world to pick from, the picked camera's name,
     * view and exact coordinates, stepped or typed, and the buttons that set
     * the World Editor to place cameras and open a Preview tab whose main
     * menu looks out from the picked one as it is edited. It is all words;
     * the cameras stand on the World Editor's map, each drawn with its own
     * icon turned the way it looks. The design, its cameras and their live
     * edits live in the editor.
     */

    // Internal
    private CameraPlannerMenuSystem cameraPlannerMenuSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.cameraPlannerMenuSystem = create(CameraPlannerMenuSystem.class);
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
