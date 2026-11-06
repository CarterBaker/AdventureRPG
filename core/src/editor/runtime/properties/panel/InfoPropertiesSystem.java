package editor.runtime.properties.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.infopipeline.inforow.InfoRowStruct;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.runtime.properties.PropertiesSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoPropertiesSystem extends SystemPackage implements PropertiesView {

    /*
     * The Properties tab's view of the content entry selected in the
     * Hierarchy, the editor's Info Panel. Whenever InfoManager's revision
     * moves while it shows, it injects one row per value, group, and missing
     * optional field, indented by depth, with every button carrying its
     * row's path and the add and remove buttons kept only where allowed. A
     * row holding an error is drawn in the danger color; a row the game
     * ignores is drawn muted. The title is the entry's; the toolbar and
     * status line are the tab's own.
     */

    // Internal
    private MenuManager menuManager;
    private InfoManager infoManager;
    private PropertiesSystem propertiesSystem;

    // Colors
    private MenuColorStruct problemColor;
    private MenuColorStruct ignoredColor;

    // Rows
    private ObjectArrayList<InfoRowStruct> rows;
    private ObjectArrayList<ElementInstance> rowElements;
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.problemColor = new MenuColorStruct(ThemeColor.DANGER, EngineSetting.COLOR_CHANNEL_MAX);
        this.ignoredColor = new MenuColorStruct(ThemeColor.TEXT_MUTED, EngineSetting.COLOR_CHANNEL_MAX);

        // Rows
        this.rows = new ObjectArrayList<>();
        this.rowElements = new ObjectArrayList<>();
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.infoManager = get(InfoManager.class);
        this.propertiesSystem = get(PropertiesSystem.class);
    }

    // View \\

    @Override
    public boolean refresh(PropertiesProvider provider, boolean force) {

        if (!force && infoManager.getRevision() == shownRevision)
            return false;

        this.shownRevision = infoManager.getRevision();
        clear();
        infoManager.buildRows(rows);

        for (int i = 0; i < rows.size(); i++)
            rowElements.add(injectRow(rows.get(i)));

        return true;
    }

    @Override
    public String getTitle(PropertiesProvider provider) {
        return infoManager.getTitleText();
    }

    @Override
    public void clear() {

        MenuInstance menu = propertiesSystem.getMenu();

        for (int i = 0; i < rowElements.size(); i++)
            menuManager.eject(menu, PropertiesSetting.ENTRY_ROWS, rowElements.get(i));

        rowElements.clear();
        rows.clear();
    }

    // Rows \\

    private ElementInstance injectRow(InfoRowStruct row) {

        return switch (row.getKind()) {
            case VALUE -> menuManager.inject(
                    propertiesSystem.getMenu(), PropertiesSetting.ENTRY_ROWS, PropertiesSetting.TEMPLATE_VALUE_ROW,
                    element -> fillValueRow(element, row));
            case GROUP -> menuManager.inject(
                    propertiesSystem.getMenu(), PropertiesSetting.ENTRY_ROWS, PropertiesSetting.TEMPLATE_GROUP_ROW,
                    element -> fillGroupRow(element, row));
            case MISSING -> menuManager.inject(
                    propertiesSystem.getMenu(), PropertiesSetting.ENTRY_ROWS, PropertiesSetting.TEMPLATE_MISSING_ROW,
                    element -> fillMissingRow(element, row));
        };
    }

    private void fillValueRow(ElementInstance element, InfoRowStruct row) {

        fillKeyLabel(element, row);
        bindChild(element, PropertiesSetting.ELEMENT_VALUE_BUTTON, row.getPath(), true);
        bindChild(element, PropertiesSetting.ELEMENT_REMOVE_BUTTON, row.getPath(), row.isRemovable());
        setChildText(element, PropertiesSetting.ELEMENT_VALUE_LABEL, row.getValueText());
        markIssue(element, PropertiesSetting.ELEMENT_VALUE_LABEL, row);
    }

    private void fillGroupRow(ElementInstance element, InfoRowStruct row) {

        element.setActionArgOverride(row.getPath());
        fillKeyLabel(element, row);
        bindChild(element, PropertiesSetting.ELEMENT_ADD_BUTTON, row.getPath(), row.isAddable());
        bindChild(element, PropertiesSetting.ELEMENT_REMOVE_BUTTON, row.getPath(), row.isRemovable());

        ElementInstance toggleLabel = element.findChildById(PropertiesSetting.ELEMENT_TOGGLE_LABEL);

        if (toggleLabel == null)
            return;

        toggleLabel.setPositionOverride(toOffset(toIndent(row)));
        toggleLabel.setFontText(row.isExpanded()
                ? EngineSetting.HIERARCHY_EXPANDED_MARKER
                : EngineSetting.HIERARCHY_COLLAPSED_MARKER);
    }

    private void fillMissingRow(ElementInstance element, InfoRowStruct row) {

        fillKeyLabel(element, row);
        bindChild(element, PropertiesSetting.ELEMENT_ADD_BUTTON, row.getPath(), true);
    }

    private void fillKeyLabel(ElementInstance element, InfoRowStruct row) {

        ElementInstance keyLabel = element.findChildById(PropertiesSetting.ELEMENT_KEY_LABEL);

        if (keyLabel == null)
            return;

        keyLabel.setPositionOverride(toOffset(toIndent(row) + PropertiesSetting.TOGGLE_WIDTH_PIXELS));
        keyLabel.setFontText(row.getLabel());
        markIssue(element, PropertiesSetting.ELEMENT_KEY_LABEL, row);
    }

    // Utility \\

    private void bindChild(ElementInstance element, String childId, String path, boolean kept) {

        ElementInstance child = element.findChildById(childId);

        if (child == null)
            return;

        if (kept)
            child.setActionArgOverride(path);
        else
            menuManager.eject(element, child);
    }

    private void setChildText(ElementInstance element, String childId, String text) {

        ElementInstance child = element.findChildById(childId);

        if (child != null)
            child.setFontText(text);
    }

    private void markIssue(ElementInstance element, String childId, InfoRowStruct row) {

        ElementInstance child = element.findChildById(childId);

        if (child == null)
            return;

        if (row.hasProblem())
            child.setColorOverride(problemColor);
        else if (row.isIgnored())
            child.setColorOverride(ignoredColor);
    }



    private float toIndent(InfoRowStruct row) {
        return PropertiesSetting.ROW_OFFSET_PIXELS + row.getDepth() * PropertiesSetting.INDENT_PIXELS;
    }

    private DimensionVector2Struct toOffset(float x) {
        return new DimensionVector2Struct(DimensionValueStruct.ofAbsolute(x), DimensionValueStruct.ofAbsolute(0f));
    }
}
