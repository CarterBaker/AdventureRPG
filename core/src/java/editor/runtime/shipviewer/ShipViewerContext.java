package editor.runtime.shipviewer;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.shipviewer.menu.ShipViewerMenuSystem;
import editor.runtime.shipviewer.render.ShipViewerRenderSystem;
import editor.runtime.shipviewer.view.ShipViewerSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import editor.runtime.viewcamera.ViewGizmoSystem;
import engine.root.ContextPackage;

public class ShipViewerContext extends ContextPackage implements ViewCameraProvider, PropertiesProvider {

    /*
     * Editor tab showing the ship chosen in the hierarchy, or the one on the
     * Shipwright's table as it is drawn, as the game draws it, at rest with
     * its sails set, its anchors aboard and its furniture in place, lit by one
     * fixed light. The orbit button turns the view, the pan button slides it
     * with the pointer and scrolling zooms; the axis gizmo in its corner
     * (ViewGizmoSystem) looks back at the ship along an axis clicked and its
     * middle centres the view; the toolbar's minus and plus step the cutaway
     * down through the decks and back up to them all.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ShipViewerSystem shipViewerSystem;
    private ShipViewerRenderSystem shipViewerRenderSystem;
    private ShipViewerMenuSystem shipViewerMenuSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.shipViewerSystem = create(ShipViewerSystem.class);
        create(ViewCameraSystem.class);
        this.shipViewerRenderSystem = create(ShipViewerRenderSystem.class);
        this.shipViewerMenuSystem = create(ShipViewerMenuSystem.class);
        create(ViewGizmoSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return shipViewerSystem;
    }

    // Accessible \\

    // Steps the cutaway a deck down or up
    public void stepDeck(int step) {
        shipViewerSystem.stepDeck(step);
    }

    // Properties \\

    // What this tool works on is content selected in the Hierarchy, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }
}
