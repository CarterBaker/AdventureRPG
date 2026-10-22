package editor.runtime.shipwright;

import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightStorage;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.shipwright.ShipwrightSystem;
import engine.root.SystemPackage;

public class ShipwrightTabSystem extends SystemPackage {

    /*
     * Sets the editor's drafting table up in its tab: the tab's menus are
     * drawn into a target of their own, and the table opens at once writing
     * ship files into the game's assets.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ShipwrightSystem shipwrightSystem;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.shipwrightSystem = get(ShipwrightSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        shipwrightSystem.open(ShipwrightStorage.ASSETS);
    }
}
