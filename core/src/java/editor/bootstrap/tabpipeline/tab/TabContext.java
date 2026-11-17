package editor.bootstrap.tabpipeline.tab;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.StripLayoutUtility;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.runtime.EditorSetting;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TabContext extends ContextPackage {

    /*
     * One editor tab: a chrome window with its menu, plus the content context
     * living in its canvas. Placement, content sync, reparenting, z-ordering
     * and the tab strip all go through here. The tab a stack shows is placed
     * on its leaf with a strip listing the whole stack, rebuilt only when the
     * stack changes or its width fits a different number of entries to a row;
     * the entries shrink together as the tab narrows and then wrap onto more
     * rows (StripLayoutUtility), the header growing to hold them and the
     * canvas shrinking beneath it, so every entry stays readable. Every other
     * tab in the stack is withdrawn with its content.
     * The chrome's header is tinted and its border drawn in the active
     * window's color while its content is the active window, and its header
     * tinted more faintly in the hovered window's while the pointer is over
     * it, set only when that changes. dispose() is the single teardown path
     * however the tab closes.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WindowManager windowManager;
    private InputManager inputManager;
    private DockLayoutSystem dockLayoutSystem;
    private TabManager tabManager;
    private ActiveWindowManager activeWindowManager;

    // Chrome
    private MenuInstance chromeMenu;
    private ElementInstance headerElement;
    private ElementInstance outlineElement;
    private ElementInstance canvasElement;

    // Highlight — the colors the chrome shows, null for its own
    private MenuColorStruct activeHeaderColor;
    private MenuColorStruct hoverHeaderColor;
    private MenuColorStruct activeOutlineColor;
    private MenuColorStruct shownHeaderColor;
    private MenuColorStruct shownOutlineColor;

    // Content — owned by this tab, positioned within this tab's canvas
    private ContextPackage contentContext;

    // Identity — the handle that owns this context, wired immediately after
    // both are constructed. Lets dispose() deregister itself from TabManager
    // without TabManager needing to know anything about teardown order.
    private TabHandle ownerHandle;

    // Strip — the stack it lists, and this tab alone for while it is out of every stack
    private ObjectArrayList<TabHandle> stripTabs;
    private ObjectArrayList<TabHandle> loneStrip;
    private ObjectArrayList<ElementInstance> stripEntries;
    private MenuColorStruct activeStripColor;
    private MenuColorStruct activeStripAccentColor;
    private float shownStripWidth;
    private int shownPerRow;
    private int shownRowCount;

    // Internal \\

    @Override
    protected void create() {

        // Strip
        this.stripTabs = new ObjectArrayList<>();
        this.loneStrip = new ObjectArrayList<>(1);
        this.stripEntries = new ObjectArrayList<>();
        this.shownStripWidth = EngineSetting.INDEX_NOT_FOUND;
        this.shownPerRow = EngineSetting.INDEX_NOT_FOUND;
        this.shownRowCount = EngineSetting.INDEX_NOT_FOUND;
        this.activeStripColor = new MenuColorStruct(ThemeColor.ACCENT, EditorSetting.TAB_STRIP_ACTIVE_ALPHA);
        this.activeStripAccentColor = new MenuColorStruct(ThemeColor.ACCENT, EngineSetting.MENU_THEME_ALPHA_DEFAULT);

        // Highlight
        this.activeHeaderColor = new MenuColorStruct(ThemeColor.WINDOW_ACTIVE, EditorSetting.TAB_HEADER_ACTIVE_ALPHA);
        this.hoverHeaderColor = new MenuColorStruct(ThemeColor.WINDOW_HOVER, EditorSetting.TAB_HEADER_HOVER_ALPHA);
        this.activeOutlineColor = new MenuColorStruct(ThemeColor.WINDOW_ACTIVE, EngineSetting.MENU_THEME_ALPHA_DEFAULT);
    }

    @Override
    protected void get() {
        menuManager = get(MenuManager.class);
        fboManager = get(FBOManager.class);
        windowManager = get(WindowManager.class);
        inputManager = get(InputManager.class);
        dockLayoutSystem = get(DockLayoutSystem.class);
        tabManager = get(TabManager.class);
        activeWindowManager = get(ActiveWindowManager.class);
    }

    @Override
    protected void awake() {
        menuManager.setMenuTargetFbo(getWindow(), fboManager.cloneFbo(RuntimeSetting.FBO_UI, getWindow()));
        chromeMenu = menuManager.openMenu(EditorSetting.MENU_TAB_SHELL, getWindow());
        headerElement = findChromeElement(EditorSetting.ELEMENT_TAB_TOOLBAR);
        outlineElement = findChromeElement(EditorSetting.ELEMENT_TAB_OUTLINE);
        canvasElement = findChromeElement(EditorSetting.ELEMENT_TAB_CANVAS);
    }

    @Override
    protected void update() {

        float stripWidth = resolveStripWidth();

        if (stripWidth != shownStripWidth)
            fitStrip(stripWidth);

        if (contentContext == null)
            return;

        WindowInstance contentWindow = contentContext.getWindow();
        WindowInstance hovered = windowManager.getHoveredWindow();
        boolean active = activeWindowManager.isActive(contentWindow);
        boolean hover = hovered != null && tabManager.resolveContentWindow(hovered) == contentWindow;

        showHighlight(
                active ? activeHeaderColor : hover ? hoverHeaderColor : null,
                active ? activeOutlineColor : null);
    }

    // Dispose \\

    @Override
    protected void dispose() {

        WindowInstance osWindow = getWindow().getGLWindow();
        dockLayoutSystem.removeTab(osWindow, ownerHandle);

        if (contentContext != null) {
            contentContext.getWindow().getMenuListHandle().setLockReleaseListener(null);
            contentContext.getWindow().dispose();
        }

        tabManager.deregisterTab(ownerHandle);
    }

    // Management \\

    public void linkContent(ContextPackage contentContext) {

        this.contentContext = contentContext;

        contentContext.getWindow().getMenuListHandle().setLockReleaseListener(
                () -> inputManager.onInputLockReleased(contentContext.getWindow()));
    }

    public void setOwnerHandle(TabHandle handle) {
        this.ownerHandle = handle;
        loneStrip.add(handle);
        showStrip(loneStrip);
    }

    public void bringToFront() {

        if (contentContext == null)
            throwException("bringToFront() called before linkContent() — chrome and content must be paired first.");

        windowManager.bringToFront(getWindow());
        windowManager.bringToFront(contentContext.getWindow());
    }

    // The stack's shown tab fills its leaf; any other tab in it waits withdrawn behind that one
    public void dockIn(DockNodeStruct leaf) {

        if (leaf.getActiveTab() != ownerHandle) {
            withdraw();
            return;
        }

        placeAt(leaf.getX(), leaf.getY(), leaf.getW(), leaf.getH());
        showStrip(leaf.getTabs());
    }

    public void undock() {
        showStrip(loneStrip);
    }

    public void placeAt(float x, float y, float w, float h) {

        if (w <= 0 || h <= 0)
            return;

        getWindow().place(x, y, w, h);
    }

    public void syncContent() {

        if (contentContext == null || chromeMenu == null || chromeMenu.getCanvas() == null)
            return;

        WindowInstance tabWindow = getWindow();

        if (!tabWindow.hasCompositeRect())
            return;

        float cw = chromeMenu.getCanvas().getW();
        float ch = chromeMenu.getCanvas().getH();

        if (cw <= 0 || ch <= 0)
            return;

        float cx = tabWindow.getCompositeX() + chromeMenu.getCanvas().getX();
        float cy = tabWindow.getCompositeY() + chromeMenu.getCanvas().getY();

        WindowInstance contentWindow = contentContext.getWindow();

        if (contentWindow.isPlacedAt(cx, cy, cw, ch))
            return;

        contentWindow.place(cx, cy, cw, ch);
    }

    public void moveTo(WindowInstance targetOsWindow) {
        windowManager.reparentWindow(getWindow(), targetOsWindow);
        windowManager.reparentWindow(contentContext.getWindow(), targetOsWindow);
    }

    // Utility \\

    private void showHighlight(MenuColorStruct headerColor, MenuColorStruct outlineColor) {

        if (headerColor == shownHeaderColor && outlineColor == shownOutlineColor)
            return;

        this.shownHeaderColor = headerColor;
        this.shownOutlineColor = outlineColor;
        headerElement.setColorOverride(headerColor);
        outlineElement.setColorOverride(outlineColor);
    }

    private ElementInstance findChromeElement(String elementID) {

        for (int i = 0; i < chromeMenu.getElements().size(); i++) {

            ElementInstance element = chromeMenu.getElements().get(i);

            if (element.getElementData().getId().equals(elementID))
                return element;
        }

        return throwException("Tab chrome menu has no element '" + elementID + "'.");
    }

    private void withdraw() {

        WindowInstance contentWindow = contentContext.getWindow();

        getWindow().withdraw();
        contentWindow.withdraw();
        inputManager.releaseWindow(contentWindow);
    }

    private void showStrip(ObjectArrayList<TabHandle> tabs) {

        if (stripTabs.equals(tabs))
            return;

        stripTabs.clear();
        stripTabs.addAll(tabs);
        layoutStrip(resolveStripWidth());
    }

    // The strip refitted to a new width: listed again when a row holds a different number of entries, else every entry
    // resized in place
    private void fitStrip(float stripWidth) {

        int perRow = resolvePerRow(stripWidth);

        if (perRow != shownPerRow)
            layoutStrip(stripWidth);
        else
            resizeStrip(stripWidth, perRow);
    }

    // Every tab in the stack listed again, as many to a row as fit, the header and canvas sized for the rows
    private void layoutStrip(float stripWidth) {

        int perRow = resolvePerRow(stripWidth);
        ElementInstance row = null;

        menuManager.ejectAll(chromeMenu, EditorSetting.TAB_ENTRY_STRIP);
        stripEntries.clear();

        for (int i = 0; i < stripTabs.size(); i++) {

            if (i % perRow == 0)
                row = menuManager.inject(chromeMenu, EditorSetting.TAB_ENTRY_STRIP, EditorSetting.MENU_TAB_STRIP_ROW);

            stripEntries.add(injectStripEntry(row, stripTabs.get(i)));
        }

        this.shownPerRow = perRow;
        resizeHeader(StripLayoutUtility.resolveRowCount(stripTabs.size(), perRow));
        resizeStrip(stripWidth, perRow);
    }

    private void resizeStrip(float stripWidth, int perRow) {

        float entryWidth = StripLayoutUtility.resolveWidth(
                stripWidth,
                perRow,
                EditorSetting.TAB_STRIP_ENTRY_WIDTH_PIXELS,
                EditorSetting.TAB_STRIP_SPACING_PIXELS);
        DimensionVector2Struct size = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(entryWidth),
                DimensionValueStruct.ofPercent(EngineSetting.PERCENT_MAX));

        for (int i = 0; i < stripEntries.size(); i++)
            stripEntries.get(i).setSizeOverride(size);

        this.shownStripWidth = stripWidth;
    }

    // The header grown to hold every row of the strip and the canvas shrunk beneath it, only when the rows change
    private void resizeHeader(int rowCount) {

        if (rowCount == shownRowCount)
            return;

        float headerHeight = StripLayoutUtility.resolveStripHeight(
                rowCount,
                EditorSetting.TAB_STRIP_ROW_HEIGHT_PIXELS,
                EditorSetting.TAB_STRIP_SPACING_PIXELS) + EditorSetting.TAB_HEADER_PADDING_PIXELS;

        headerElement.setSizeOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofPercentWithOffset(
                        EngineSetting.PERCENT_MAX, -EditorSetting.TAB_HEADER_MARGIN_PIXELS),
                DimensionValueStruct.ofAbsolute(headerHeight)));
        canvasElement.setSizeOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofPercentWithOffset(
                        EngineSetting.PERCENT_MAX, -EditorSetting.TAB_CANVAS_MARGIN_PIXELS),
                DimensionValueStruct.ofPercentWithOffset(
                        EngineSetting.PERCENT_MAX, -(headerHeight + EditorSetting.TAB_CANVAS_GAP_PIXELS))));
        this.shownRowCount = rowCount;
    }

    private int resolvePerRow(float stripWidth) {
        return StripLayoutUtility.resolvePerRow(
                stripWidth,
                stripTabs.size(),
                EditorSetting.TAB_STRIP_ENTRY_MIN_WIDTH_PIXELS,
                EditorSetting.TAB_STRIP_SPACING_PIXELS);
    }

    private float resolveStripWidth() {
        return getWindow().getWidth() - EditorSetting.TAB_STRIP_MARGIN_PIXELS;
    }

    private ElementInstance injectStripEntry(ElementInstance row, TabHandle tab) {
        return menuManager.inject(
                row, EditorSetting.MENU_TAB_STRIP_ENTRY,
                entry -> {
                    entry.setOnDragArgOverride(tab.getTabTitle());
                    entry.findChildById(EditorSetting.ELEMENT_TAB_STRIP_LABEL).setFontText(tab.getBaseTitle());
                    entry.findChildById(EditorSetting.ELEMENT_TAB_STRIP_CLOSE).setActionArgOverride(tab.getTabTitle());

                    if (tab != ownerHandle)
                        return;

                    entry.setColorOverride(activeStripColor);
                    entry.findChildById(EditorSetting.ELEMENT_TAB_STRIP_ACCENT)
                            .setColorOverride(activeStripAccentColor);
                });
    }

    // Accessible \\

    public MenuInstance getChromeMenu() {
        return chromeMenu;
    }

    public ContextPackage getContentContext() {
        return contentContext;
    }
}
