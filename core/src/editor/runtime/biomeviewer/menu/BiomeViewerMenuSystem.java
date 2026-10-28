package editor.runtime.biomeviewer.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import editor.bootstrap.biomeplannerpipeline.biomeplannermanager.BiomePlannerManager;
import editor.runtime.biomeviewer.BiomeViewerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class BiomeViewerMenuSystem extends SystemPackage {

    /*
     * Opens the Biome Viewer's status line over the world, naming the biome
     * at the centre of the planner's grid and the planner's last message,
     * rewritten only when the planner changes.
     */

    // Internal
    private MenuManager menuManager;
    private BiomePlannerManager biomePlannerManager;

    // Menus
    private MenuInstance statusMenu;

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
        this.biomePlannerManager = get(BiomePlannerManager.class);
    }

    @Override
    protected void awake() {
        this.statusMenu = menuManager.openMenu(BiomeViewerSetting.MENU_STATUS, context.getWindow());
    }

    // Update \\

    @Override
    protected void update() {

        if (biomePlannerManager.getRevision() == shownRevision)
            return;

        shownRevision = biomePlannerManager.getRevision();

        ElementInstance label = statusMenu.getEntryPoint(BiomeViewerSetting.ENTRY_STATUS);

        if (label != null)
            label.setFontText(BiomeViewerSetting.STATUS_PREFIX + biomePlannerManager.getStatusText());
    }
}
