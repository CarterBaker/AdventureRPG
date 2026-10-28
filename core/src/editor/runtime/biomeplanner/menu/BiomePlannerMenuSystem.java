package editor.runtime.biomeplanner.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.biomeplannerpipeline.biomeplannermanager.BiomePlannerManager;
import editor.bootstrap.worldeditorpipeline.worldbiome.WorldBiomeEntryStruct;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.biomeplanner.BiomePlannerSetting;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomePlannerMenuSystem extends SystemPackage {

    /*
     * Opens the Biome Planner menu. Its catalogue lists every biome the world
     * can paint, one row per biome with its swatch and name, the selected one
     * highlighted, rebuilt when the palette moves; every grid cell shows the
     * biome it stands for in its color and by name, marked as the centre or
     * as following it; and the status line names the centre and the
     * planner's last message. Cells and status are rewritten only when the
     * planner changes.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WorldEditorManager worldEditorManager;
    private BiomePlannerManager biomePlannerManager;

    // Menus
    private MenuInstance plannerMenu;

    // Catalogue
    private ObjectArrayList<ElementInstance> catalogueElements;

    // Status
    private int shownPaletteRevision;
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Catalogue
        this.catalogueElements = new ObjectArrayList<>();

        // Status
        this.shownPaletteRevision = EngineSetting.INDEX_NOT_FOUND;
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

        if (worldEditorManager.getPaletteRevision() != shownPaletteRevision) {
            shownPaletteRevision = worldEditorManager.getPaletteRevision();
            refreshCatalogue();
        }

        if (biomePlannerManager.getRevision() != shownRevision) {
            shownRevision = biomePlannerManager.getRevision();
            refreshCells();
            refreshStatus();
        }
    }

    // Catalogue \\

    private void refreshCatalogue() {

        for (int i = 0; i < catalogueElements.size(); i++)
            menuManager.eject(plannerMenu, BiomePlannerSetting.ENTRY_CATALOGUE, catalogueElements.get(i));

        catalogueElements.clear();

        ObjectArrayList<WorldBiomeEntryStruct> palette = worldEditorManager.getPalette();

        for (int i = 0; i < palette.size(); i++)
            catalogueElements.add(injectBiome(palette.get(i)));
    }

    private ElementInstance injectBiome(WorldBiomeEntryStruct entry) {

        String template = entry.getBiomeName().equals(worldEditorManager.getSelectedBiomeName())
                ? BiomePlannerSetting.TEMPLATE_BIOME_ROW_SELECTED
                : BiomePlannerSetting.TEMPLATE_BIOME_ROW;
        MenuColorStruct swatchColor = new MenuColorStruct(PackedColorUtility.toColor(entry.getMapColor()));

        return menuManager.inject(
                plannerMenu, BiomePlannerSetting.ENTRY_CATALOGUE, template,
                element -> {
                    element.setActionArgOverride(entry.getBiomeName());

                    ElementInstance swatch = element.findChildById(BiomePlannerSetting.ELEMENT_BIOME_SWATCH);
                    ElementInstance label = element.findChildById(BiomePlannerSetting.ELEMENT_BIOME_LABEL);

                    if (swatch != null)
                        swatch.setColorOverride(swatchColor);

                    if (label != null)
                        label.setFontText(entry.getDisplayName());
                });
    }

    // Cells \\

    private void refreshCells() {
        for (int cell = 0; cell < biomePlannerManager.getCellCount(); cell++)
            refreshCell(cell, plannerMenu.getEntryPoint(BiomePlannerSetting.ENTRY_FIRST_CELL + cell));
    }

    private void refreshCell(int cell, ElementInstance element) {

        if (element == null)
            return;

        ElementInstance swatch = element.findChildById(BiomePlannerSetting.ELEMENT_CELL_SWATCH);
        ElementInstance label = element.findChildById(BiomePlannerSetting.ELEMENT_CELL_LABEL);
        int mapColor = biomePlannerManager.getCellMapColor(cell);

        if (swatch != null && mapColor != EngineSetting.INDEX_NOT_FOUND)
            swatch.setColorOverride(new MenuColorStruct(PackedColorUtility.toColor(mapColor)));

        if (label != null)
            label.setFontText(worldEditorManager.getBiomeDisplayName(biomePlannerManager.getCellBiomeName(cell))
                    + describeCell(cell));
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
