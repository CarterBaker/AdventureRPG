package application.runtime.menueventsmanager.menus.charactercreator;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.StructPackage;

public class CreatorSessionStruct extends StructPackage {

    /*
     * One window's open character creator: the creator menu, the menu it
     * was opened from and returns to, the player being shaped, the tab on
     * show, and the name being typed with its caret state.
     */

    // Menus
    private final MenuInstance creatorMenu;
    private final MenuInstance parentMenu;

    // Character
    private final EntityInstance player;

    // Tab
    private CreatorTab activeTab;

    // Name
    private final StringBuilder nameBuffer;
    private boolean caretVisible;

    // Constructor \\

    public CreatorSessionStruct(
            MenuInstance creatorMenu,
            MenuInstance parentMenu,
            EntityInstance player,
            String defaultName) {

        // Menus
        this.creatorMenu = creatorMenu;
        this.parentMenu = parentMenu;

        // Character
        this.player = player;

        // Name
        this.nameBuffer = new StringBuilder(defaultName);
    }

    // Accessible \\

    public MenuInstance getCreatorMenu() {
        return creatorMenu;
    }

    public MenuInstance getParentMenu() {
        return parentMenu;
    }

    public WindowInstance getWindow() {
        return creatorMenu.getWindow();
    }

    public EntityInstance getPlayer() {
        return player;
    }

    public CreatorTab getActiveTab() {
        return activeTab;
    }

    public void setActiveTab(CreatorTab activeTab) {
        this.activeTab = activeTab;
    }

    public StringBuilder getNameBuffer() {
        return nameBuffer;
    }

    public boolean isCaretVisible() {
        return caretVisible;
    }

    public void setCaretVisible(boolean caretVisible) {
        this.caretVisible = caretVisible;
    }
}
