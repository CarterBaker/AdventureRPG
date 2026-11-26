package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.layerspipeline.layersmanager.LayersManager;
import editor.bootstrap.propertiespipeline.propertiesmanager.PropertiesManager;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.bootstrap.tabpipeline.layoutmanager.LayoutManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the editor toolbar. Each dropdown's operations
     * open their tool's tab in the clicked OS window; the Tools menu, as it
     * opens, names after each shared tab the window it would serve now, the
     * way the File menu names the tool it acts on; layout operations list
     * saved layouts and create, load, save over, and delete them, listing
     * them again the moment one is created or deleted so the dropdown never
     * shows a layout that is not on disk. Saving over a layout asks first,
     * its name already filled in, so a stray click never replaces it.
     */

    // Internal
    private MenuManager menuManager;
    private TabManager tabManager;
    private LayoutManager layoutManager;
    private NameDialogBranch nameDialogBranch;
    private ActiveWindowManager activeWindowManager;
    private PropertiesManager propertiesManager;
    private LayersManager layersManager;
    private ToolManager toolManager;

    // Layouts
    private MenuInstance layoutMenu;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.tabManager = get(TabManager.class);
        this.layoutManager = get(LayoutManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.activeWindowManager = get(ActiveWindowManager.class);
        this.propertiesManager = get(PropertiesManager.class);
        this.layersManager = get(LayersManager.class);
        this.toolManager = get(ToolManager.class);
    }

    // Tools Dropdown \\

    // Every shared tab's entry named after the window it would serve: the one its own manager follows
    public void refreshToolsMenu(MenuInstance menu) {

        ElementInstance list = menu.getEntryPoint(EditorSetting.ENTRY_TOOLS_MENU_LIST);

        if (list == null)
            return;

        WindowInstance canvasWindow = toolManager.getCanvasWindow();

        nameEntry(list, EditorSetting.ELEMENT_TOOLS_MENU_PROPERTIES, EditorSetting.TAB_TITLE_PROPERTIES,
                propertiesManager.getProviderWindow());
        nameEntry(list, EditorSetting.ELEMENT_TOOLS_MENU_LAYERS, EditorSetting.TAB_TITLE_LAYERS,
                layersManager.getProviderWindow());
        nameEntry(list, EditorSetting.ELEMENT_TOOLS_MENU_TOOLS, EditorSetting.TAB_TITLE_TOOLS, canvasWindow);
        nameEntry(list, EditorSetting.ELEMENT_TOOLS_MENU_BRUSHES, EditorSetting.TAB_TITLE_BRUSHES, canvasWindow);
        nameEntry(list, EditorSetting.ELEMENT_TOOLS_MENU_STAMPS, EditorSetting.TAB_TITLE_STAMPS, canvasWindow);
        nameEntry(list, EditorSetting.ELEMENT_TOOLS_MENU_PALETTE, EditorSetting.TAB_TITLE_PALETTE,
                propertiesManager.getPaletteWindow());
    }

    private void nameEntry(ElementInstance list, String labelId, String title, WindowInstance window) {

        ElementInstance label = list.findChildById(labelId);

        if (label != null)
            label.setFontText(activeWindowManager.nameContext(title, window));
    }

    public void openHierarchy(WindowInstance window) {
        tabManager.openHierarchy(window.getGLWindow());
    }

    public void openProperties(WindowInstance window) {
        tabManager.openProperties(window.getGLWindow());
    }

    public void openLayers(WindowInstance window) {
        tabManager.openLayers(window.getGLWindow());
    }

    public void openArpgViewer(WindowInstance window) {
        tabManager.openArpgViewer(window.getGLWindow());
    }

    public void openTools(WindowInstance window) {
        tabManager.openTools(window.getGLWindow());
    }

    public void openBrushes(WindowInstance window) {
        tabManager.openBrushes(window.getGLWindow());
    }

    public void openStamps(WindowInstance window) {
        tabManager.openStamps(window.getGLWindow());
    }

    public void openPalette(WindowInstance window) {
        tabManager.openPalette(window.getGLWindow());
    }

    public void openRecorder(WindowInstance window) {
        tabManager.openRecorder(window.getGLWindow());
    }

    // Assets Dropdown \\

    public void openItemEditor(WindowInstance window) {
        tabManager.openItemEditor(window.getGLWindow());
    }

    public void openShipwright(WindowInstance window) {
        tabManager.openShipwright(window.getGLWindow());
    }

    public void openShipViewer(WindowInstance window) {
        tabManager.openShipViewer(window.getGLWindow());
    }

    // Textures Dropdown \\

    public void openImageEditor(WindowInstance window) {
        tabManager.openImageEditor(window.getGLWindow());
    }

    // Characters Dropdown \\

    public void openCharacterAppearance(WindowInstance window) {
        tabManager.openCharacterAppearance(window.getGLWindow());
    }

    public void openCharacterEquipment(WindowInstance window) {
        tabManager.openCharacterEquipment(window.getGLWindow());
    }

    public void openCharacterMesh(WindowInstance window) {
        tabManager.openCharacterMesh(window.getGLWindow());
    }

    public void openCharacterTopology(WindowInstance window) {
        tabManager.openCharacterTopology(window.getGLWindow());
    }

    public void openCharacterSculpt(WindowInstance window) {
        tabManager.openCharacterSculpt(window.getGLWindow());
    }

    public void openCharacterWeights(WindowInstance window) {
        tabManager.openCharacterWeights(window.getGLWindow());
    }

    public void openCharacterPose(WindowInstance window) {
        tabManager.openCharacterPose(window.getGLWindow());
    }

    public void openCharacterPaint(WindowInstance window) {
        tabManager.openCharacterPaint(window.getGLWindow());
    }

    public void openCharacterRig(WindowInstance window) {
        tabManager.openCharacterRig(window.getGLWindow());
    }

    public void openCharacterUV(WindowInstance window) {
        tabManager.openCharacterUV(window.getGLWindow());
    }

    public void openUVEditor(WindowInstance window) {
        tabManager.openUVEditor(window.getGLWindow());
    }

    public void openCharacterHair(WindowInstance window) {
        tabManager.openCharacterHair(window.getGLWindow());
    }

    public void openMaterials(WindowInstance window) {
        tabManager.openMaterials(window.getGLWindow());
    }

    public void openMaterialViewer(WindowInstance window) {
        tabManager.openMaterialViewer(window.getGLWindow());
    }

    public void openAnimationEditor(WindowInstance window) {
        tabManager.openAnimationEditor(window.getGLWindow());
    }

    public void openAnimationTreeEditor(WindowInstance window) {
        tabManager.openAnimationTreeEditor(window.getGLWindow());
    }

    // World Dropdown \\

    public void openWorldMap(WindowInstance window) {
        tabManager.openWorldMap(window.getGLWindow());
    }

    public void openWorldEditor(WindowInstance window) {
        tabManager.openWorldEditor(window.getGLWindow());
    }

    public void openSettlementPlanner(WindowInstance window) {
        tabManager.openSettlementPlanner(window.getGLWindow());
    }

    public void openCameraPlanner(WindowInstance window) {
        tabManager.openCameraPlanner(window.getGLWindow());
    }

    public void openBiomePlanner(WindowInstance window) {
        tabManager.openBiomePlanner(window.getGLWindow());
    }

    public void openBiomeViewer(WindowInstance window) {
        tabManager.openBiomeViewer(window.getGLWindow());
    }

    public void openTreePlanner(WindowInstance window) {
        tabManager.openTreePlanner(window.getGLWindow());
    }

    public void openTreeViewer(WindowInstance window) {
        tabManager.openTreeViewer(window.getGLWindow());
    }

    // Media Dropdown \\

    public void openTimeline(WindowInstance window) {
        tabManager.openTimeline(window.getGLWindow());
    }

    public void openRecorderKeys(WindowInstance window) {
        tabManager.openRecorderKeys(window.getGLWindow());
    }

    public void openRecorderKeyGraph(WindowInstance window) {
        tabManager.openRecorderKeyGraph(window.getGLWindow());
    }

    public void openRecordings(WindowInstance window) {
        tabManager.openRecordings(window.getGLWindow());
    }

    public void openSoundLibrary(WindowInstance window) {
        tabManager.openSoundLibrary(window.getGLWindow());
    }

    public void openNoteLibrary(WindowInstance window) {
        tabManager.openNoteLibrary(window.getGLWindow());
    }

    public void openTransitions(WindowInstance window) {
        tabManager.openTransitions(window.getGLWindow());
    }

    // Debug Dropdown \\

    public void openPreview(WindowInstance window) {
        tabManager.openPreview(window.getGLWindow());
    }

    public void openSceneAssets(WindowInstance window) {
        tabManager.openSceneAssets(window.getGLWindow());
    }

    public void openConsole(WindowInstance window) {
        tabManager.openConsole(window.getGLWindow());
    }

    public void openCommandConsole(WindowInstance window) {
        tabManager.openCommandConsole(window.getGLWindow());
    }

    public void openProfiler(WindowInstance window) {
        tabManager.openProfiler(window.getGLWindow());
    }

    // Layout Dropdown \\

    public void refreshLayoutList(MenuInstance menu) {

        this.layoutMenu = menu;

        ElementInstance list = menu.getEntryPoint(EditorSetting.ENTRY_LAYOUTS_LIST);

        if (list == null)
            return;

        ObjectArrayList<ElementInstance> staleItems = new ObjectArrayList<>(list.getChildren());

        for (int i = 0; i < staleItems.size(); i++)
            menuManager.eject(menu, EditorSetting.ENTRY_LAYOUTS_LIST, staleItems.get(i));

        ObjectArrayList<String> layoutNames = layoutManager.getLayoutNames();

        for (int i = 0; i < layoutNames.size(); i++)
            injectLayoutItem(menu, layoutNames.get(i));

        fitLayoutList(menu, list, layoutNames.size());
    }

    // The list as tall as its rows and the dropdown as tall as the list, so every saved layout shows
    private void fitLayoutList(MenuInstance menu, ElementInstance list, int layoutCount) {

        float listHeight = layoutCount * EditorSetting.LAYOUTS_ROW_PIXELS;

        list.setSizeOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(EngineSetting.PERCENT_MAX),
                DimensionValueStruct.ofAbsolute(listHeight)));

        ElementInstance panel = menu.getEntryPoint(EditorSetting.ENTRY_LAYOUTS_PANEL);

        if (panel != null)
            panel.setSizeOverride(new DimensionVector2Struct(
                    DimensionValueStruct.ofAbsolute(EditorSetting.LAYOUTS_DROPDOWN_WIDTH_PIXELS),
                    DimensionValueStruct.ofAbsolute(listHeight + EditorSetting.LAYOUTS_DROPDOWN_BASE_PIXELS)));
    }

    private void injectLayoutItem(MenuInstance menu, String layoutName) {
        menuManager.inject(
                menu, EditorSetting.ENTRY_LAYOUTS_LIST, EditorSetting.MENU_EDITOR_LAYOUT_ITEM_TEMPLATE,
                item -> {
                    item.findChildById(EditorSetting.ELEMENT_LAYOUT_ITEM_LOAD).setActionArgOverride(layoutName);
                    item.findChildById(EditorSetting.ELEMENT_LAYOUT_ITEM_SAVE).setActionArgOverride(layoutName);
                    item.findChildById(EditorSetting.ELEMENT_LAYOUT_ITEM_DELETE).setActionArgOverride(layoutName);
                    item.findChildById(EditorSetting.ELEMENT_LAYOUT_ITEM_LABEL).setFontText(layoutName);
                });
    }

    // Layout Operations \\

    public void openCreateLayoutDialog(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_CREATE_LAYOUT,
                layoutManager::isLayoutNameAvailable,
                this::createLayout);
    }

    public void loadLayout(String layoutName) {
        layoutManager.loadLayout(layoutName);
    }

    public void saveLayout(String layoutName, WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_SAVE_OVER_PREFIX + layoutName + EditorSetting.DIALOG_TITLE_SAVE_OVER_SUFFIX,
                layoutName,
                layoutName::equals,
                layoutManager::saveLayout);
    }

    public void deleteLayout(String layoutName, WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + layoutName + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                layoutName::equals,
                this::removeLayout);
    }

    private void createLayout(String layoutName) {

        layoutManager.createLayout(layoutName);
        refreshLayoutList(layoutMenu);
    }

    private void removeLayout(String layoutName) {

        layoutManager.deleteLayout(layoutName);
        refreshLayoutList(layoutMenu);
    }
}
