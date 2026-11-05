package editor.runtime.shipviewer;

import editor.runtime.input.EditorInputSystem;
import editor.runtime.shipviewer.camera.ShipViewerCameraSystem;
import editor.runtime.shipviewer.menu.ShipViewerMenuSystem;
import editor.runtime.shipviewer.render.ShipViewerRenderSystem;
import editor.runtime.shipviewer.view.ShipViewerSystem;
import engine.root.ContextPackage;

public class ShipViewerContext extends ContextPackage {

    /*
     * Editor tab showing the ship chosen in the hierarchy, or the one on the
     * Shipwright's table as it is drawn, as the game draws it, at rest with
     * its sails set, its anchors aboard and its furniture in place, lit by one
     * fixed light. The orbit button turns the view, the pan button slides it
     * with the pointer and scrolling zooms; clicking an axis of the gizmo
     * looks back at the ship along it, centred, and its middle centres the
     * view; the toolbar's minus and plus step the cutaway down through the
     * decks and back up to them all.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ShipViewerSystem shipViewerSystem;
    private ShipViewerCameraSystem shipViewerCameraSystem;
    private ShipViewerRenderSystem shipViewerRenderSystem;
    private ShipViewerMenuSystem shipViewerMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.shipViewerSystem = create(ShipViewerSystem.class);
        this.shipViewerCameraSystem = create(ShipViewerCameraSystem.class);
        this.shipViewerRenderSystem = create(ShipViewerRenderSystem.class);
        this.shipViewerMenuSystem = create(ShipViewerMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Accessible \\

    // Looks back at the ship along an axis named as in VIEW_AXES
    public void view(String axisName) {

        for (int i = 0; i < ShipViewerSetting.VIEW_AXES.length; i++)
            if (ShipViewerSetting.VIEW_AXES[i].equals(axisName))
                shipViewerCameraSystem.snap(i);
    }

    public void centre() {
        shipViewerCameraSystem.centre();
    }

    // Steps the cutaway a deck down or up
    public void stepDeck(int step) {
        shipViewerSystem.stepDeck(step);
    }
}
