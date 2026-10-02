package application.runtime.menueventsmanager.menus;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.menueventsmanager.menus.inventory.InventoryBranch;
import engine.input.InputNameUtility;
import engine.root.BranchPackage;
import engine.settings.KeyBindings;

public class HUDBranch extends BranchPackage {

    /*
     * Runs the play HUD for this context's window: a small cursor at the
     * centre of the screen for as long as the player is in play, and under it
     * the activate key cap while the player faces something the activate
     * binding acts on — a container it can open where it lies, as
     * InventoryBranch decides, or a vehicle's helm, sails or anchor, or while
     * it holds a helm it can let go. The key cap carries the binding's current
     * name, so a rebound key shows at once. Neither menu ever takes input.
     */

    // Internal
    private MenuManager menuManager;
    private PlayerManager playerManager;
    private InventoryBranch inventoryBranch;

    // State
    private MenuInstance crosshairMenu;
    private MenuInstance activateMenu;
    private String keyName;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.playerManager = get(PlayerManager.class);
        this.inventoryBranch = get(InventoryBranch.class);
    }

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        boolean playing = playerManager.isPlayingForWindow(window.getWindowID());
        boolean activatable = playing && (inventoryBranch.findOpenableContainer(window)
                || playerManager.isFacingVehicleControlForWindow(window.getWindowID()));

        this.crosshairMenu = showMenu(crosshairMenu, RuntimeSetting.MENU_HUD_CROSSHAIR, playing, window);
        this.activateMenu = showMenu(activateMenu, RuntimeSetting.MENU_HUD_ACTIVATE, activatable, window);

        if (activateMenu == null) {
            this.keyName = null;
            return;
        }

        refreshKey();
    }

    // Open / Close \\

    // Opens or closes a HUD menu to match whether it should show, returning the menu left open
    private MenuInstance showMenu(MenuInstance menu, String menuName, boolean shown, WindowInstance window) {

        if (!shown)
            return menuManager.closeMenu(menu);

        return menu != null ? menu : menuManager.openMenu(menuName, window);
    }

    // Key \\

    // The key cap follows the activate binding, rewritten only when it is newly shown or rebound
    private void refreshKey() {

        String name = InputNameUtility.getName(KeyBindings.ACTIVATE);

        if (name.equals(keyName))
            return;

        this.keyName = name;
        activateMenu.getEntryPoint(RuntimeSetting.ENTRY_HUD_ACTIVATE_KEY).setFontText(name);
    }
}
