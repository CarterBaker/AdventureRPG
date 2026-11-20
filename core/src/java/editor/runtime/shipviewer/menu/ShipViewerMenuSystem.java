package editor.runtime.shipviewer.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.runtime.shipviewer.ShipViewerSetting;
import editor.runtime.shipviewer.view.ShipViewerSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class ShipViewerMenuSystem extends SystemPackage {

    /*
     * Opens the Ship Viewer's menu over the viewport: the status line names
     * the ship shown, whether it is the one on the Shipwright's table, and
     * how many parts it is built of, or why none is shown; the deck label
     * says which decks the cutaway leaves; both are rewritten only when what
     * is shown changes. The axis gizmo is every viewport's own
     * (ViewGizmoSystem).
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

        // Status
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

        setLabel(ShipViewerSetting.ENTRY_STATUS, ShipViewerSetting.STATUS_PREFIX + describeShip());
        setLabel(ShipViewerSetting.ENTRY_DECK, describeDeck());
    }

    // The ship's name, whether it is the one on the table, and its part count, or why no ship is shown
    private String describeShip() {

        String vehicleName = shipViewerSystem.getVehicleName();

        if (shipViewerSystem.isShown())
            return shipViewerSystem.getDisplayName()
                    + (shipViewerSystem.isDraft()
                            ? ShipViewerSetting.STATUS_SEPARATOR + ShipViewerSetting.STATUS_DRAFT
                            : "")
                    + ShipViewerSetting.STATUS_SEPARATOR
                    + shipViewerSystem.getPartCount()
                    + ShipViewerSetting.STATUS_PARTS;

        return vehicleName == null
                ? ShipViewerSetting.STATUS_NONE
                : vehicleName + ShipViewerSetting.STATUS_UNREGISTERED;
    }

    // Every deck, or the deck the cutaway leaves with those under it
    private String describeDeck() {

        if (shipViewerSystem.getDeck() == ShipViewerSetting.DECK_ALL)
            return ShipViewerSetting.DECK_TEXT_ALL;

        return String.format(ShipViewerSetting.DECK_TEXT_FORMAT,
                shipViewerSystem.getDeck() + 1, shipViewerSystem.getDeckCount());
    }

    // Utility \\

    private void setLabel(int entryPoint, String value) {

        ElementInstance label = viewerMenu.getEntryPoint(entryPoint);

        if (label != null)
            label.setFontText(value);
    }
}
