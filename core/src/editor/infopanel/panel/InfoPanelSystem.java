package editor.infopanel.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.infopipeline.inforow.InfoRowStruct;
import editor.infopanel.InfoPanelSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the selected entry in
     * the info panel menu. Whenever InfoManager's revision moves it rewrites
     * the title and status lines and injects one row per value, group, and
     * missing optional field, indented by depth, with every button carrying
     * its row's path and the add and remove buttons kept only where allowed.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private InfoManager infoManager;

    // Menus
    private MenuInstance panelMenu;

    // Rows
    private ObjectArrayList<InfoRowStruct> rows;
    private ObjectArrayList<ElementInstance> rowElements;
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.rows = new ObjectArrayList<>();
        this.rowElements = new ObjectArrayList<>();
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.infoManager = get(InfoManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.panelMenu = menuManager.openMenu(InfoPanelSetting.MENU_INFO_PANEL, window);
    }

    @Override
    protected void dispose() {

        menuManager.closeMenu(panelMenu);
        menuManager.setMenuTargetFbo(context.getWindow(), null);
    }

    // Update \\

    @Override
    protected void update() {

        if (infoManager.getRevision() == shownRevision)
            return;

        this.shownRevision = infoManager.getRevision();

        setEntryText(InfoPanelSetting.ENTRY_TITLE, infoManager.getTitleText());
        setEntryText(InfoPanelSetting.ENTRY_STATUS, infoManager.getStatusText());
        refreshRows();
    }

    private void refreshRows() {

        for (int i = 0; i < rowElements.size(); i++)
            menuManager.eject(panelMenu, InfoPanelSetting.ENTRY_ROWS, rowElements.get(i));

        rowElements.clear();
        rows.clear();
        infoManager.buildRows(rows);

        for (int i = 0; i < rows.size(); i++)
            rowElements.add(injectRow(rows.get(i)));
    }

    // Rows \\

    private ElementInstance injectRow(InfoRowStruct row) {

        return switch (row.getKind()) {
            case VALUE -> menuManager.inject(
                    panelMenu, InfoPanelSetting.ENTRY_ROWS, InfoPanelSetting.TEMPLATE_VALUE_ROW,
                    element -> fillValueRow(element, row));
            case GROUP -> menuManager.inject(
                    panelMenu, InfoPanelSetting.ENTRY_ROWS, InfoPanelSetting.TEMPLATE_GROUP_ROW,
                    element -> fillGroupRow(element, row));
            case MISSING -> menuManager.inject(
                    panelMenu, InfoPanelSetting.ENTRY_ROWS, InfoPanelSetting.TEMPLATE_MISSING_ROW,
                    element -> fillMissingRow(element, row));
        };
    }

    private void fillValueRow(ElementInstance element, InfoRowStruct row) {

        fillKeyLabel(element, row);
        bindChild(element, InfoPanelSetting.ELEMENT_VALUE_BUTTON, row.getPath(), true);
        bindChild(element, InfoPanelSetting.ELEMENT_REMOVE_BUTTON, row.getPath(), row.isRemovable());
        setChildText(element, InfoPanelSetting.ELEMENT_VALUE_LABEL, row.getValueText());
    }

    private void fillGroupRow(ElementInstance element, InfoRowStruct row) {

        element.setActionArgOverride(row.getPath());
        fillKeyLabel(element, row);
        bindChild(element, InfoPanelSetting.ELEMENT_ADD_BUTTON, row.getPath(), row.isAddable());
        bindChild(element, InfoPanelSetting.ELEMENT_REMOVE_BUTTON, row.getPath(), row.isRemovable());

        ElementInstance toggleLabel = element.findChildById(InfoPanelSetting.ELEMENT_TOGGLE_LABEL);

        if (toggleLabel == null)
            return;

        toggleLabel.setPositionOverride(toOffset(toIndent(row)));
        toggleLabel.setFontText(row.isExpanded()
                ? EngineSetting.HIERARCHY_EXPANDED_MARKER
                : EngineSetting.HIERARCHY_COLLAPSED_MARKER);
    }

    private void fillMissingRow(ElementInstance element, InfoRowStruct row) {

        fillKeyLabel(element, row);
        bindChild(element, InfoPanelSetting.ELEMENT_ADD_BUTTON, row.getPath(), true);
    }

    private void fillKeyLabel(ElementInstance element, InfoRowStruct row) {

        ElementInstance keyLabel = element.findChildById(InfoPanelSetting.ELEMENT_KEY_LABEL);

        if (keyLabel == null)
            return;

        keyLabel.setPositionOverride(toOffset(toIndent(row) + InfoPanelSetting.TOGGLE_WIDTH_PIXELS));
        keyLabel.setFontText(row.getLabel());
    }

    // Utility \\

    private void bindChild(ElementInstance element, String childId, String path, boolean kept) {

        ElementInstance child = element.findChildById(childId);

        if (child == null)
            return;

        if (kept)
            child.setActionArgOverride(path);
        else
            element.removeChild(child);
    }

    private void setChildText(ElementInstance element, String childId, String text) {

        ElementInstance child = element.findChildById(childId);

        if (child != null)
            child.setFontText(text);
    }

    private void setEntryText(int entryPoint, String text) {

        ElementInstance label = panelMenu.getEntryPoint(entryPoint);

        if (label != null)
            label.setFontText(text);
    }

    private float toIndent(InfoRowStruct row) {
        return InfoPanelSetting.ROW_OFFSET_PIXELS + row.getDepth() * InfoPanelSetting.INDENT_PIXELS;
    }

    private DimensionVector2Struct toOffset(float x) {
        return new DimensionVector2Struct(DimensionValueStruct.ofAbsolute(x), DimensionValueStruct.ofAbsolute(0f));
    }
}
