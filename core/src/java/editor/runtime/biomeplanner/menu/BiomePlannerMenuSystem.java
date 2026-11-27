package editor.runtime.biomeplanner.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.biomeplannerpipeline.biomeplannermanager.BiomePlannerManager;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.biomegrid.BiomeGridUtility;
import editor.runtime.biomeplanner.BiomePlannerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class BiomePlannerMenuSystem extends SystemPackage {

    /*
     * Opens the Biome Planner menu. Every grid cell shows the biome it stands
     * for in its color and by name, marked as the centre or as following it,
     * and the status line names the centre and the planner's last message.
     * Cells and status are rewritten only when the planner changes.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WorldEditorManager worldEditorManager;
    private BiomePlannerManager biomePlannerManager;

    // Menus
    private MenuInstance plannerMenu;

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
        this.fboManager = get(FBOManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.biomePlannerManager = get(BiomePlannerManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.plannerMenu = menuManager.openMenu(BiomePlannerSetting.MENU_PLANNER, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (biomePlannerManager.getRevision() == shownRevision)
            return;

        this.shownRevision = biomePlannerManager.getRevision();
        refreshCells();
        refreshStatus();
    }

    // Cells \\

    private void refreshCells() {
        for (int cell = 0; cell < biomePlannerManager.getCellCount(); cell++)
            BiomeGridUtility.refreshCell(
                    plannerMenu.getEntryPoint(BiomePlannerSetting.ENTRY_FIRST_CELL + cell), cell, describeCell(cell),
                    biomePlannerManager, worldEditorManager);
    }

    private String describeCell(int cell) {

        if (cell == biomePlannerManager.getCentreCell())
            return BiomePlannerSetting.CELL_CENTRE_SUFFIX;

        return biomePlannerManager.isCellPainted(cell) ? "" : BiomePlannerSetting.CELL_FOLLOWING_SUFFIX;
    }

    // Status \\

    private void refreshStatus() {

        ElementInstance label = plannerMenu.getEntryPoint(BiomePlannerSetting.ENTRY_STATUS);

        if (label != null)
            label.setFontText(biomePlannerManager.getStatusText());
    }
}
