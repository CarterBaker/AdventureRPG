package editor.runtime.shipviewer.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.runtime.shipviewer.ShipViewerSetting;
import editor.runtime.shipviewer.view.ShipViewerSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class ShipViewerMenuSystem extends SystemPackage {

    /*
     * Opens the Ship Viewer's toolbar and status line over the viewport. The
     * toolbar steps to the previous or next vehicle; the status line names
     * the one shown, its place among them all and how many parts it is
     * built of, rewritten only when what is shown is chosen again.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ShipViewerSystem shipViewerSystem;

    // Menus
    private MenuInstance viewerMenu;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.shipViewerSystem = get(ShipViewerSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.viewerMenu = menuManager.openMenu(ShipViewerSetting.MENU_VIEWER, window);
    }

    // Update \\

    @Override
    protected void update() {

        int revision = shipViewerSystem.getRevision();

        if (revision == shownRevision)
            return;

        shownRevision = revision;

        ElementInstance label = viewerMenu.getEntryPoint(ShipViewerSetting.ENTRY_STATUS);

        if (label != null)
            label.setFontText(ShipViewerSetting.STATUS_PREFIX + describeShip());
    }

    // The ship's name, its place among every vehicle and its part count
    private String describeShip() {

        VehicleHandle vehicleHandle = shipViewerSystem.getVehicleHandle();

        if (vehicleHandle == null)
            return ShipViewerSetting.STATUS_NONE;

        return vehicleHandle.getDisplayName()
                + ShipViewerSetting.STATUS_SEPARATOR
                + (shipViewerSystem.getVehicleIndex() + 1)
                + ShipViewerSetting.STATUS_OF
                + shipViewerSystem.getVehicleCount()
                + ShipViewerSetting.STATUS_SEPARATOR
                + vehicleHandle.getPartCount()
                + ShipViewerSetting.STATUS_PARTS
                + ShipViewerSetting.STATUS_SEPARATOR
                + ShipViewerSetting.STATUS_FOLLOWING;
    }
}
