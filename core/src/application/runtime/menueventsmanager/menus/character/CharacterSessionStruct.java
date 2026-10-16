package application.runtime.menueventsmanager.menus.character;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class CharacterSessionStruct extends StructPackage {

    /*
     * One window's open character sheet: the sheet menu, the player it shows,
     * the tab on show, whether the next Confirm spends the staged points for
     * good, and which state of the character and how far through its level the
     * sheet shows, so it redraws only when either moves. The preview holds
     * every stat as it would stand with the staged points spent.
     */

    // Menu
    private final MenuInstance sheetMenu;

    // Character
    private final EntityInstance player;

    // Tab
    private CharacterTab activeTab;

    // Confirm
    private boolean confirmArmed;

    // Shown
    private int shownRevision;
    private double shownProgress;

    // Preview
    private final float[] preview;

    // Constructor \\

    public CharacterSessionStruct(MenuInstance sheetMenu, EntityInstance player, int statCount) {

        // Menu
        this.sheetMenu = sheetMenu;

        // Character
        this.player = player;

        // Tab
        this.activeTab = CharacterTab.VALUES[0];

        // Shown
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownProgress = EngineSetting.INDEX_NOT_FOUND;

        // Preview
        this.preview = new float[statCount];
    }

    // Accessible \\

    public MenuInstance getSheetMenu() {
        return sheetMenu;
    }

    public WindowInstance getWindow() {
        return sheetMenu.getWindow();
    }

    public EntityInstance getPlayer() {
        return player;
    }

    public CharacterTab getActiveTab() {
        return activeTab;
    }

    public void setActiveTab(CharacterTab activeTab) {
        this.activeTab = activeTab;
    }

    public boolean isConfirmArmed() {
        return confirmArmed;
    }

    public void setConfirmArmed(boolean confirmArmed) {
        this.confirmArmed = confirmArmed;
    }

    public int getShownRevision() {
        return shownRevision;
    }

    public void setShownRevision(int shownRevision) {
        this.shownRevision = shownRevision;
    }

    public double getShownProgress() {
        return shownProgress;
    }

    public void setShownProgress(double shownProgress) {
        this.shownProgress = shownProgress;
    }

    // Forces the next update to redraw the whole sheet
    public void invalidate() {
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    public float[] getPreview() {
        return preview;
    }
}
