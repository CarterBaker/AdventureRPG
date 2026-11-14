package editor.runtime.properties.panel;

import java.util.function.Consumer;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.infopipeline.inforow.InfoRowStruct;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.runtime.EditorSetting;
import editor.runtime.properties.PropertiesSetting;
import editor.runtime.slider.SliderUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoPropertiesSystem extends SystemPackage implements PropertiesView {

    /*
     * The Properties tab's view of the content entry selected in the
     * Hierarchy, the editor's Info Panel. Whenever InfoManager's revision
     * moves while it shows, it lists the entry's rows again: when they are
     * laid out as before it sets the moved values and knobs in place, so a
     * slider being dragged keeps its row; otherwise it injects one row per
     * value, group, and missing optional field, indented by depth, with every
     * button carrying its row's path and the add, remove and duplicate
     * buttons kept only where allowed. A number slides across its declared
     * range, or by steps about where it stood when listed; a choice steps
     * through what it may name; both still open for typing. A row holding an
     * error is drawn in the danger color; a row the game ignores is drawn
     * muted. The title is the entry's; the status line is the tab's own.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private InfoManager infoManager;
    private PropertiesSystem propertiesSystem;

    // Colors
    private MenuColorStruct problemColor;
    private MenuColorStruct ignoredColor;

    // Rows
    private ObjectArrayList<InfoRowStruct> rows;
    private ObjectArrayList<InfoRowStruct> listedRows;
    private ObjectArrayList<ElementInstance> rowElements;
    private IntArrayList sliderSteps;
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.problemColor = new MenuColorStruct(ThemeColor.DANGER, EngineSetting.COLOR_CHANNEL_MAX);
        this.ignoredColor = new MenuColorStruct(ThemeColor.TEXT_MUTED, EngineSetting.COLOR_CHANNEL_MAX);

        // Rows
        this.rows = new ObjectArrayList<>();
        this.listedRows = new ObjectArrayList<>();
        this.rowElements = new ObjectArrayList<>();
        this.sliderSteps = new IntArrayList();
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.infoManager = get(InfoManager.class);
        this.propertiesSystem = get(PropertiesSystem.class);
    }

    // View \\

    @Override
    public boolean refresh(PropertiesProvider provider, boolean force) {

        if (!force && infoManager.getRevision() == shownRevision)
            return false;

        this.shownRevision = infoManager.getRevision();
        listedRows.clear();
        infoManager.buildRows(listedRows);

        if (!force && isLaidOutAsShown()) {
            refreshInPlace();
            return true;
        }

        clear();
        rows.addAll(listedRows);

        for (int i = 0; i < rows.size(); i++) {
            rowElements.add(injectRow(rows.get(i)));
            sliderSteps.add(0);
        }

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
        sliderSteps.clear();
    }

    // Refresh \\

    private boolean isLaidOutAsShown() {

        if (listedRows.size() != rows.size())
            return false;

        for (int i = 0; i < rows.size(); i++)
            if (!rows.get(i).isLaidOutAs(listedRows.get(i)))
                return false;

        return true;
    }

    // Sets every value and ranged knob anew where the rows stand as they were
    private void refreshInPlace() {

        for (int i = 0; i < rows.size(); i++) {

            InfoRowStruct row = listedRows.get(i);
            ElementInstance element = rowElements.get(i);

            rows.set(i, row);
            setChildText(element, PropertiesSetting.ELEMENT_VALUE_LABEL, row.getValueText());

            if (row.hasRange())
                placeRangedKnob(element, row);
        }
    }

    // Rows \\

    private ElementInstance injectRow(InfoRowStruct row) {

        return switch (row.getKind()) {
            case VALUE -> inject(PropertiesSetting.TEMPLATE_VALUE_ROW, element -> fillValueRow(element, row));
            case NUMBER -> inject(PropertiesSetting.TEMPLATE_NUMBER_ROW, element -> fillNumberRow(element, row));
            case CHOICE -> inject(PropertiesSetting.TEMPLATE_CHOICE_ROW, element -> fillChoiceRow(element, row));
            case GROUP -> inject(PropertiesSetting.TEMPLATE_GROUP_ROW, element -> fillGroupRow(element, row));
            case MISSING -> inject(PropertiesSetting.TEMPLATE_MISSING_ROW, element -> fillMissingRow(element, row));
        };
    }

    private ElementInstance inject(String template, Consumer<ElementInstance> filler) {
        return menuManager.inject(propertiesSystem.getMenu(), PropertiesSetting.ENTRY_ROWS, template, filler);
    }

    private void fillValueRow(ElementInstance element, InfoRowStruct row) {

        fillKeyLabel(element, row);
        bindChild(element, PropertiesSetting.ELEMENT_VALUE_BUTTON, row.getPath(), true);
        bindChild(element, PropertiesSetting.ELEMENT_REMOVE_BUTTON, row.getPath(), row.isRemovable());
        setChildText(element, PropertiesSetting.ELEMENT_VALUE_LABEL, row.getValueText());
        markIssue(element, PropertiesSetting.ELEMENT_VALUE_LABEL, row);
    }

    private void fillNumberRow(ElementInstance element, InfoRowStruct row) {

        fillValueRow(element, row);
        bindChild(element, PropertiesSetting.ELEMENT_DECREASE_BUTTON, row.getPath(), true);
        bindChild(element, PropertiesSetting.ELEMENT_INCREASE_BUTTON, row.getPath(), true);

        ElementInstance track = element.findChildById(PropertiesSetting.ELEMENT_TRACK);

        if (track == null)
            return;

        track.setOnDragArgOverride(row.getPath());
        SliderUtility.placeKnob(
                track,
                PropertiesSetting.ELEMENT_KNOB,
                SliderUtility.resolveStep(row.hasRange() ? row.getFraction() : 0.5f));
    }

    private void fillChoiceRow(ElementInstance element, InfoRowStruct row) {

        fillValueRow(element, row);
        bindChild(element, PropertiesSetting.ELEMENT_DECREASE_BUTTON, row.getPath(), true);
        bindChild(element, PropertiesSetting.ELEMENT_INCREASE_BUTTON, row.getPath(), true);
    }

    private void fillGroupRow(ElementInstance element, InfoRowStruct row) {

        element.setActionArgOverride(row.getPath());
        fillKeyLabel(element, row);
        bindChild(element, PropertiesSetting.ELEMENT_ADD_BUTTON, row.getPath(), row.isAddable());
        bindChild(element, PropertiesSetting.ELEMENT_DUPLICATE_BUTTON, row.getPath(), row.isDuplicable());
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

    // Sliders \\

    // A number's slider dragged: a ranged one is set where it lies, any other moves by the steps the drag spans
    public void dragNumber(String path, WindowInstance window, ElementInstance track) {

        int index = findRow(path);

        if (index == EngineSetting.INDEX_NOT_FOUND)
            return;

        float fraction = SliderUtility.resolveFraction(inputManager.getHoverMouseX(window), track);

        if (rows.get(index).hasRange()) {
            infoManager.setValueFraction(path, fraction);
            return;
        }

        int span = EditorSetting.INFO_SLIDER_STEPS;
        int steps = Math.round((fraction - 0.5f) * span);
        int moved = steps - sliderSteps.getInt(index);

        if (moved == 0)
            return;

        sliderSteps.set(index, steps);
        SliderUtility.placeKnob(
                track,
                PropertiesSetting.ELEMENT_KNOB,
                SliderUtility.resolveStep(0.5f + steps / (float) span));
        infoManager.stepValue(path, moved);
    }

    private void placeRangedKnob(ElementInstance element, InfoRowStruct row) {

        ElementInstance track = element.findChildById(PropertiesSetting.ELEMENT_TRACK);

        if (track != null)
            SliderUtility.placeKnob(
                    track,
                    PropertiesSetting.ELEMENT_KNOB,
                    SliderUtility.resolveStep(row.getFraction()));
    }

    private int findRow(String path) {

        for (int i = 0; i < rows.size(); i++)
            if (rows.get(i).getPath().equals(path))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
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
