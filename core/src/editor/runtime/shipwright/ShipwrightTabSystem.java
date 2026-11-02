package editor.runtime.shipwright;

import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightStorage;
import application.bootstrap.shipwrightpipeline.shipwrightmanager.ShipwrightManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.shipwright.ShipwrightDialogSystem;
import application.runtime.shipwright.ShipwrightSystem;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.shipviewerpipeline.shipviewermanager.ShipViewerManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgObjectStruct;

public class ShipwrightTabSystem extends SystemPackage {

    /*
     * Sets the editor's drafting table up in its tab: the tab's menus are
     * drawn into a target of their own, and the table opens at once writing
     * ship files into the game's assets — each through InfoManager, so a ship
     * is checked and recorded as every content file is, and the Info Panel
     * shows what was written. Closing the tab over unsaved work asks first
     * through the table's dialog, and closing the table closes the tab. Each
     * ship opened or saved at the table is chosen for the Ship Viewer.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ShipwrightManager shipwrightManager;
    private InfoManager infoManager;
    private ShipwrightSystem shipwrightSystem;
    private ShipwrightDialogSystem shipwrightDialogSystem;
    private TabManager tabManager;
    private ShipViewerManager shipViewerManager;

    // Ship Viewer — the saved name of the ship last chosen from the table
    private String chosenName;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.shipwrightManager = get(ShipwrightManager.class);
        this.infoManager = get(InfoManager.class);
        this.shipwrightSystem = get(ShipwrightSystem.class);
        this.shipwrightDialogSystem = get(ShipwrightDialogSystem.class);
        this.tabManager = get(TabManager.class);
        this.shipViewerManager = get(ShipViewerManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        shipwrightManager.setAssetWriter(this::writeShip);
        shipwrightDialogSystem.setCloseAction(() -> tabManager.closeTabOf(window));
        tabManager.setCloseGuard(window, shipwrightDialogSystem::allowClose);
        shipwrightSystem.open(ShipwrightStorage.ASSETS);
    }

    // Update \\

    // The ship on the table chosen for the Ship Viewer once it is opened
    @Override
    protected void update() {

        ShipwrightSessionInstance session = shipwrightSystem.getSession();

        if (!shipwrightSystem.isOpen() || !session.isSaved() || session.getSavedName().equals(chosenName))
            return;

        chosenName = session.getSavedName();
        chooseShip(chosenName);
    }

    // Write \\

    // A ship written into the assets through InfoManager, chosen for the Ship Viewer once written
    private String writeShip(String shipName, ArpgObjectStruct shipArpg) {

        if (!infoManager.writeDocument(EditorSetting.INFO_SCHEMA_VEHICLES, shipName, shipArpg))
            return infoManager.getRefusal();

        chooseShip(shipName);

        return EngineSetting.SHIPWRIGHT_STATUS_NONE;
    }

    private void chooseShip(String shipName) {
        shipViewerManager.choose(shipwrightManager.toVehicleName(ShipwrightStorage.ASSETS, shipName));
    }
}
