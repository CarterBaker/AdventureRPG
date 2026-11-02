package editor.runtime.shipviewer;

import editor.runtime.input.EditorInputSystem;
import editor.runtime.shipviewer.camera.ShipViewerCameraSystem;
import editor.runtime.shipviewer.menu.ShipViewerMenuSystem;
import editor.runtime.shipviewer.render.ShipViewerRenderSystem;
import editor.runtime.shipviewer.view.ShipViewerSystem;
import engine.root.ContextPackage;

public class ShipViewerContext extends ContextPackage {

    /*
     * Editor tab showing the ship chosen in the hierarchy or at the
     * Shipwright as the game draws it, at rest with its sails set and its
     * anchors aboard, lit by one fixed light. A ship saved at the Shipwright
     * is registered again at once and shows here the moment it is. The orbit
     * button turns the view and scrolling zooms.
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
}
