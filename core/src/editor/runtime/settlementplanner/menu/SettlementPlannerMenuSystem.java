package editor.runtime.settlementplanner.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.settlementplannerpipeline.settlementplanner.SettlementPlannerArchitectureStruct;
import editor.bootstrap.settlementplannerpipeline.settlementplanner.SettlementPlannerEntryStruct;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.runtime.EditorSetting;
import editor.runtime.settlementplanner.SettlementPlannerSetting;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SettlementPlannerMenuSystem extends SystemPackage {

    /*
     * Opens the Settlement Planner toolbar and its two lists over the map.
     * The catalogue lists every settlement type and then every structure,
     * one row each with a swatch in its icon's color and its name, and the
     * architecture list every architecture the chosen settlement can be
     * built in, led by the biome's own choice; the chosen row of each is
     * highlighted and every row carries its index. The lists are rebuilt only
     * when the planner's rows change, and the status line, naming the design,
     * its site and what it plans to, only when the planner changes.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private SettlementPlannerManager settlementPlannerManager;

    // Menus
    private MenuInstance toolbarMenu;

    // Rows
    private ObjectArrayList<ElementInstance> entryElements;
    private ObjectArrayList<ElementInstance> architectureElements;
    private MenuColorStruct settlementSwatch;
    private MenuColorStruct structureSwatch;
    private int shownRowRevision;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.entryElements = new ObjectArrayList<>();
        this.architectureElements = new ObjectArrayList<>();
        this.settlementSwatch = toSwatchColor(EditorSetting.PLACEMENT_ICON_COLOR_SETTLEMENT);
        this.structureSwatch = toSwatchColor(EditorSetting.PLACEMENT_ICON_COLOR_STRUCTURE);
        this.shownRowRevision = EngineSetting.INDEX_NOT_FOUND;

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(SettlementPlannerSetting.MENU_TOOLBAR, window);
    }

    // Update \\

    @Override
    protected void update() {

        refreshStatus();

        if (settlementPlannerManager.getRowRevision() == shownRowRevision)
            return;

        shownRowRevision = settlementPlannerManager.getRowRevision();
        refreshEntries();
        refreshArchitectures();
    }

    // Status \\

    private void refreshStatus() {

        if (settlementPlannerManager.getRevision() == shownRevision)
            return;

        shownRevision = settlementPlannerManager.getRevision();

        ElementInstance label = toolbarMenu.getEntryPoint(SettlementPlannerSetting.ENTRY_STATUS);

        if (label != null)
            label.setFontText(settlementPlannerManager.getStatusText());
    }

    // Rows \\

    private void refreshEntries() {

        ejectRows(SettlementPlannerSetting.ENTRY_ENTRIES, entryElements);

        ObjectArrayList<SettlementPlannerEntryStruct> entries = settlementPlannerManager.getEntries();

        for (int i = 0; i < entries.size(); i++)
            entryElements.add(injectEntry(entries.get(i), i));
    }

    private void refreshArchitectures() {

        ejectRows(SettlementPlannerSetting.ENTRY_ARCHITECTURES, architectureElements);

        ObjectArrayList<SettlementPlannerArchitectureStruct> architectures = settlementPlannerManager
                .getArchitectures();

        for (int i = 0; i < architectures.size(); i++)
            architectureElements.add(injectArchitecture(architectures.get(i), i));
    }

    private void ejectRows(int entryPoint, ObjectArrayList<ElementInstance> elements) {

        for (int i = 0; i < elements.size(); i++)
            menuManager.eject(toolbarMenu, entryPoint, elements.get(i));

        elements.clear();
    }

    private ElementInstance injectEntry(SettlementPlannerEntryStruct entry, int index) {

        String template = index == settlementPlannerManager.getEntryIndex()
                ? SettlementPlannerSetting.TEMPLATE_ENTRY_ROW_SELECTED
                : SettlementPlannerSetting.TEMPLATE_ENTRY_ROW;
        MenuColorStruct swatchColor = entry.isSettlement() ? settlementSwatch : structureSwatch;

        return menuManager.inject(
                toolbarMenu, SettlementPlannerSetting.ENTRY_ENTRIES, template,
                element -> {
                    element.setActionArgOverride(Integer.toString(index));

                    ElementInstance swatch = element.findChildById(SettlementPlannerSetting.ELEMENT_ENTRY_SWATCH);
                    ElementInstance label = element.findChildById(SettlementPlannerSetting.ELEMENT_ENTRY_LABEL);

                    if (swatch != null)
                        swatch.setColorOverride(swatchColor);

                    if (label != null)
                        label.setFontText(entry.getDisplayName());
                });
    }

    private ElementInstance injectArchitecture(SettlementPlannerArchitectureStruct architecture, int index) {

        String template = index == settlementPlannerManager.getArchitectureIndex()
                ? SettlementPlannerSetting.TEMPLATE_ARCHITECTURE_ROW_SELECTED
                : SettlementPlannerSetting.TEMPLATE_ARCHITECTURE_ROW;

        return menuManager.inject(
                toolbarMenu, SettlementPlannerSetting.ENTRY_ARCHITECTURES, template,
                element -> {
                    element.setActionArgOverride(Integer.toString(index));

                    ElementInstance label = element.findChildById(
                            SettlementPlannerSetting.ELEMENT_ARCHITECTURE_LABEL);

                    if (label != null)
                        label.setFontText(architecture.getDisplayName());
                });
    }

    // Utility \\

    private MenuColorStruct toSwatchColor(float[] channels) {
        return new MenuColorStruct(new Color(channels[0], channels[1], channels[2], channels[3]));
    }
}
