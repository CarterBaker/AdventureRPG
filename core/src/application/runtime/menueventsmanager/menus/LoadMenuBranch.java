package application.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.savepipeline.savemanager.SaveManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LoadMenuBranch extends BranchPackage {

    /*
     * Handles the load menu opened from the main menu. Opening hides the menu
     * it was opened from and lists every saved character, most recently played
     * first; Back closes it and shows that menu again. Choosing a character
     * loads it into the window's player through SaveManager and closes both
     * menus so play begins — a character that fails to load leaves the menu
     * open. The hidden menu is tracked per window so every window's load menu
     * returns to its own.
     */

    // Internal
    private MenuManager menuManager;
    private SaveManager saveManager;

    // Per-window
    private Int2ObjectOpenHashMap<MenuInstance> windowID2ParentMenu;

    // Base \\

    @Override
    protected void create() {
        this.windowID2ParentMenu = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.saveManager = get(SaveManager.class);
    }

    // Open / Close \\

    public void openMenu(MenuInstance parentMenu, WindowInstance window) {

        int windowID = window.getWindowID();

        if (windowID2ParentMenu.containsKey(windowID))
            return;

        parentMenu.hide();
        windowID2ParentMenu.put(windowID, parentMenu);

        populateCharacterList(menuManager.openMenu(RuntimeSetting.MENU_LOAD, window));
    }

    public void closeMenu(MenuInstance loadMenu, WindowInstance window) {
        menuManager.closeMenu(loadMenu);
        windowID2ParentMenu.remove(window.getWindowID()).show();
    }

    // Load \\

    public void loadCharacter(String characterName, MenuInstance loadMenu, WindowInstance window) {

        if (!saveManager.loadCharacter(window, characterName))
            return;

        menuManager.closeMenu(loadMenu);
        menuManager.closeMenu(windowID2ParentMenu.remove(window.getWindowID()));
    }

    // Character List \\

    private void populateCharacterList(MenuInstance loadMenu) {

        ObjectArrayList<String> characterNames = saveManager.getCharacterNames();

        for (int i = 0; i < characterNames.size(); i++)
            injectCharacterSlot(loadMenu, characterNames.get(i));
    }

    private void injectCharacterSlot(MenuInstance loadMenu, String characterName) {
        menuManager.inject(
                loadMenu, RuntimeSetting.ENTRY_CHARACTER_LIST, RuntimeSetting.MENU_LOAD_CHARACTER_SLOT,
                slot -> {
                    slot.setActionArgOverride(characterName);
                    ElementInstance label = slot.findChildById(RuntimeSetting.ELEMENT_CHARACTER_SLOT_LABEL);
                    if (label != null)
                        label.setFontText(characterName);
                });
    }
}