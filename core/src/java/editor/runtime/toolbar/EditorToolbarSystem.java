package editor.runtime.toolbar;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.ElementHitSystem;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.StripLayoutUtility;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.runtime.EditorSetting;
import editor.runtime.dock.EditorDockSystem;
import editor.runtime.menueventsmanager.menus.search.EditorSearchBranch;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorToolbarSystem extends SystemPackage {

    /*
     * Owns the toolbar along the top of an editor window, which doubles as the
     * window's title bar: the context picks its menu through openToolbar(), so
     * the main window's carries the editor's menus and a secondary window's
     * only its title, and both carry the frame's controls and border. The
     * toolbar lives in its own logical window so its dropdowns and the border
     * composite above tabs. Each frame it is kept sized to the OS window,
     * re-raised whenever anything has been brought up over it, and given an
     * input rect: the strip above the dock canvas while idle, so it never
     * covers the tabs hover and focus resolve against, and the whole window
     * while one of its dropdowns is hovered open or the search lists its
     * results below it. Only input changes — the
     * toolbar is never resized for it, so its FBO and layout stay stable. The
     * main window's menus are laid out as every tab strip is
     * (StripLayoutUtility), again only when the window's width moves: each at
     * its preferred width while the bar has room, all shrinking together as it
     * narrows, then wrapping onto more rows, the bar growing and the dock
     * shrinking beneath it, so no menu is ever hidden. A divider closes the
     * menus' first row, and the search is centred in the room between it and
     * the divider before Layouts.
     */

    // Internal
    private WindowManager windowManager;
    private MenuManager menuManager;
    private ElementHitSystem elementHitSystem;
    private EditorDockSystem editorDockSystem;
    private EditorSearchBranch editorSearchBranch;

    // Menus
    private MenuInstance toolbarMenu;

    // Menu Bar — the menus laid out, the divider after them, the search, the bar behind them, the dock beneath and
    // the width they were laid out for
    private ObjectArrayList<ElementInstance> barMenus;
    private ElementInstance endDivider;
    private ElementInstance search;
    private ElementInstance bar;
    private ElementInstance dock;
    private int shownWidth;

    // Base \\

    @Override
    protected void create() {

        // Menu Bar
        this.barMenus = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.menuManager = get(MenuManager.class);
        this.elementHitSystem = get(ElementHitSystem.class);
        this.editorDockSystem = get(EditorDockSystem.class);
        this.editorSearchBranch = get(EditorSearchBranch.class);
    }

    @Override
    protected void dispose() {
        menuManager.closeMenuWindow(toolbarMenu);
    }

    // Management \\

    public void openToolbar(String menuName) {

        this.toolbarMenu = menuManager.openMenuWindow(menuName, context.getWindow());
        this.bar = findElement(toolbarMenu, EditorSetting.MENU_BAR_ELEMENT);
        this.endDivider = findElement(toolbarMenu, EditorSetting.MENU_BAR_END_DIVIDER_ELEMENT);
        this.search = findElement(toolbarMenu, EditorSetting.ELEMENT_SEARCH_MENU);
        this.dock = findElement(editorDockSystem.getBaseMenu(), EditorSetting.MENU_BAR_DOCK_ELEMENT);

        for (String menuElement : EditorSetting.MENU_BAR_MENU_ELEMENTS) {

            ElementInstance menu = findElement(toolbarMenu, menuElement);

            if (menu != null)
                barMenus.add(menu);
        }
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance osWindow = context.getWindow();
        WindowInstance toolbarWindow = toolbarMenu.getWindow();
        int w = osWindow.getWidth();
        int h = osWindow.getHeight();

        if (w > 0 && h > 0 && (w != toolbarWindow.getWidth() || h != toolbarWindow.getHeight()))
            toolbarWindow.place(0, 0, w, h);

        if (w > 0 && w != shownWidth && !barMenus.isEmpty())
            layoutMenuBar(w);

        float inputTop = elementHitSystem.getHoveredMenu() == toolbarMenu
                || editorSearchBranch.isShowingResults(toolbarMenu)
                ? 0f
                : editorDockSystem.getDockTop();
        toolbarWindow.setInputRect(0, inputTop, w, h - inputTop);

        if (!windowManager.isFrontmost(toolbarWindow))
            windowManager.bringToFront(toolbarWindow);
    }

    // Menu Bar \\

    private void layoutMenuBar(int width) {

        this.shownWidth = width;

        float available = width - EditorSetting.MENU_BAR_LEFT_PIXELS - EditorSetting.MENU_BAR_RIGHT_RESERVE_PIXELS;
        float row = EditorSetting.MENU_BAR_ROW_PIXELS;
        int count = barMenus.size();
        int perRow = StripLayoutUtility.resolvePerRow(
                available,
                count,
                EditorSetting.MENU_BAR_MENU_MIN_WIDTH_PIXELS,
                0f);
        float menuWidth = StripLayoutUtility.resolveWidth(
                available,
                perRow,
                EditorSetting.MENU_BAR_MENU_WIDTH_PIXELS,
                0f);
        float extra = (StripLayoutUtility.resolveRowCount(count, perRow) - 1) * row;
        DimensionVector2Struct menuSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(menuWidth),
                DimensionValueStruct.ofAbsolute(row));

        for (int i = 0; i < count; i++) {

            ElementInstance menu = barMenus.get(i);

            menu.setPositionOverride(new DimensionVector2Struct(
                    DimensionValueStruct.ofAbsolute(EditorSetting.MENU_BAR_LEFT_PIXELS + (i % perRow) * menuWidth),
                    DimensionValueStruct.ofAbsolute(-(i / perRow) * row)));
            menu.setSizeOverride(menuSize);
        }

        float menusEnd = EditorSetting.MENU_BAR_LEFT_PIXELS + Math.min(count, perRow) * menuWidth;
        float searchStart = menusEnd + EditorSetting.MENU_BAR_DIVIDER_WIDTH_PIXELS;
        float searchRoom = width - EditorSetting.MENU_BAR_RIGHT_EDGE_PIXELS - searchStart;
        float searchWidth = Math.clamp(
                searchRoom - 2f * EditorSetting.MENU_BAR_SEARCH_GAP_PIXELS,
                EditorSetting.MENU_BAR_SEARCH_MIN_WIDTH_PIXELS,
                EditorSetting.MENU_BAR_SEARCH_MAX_WIDTH_PIXELS);

        placeOnFirstRow(endDivider, menusEnd, EditorSetting.MENU_BAR_DIVIDER_WIDTH_PIXELS);
        placeOnFirstRow(search, searchStart + (searchRoom - searchWidth) * 0.5f, searchWidth);

        if (bar != null)
            bar.setSizeOverride(new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(EngineSetting.PERCENT_MAX),
                    DimensionValueStruct.ofAbsolute(row + extra)));

        if (dock != null)
            dock.setSizeOverride(new DimensionVector2Struct(
                    DimensionValueStruct.ofPercentWithOffset(
                            EngineSetting.PERCENT_MAX,
                            -EditorSetting.MENU_BAR_DOCK_INSET_X_PIXELS),
                    DimensionValueStruct.ofPercentWithOffset(
                            EngineSetting.PERCENT_MAX,
                            -EditorSetting.MENU_BAR_DOCK_INSET_Y_PIXELS - extra)));
    }

    private void placeOnFirstRow(ElementInstance element, float x, float width) {

        if (element == null)
            return;

        element.setPositionOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(x),
                DimensionValueStruct.ofAbsolute(0f)));
        element.setSizeOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(width),
                DimensionValueStruct.ofAbsolute(EditorSetting.MENU_BAR_ROW_PIXELS)));
    }

    // Utility \\

    private ElementInstance findElement(MenuInstance menu, String id) {

        if (menu == null)
            return null;

        ObjectArrayList<ElementInstance> elements = menu.getElements();

        for (int i = 0; i < elements.size(); i++) {

            ElementInstance element = elements.get(i);

            if (element.getElementData().getId().equals(id))
                return element;

            ElementInstance found = element.findChildById(id);

            if (found != null)
                return found;
        }

        return null;
    }
}
