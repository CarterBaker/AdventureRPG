package editor.runtime.shipwright;

import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightStorage;
import application.bootstrap.shipwrightpipeline.shipwrightmanager.ShipwrightManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.shipwright.ShipwrightSystem;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class ShipwrightTabSystem extends SystemPackage {

    /*
     * Sets the editor's drafting table up in its tab: the tab's menus are
     * drawn into a target of their own, and the table opens at once writing
     * ship files into the game's assets — each through InfoManager, so a ship
     * is checked and recorded as every content file is, and the Info Panel
     * shows what was written.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ShipwrightManager shipwrightManager;
    private InfoManager infoManager;
    private ShipwrightSystem shipwrightSystem;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.shipwrightManager = get(ShipwrightManager.class);
        this.infoManager = get(InfoManager.class);
        this.shipwrightSystem = get(ShipwrightSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        shipwrightManager.setAssetWriter((shipName, shipArpg) -> infoManager.writeDocument(
                EditorSetting.INFO_SCHEMA_VEHICLES, shipName, shipArpg)
                        ? EngineSetting.SHIPWRIGHT_STATUS_NONE
                        : infoManager.getRefusal());
        shipwrightSystem.open(ShipwrightStorage.ASSETS);
    }
}
