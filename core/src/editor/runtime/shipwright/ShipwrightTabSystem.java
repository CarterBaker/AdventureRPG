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
     * through the table's dialog, and closing the table closes the tab. The
     * table is chosen for the Ship Viewer once it opens and again whenever a
     * ship is written from it, so the viewer shows the ship as it is drawn.
     * Closing the tab takes back its asset writer and the session it chose.
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

    // Writer
    private ShipwrightManager.ShipwrightAssetWriter assetWriter;

    // Ship Viewer — the choice's revision when the table was last chosen, INDEX_NOT_FOUND before it ever was
    private int chosenRevision;
    private ShipwrightSessionInstance chosenSession;

    // Base \\

    @Override
    protected void create() {
        this.assetWriter = this::writeShip;
        this.chosenRevision = EngineSetting.INDEX_NOT_FOUND;
    }

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
        shipwrightManager.setAssetWriter(assetWriter);
        shipwrightDialogSystem.setCloseAction(() -> tabManager.closeTabOf(window));
        tabManager.setCloseGuard(window, shipwrightDialogSystem::allowClose);
        shipwrightSystem.open(ShipwrightStorage.ASSETS);
    }

    @Override
    protected void dispose() {
        shipwrightManager.clearAssetWriter(assetWriter);
        shipViewerManager.releaseSession(chosenSession);
    }

    // Update \\

    // The table chosen for the Ship Viewer once it is open
    @Override
    protected void update() {

        if (shipwrightSystem.isOpen() && shipViewerManager.getSession() == null && !isChosenElsewhere())
            chooseTable();
    }

    // True when a vehicle has been chosen in the hierarchy since the table was last chosen
    private boolean isChosenElsewhere() {
        return chosenRevision != EngineSetting.INDEX_NOT_FOUND && shipViewerManager.getRevision() != chosenRevision;
    }

    // Write \\

    // A ship written into the assets through InfoManager, shown in the Info Panel and chosen for the Ship Viewer
    // once written
    private String writeShip(String shipName, ArpgObjectStruct shipArpg) {

        if (!infoManager.writeDocument(EditorSetting.INFO_SCHEMA_VEHICLES, shipName, shipArpg))
            return infoManager.getRefusal();

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_VEHICLES, shipName, shipName);
        chooseTable();

        return EngineSetting.SHIPWRIGHT_STATUS_NONE;
    }

    private void chooseTable() {
        this.chosenSession = shipwrightSystem.getSession();
        shipViewerManager.chooseSession(chosenSession);
        this.chosenRevision = shipViewerManager.getRevision();
    }
}
