package editor.runtime.characterview.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.util.CharacterViewRowKind;
import editor.runtime.layers.LayersSetting;
import editor.runtime.slider.SliderUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterViewPanelSystem extends SystemPackage {

    /*
     * Fills a character tool's side panels from the rows the tool lists, one
     * injected template per row, with the row's key handed to every button
     * inside it and to the row's own press and drag, so a click or a row
     * carried away names the row it came from. A panel is emptied
     * and refilled only when what it lists changes; while the same rows are
     * listed, only the values that moved are set in place, so a slider being
     * dragged keeps its row. A number row's slider spans a fixed count of
     * its value's steps centred where the value stood when the row was
     * listed: dragging it hands the tool the steps between where its knob
     * was and where it now is, through the context's step target, the path
     * the step buttons take. A ranged row's slider spans the range it
     * declares instead, its knob standing where its value does and following
     * it whenever the value or the range moves. A pair row hands each of its
     * two value buttons its own key, and an eye item its eye the row's key,
     * the eye drawn faint while what it names is hidden. A header pressed folds the rows under it
     * shut, or opens them again, its marker saying which; the panel keeps
     * what it was handed, so a fold lays it out again without asking the
     * tool. Tools refill a panel only when their state has moved, never per
     * frame.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;

    // Target
    private CharacterViewStepTarget stepTarget;

    // Panels
    private ObjectArrayList<CharacterViewPanelStruct> panels;

    // Scratch — the rows a panel shows, those under no folded header
    private ObjectArrayList<CharacterViewRowStruct> shownRows;

    // Colors — the eye of what an eye item hides, as faint as a hidden layer's
    private MenuColorStruct hiddenColor;

    // Base \\

    @Override
    protected void create() {

        // Panels
        this.panels = new ObjectArrayList<>();

        // Scratch
        this.shownRows = new ObjectArrayList<>();

        // Colors
        this.hiddenColor = new MenuColorStruct(ThemeColor.TEXT, LayersSetting.EYE_HIDDEN_ALPHA);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
    }

    @Override
    protected void awake() {

        if (!(context instanceof CharacterViewStepTarget))
            throwException("A character panel's context must take the steps its sliders ask for.");

        this.stepTarget = (CharacterViewStepTarget) context;
    }

    // Panel \\

    public void fill(
            MenuInstance menu,
            int entryPoint,
            ObjectArrayList<CharacterViewRowStruct> rows,
            CharacterViewTemplateStruct templates) {

        CharacterViewPanelStruct panel = resolvePanel(menu, entryPoint);

        panel.keep(rows);
        layout(panel, templates);
    }

    // A panel emptied and forgotten, as when another view takes its entry point over
    public void clear(MenuInstance menu, int entryPoint) {

        menuManager.ejectAll(menu, entryPoint);
        resolvePanel(menu, entryPoint).forget();
    }

    // The rows under no folded header listed, set in place while they are the rows shown, injected afresh otherwise
    private void layout(CharacterViewPanelStruct panel, CharacterViewTemplateStruct templates) {

        resolveShownRows(panel);

        if (panel.lists(shownRows, templates)) {
            refreshValues(panel, shownRows);
            return;
        }

        menuManager.ejectAll(panel.getMenu(), panel.getEntryPoint());
        panel.clear(templates);

        for (int i = 0; i < shownRows.size(); i++) {
            CharacterViewRowStruct row = shownRows.get(i);
            panel.add(row, injectRow(panel, row, templates));
            placeRangedKnob(panel, i);
        }
    }

    private void resolveShownRows(CharacterViewPanelStruct panel) {

        ObjectArrayList<CharacterViewRowStruct> listed = panel.getListed();
        boolean folded = false;

        shownRows.clear();

        for (int i = 0; i < listed.size(); i++) {

            CharacterViewRowStruct row = listed.get(i);

            if (row.getKind() == CharacterViewRowKind.HEADER)
                folded = panel.isFolded(row.getLabel());
            else if (folded)
                continue;

            shownRows.add(row);
        }
    }

    // Fold \\

    // A header pressed: the rows under it folded shut or opened again, in the panel that shows it
    public void toggleHeader(String headerLabel, ElementInstance element) {

        for (int i = 0; i < panels.size(); i++) {

            CharacterViewPanelStruct panel = panels.get(i);

            if (panel.holds(element)) {
                panel.toggleFolded(headerLabel);
                layout(panel, panel.getTemplates());
                return;
            }
        }
    }

    private CharacterViewPanelStruct resolvePanel(MenuInstance menu, int entryPoint) {

        for (int i = 0; i < panels.size(); i++)
            if (panels.get(i).getMenu() == menu && panels.get(i).getEntryPoint() == entryPoint)
                return panels.get(i);

        CharacterViewPanelStruct panel = new CharacterViewPanelStruct(menu, entryPoint);
        panels.add(panel);

        return panel;
    }

    private ElementInstance injectRow(
            CharacterViewPanelStruct panel,
            CharacterViewRowStruct row,
            CharacterViewTemplateStruct templates) {

        boolean header = row.getKind() == CharacterViewRowKind.HEADER;

        return menuManager.inject(panel.getMenu(), panel.getEntryPoint(), resolveTemplate(row, templates), element -> {

            element.setActionArgOverride(header ? row.getLabel() : row.getKey());
            element.setOnDragArgOverride(header ? row.getLabel() : row.getKey());
            setText(element, templates.getLabelElement(), row.getLabel());
            setText(element, templates.getValueElement(), row.getValue());
            setKey(element, templates.getValueButtonElement(), row.getKey());
            setKey(element, templates.getDecreaseElement(), row.getKey());
            setKey(element, templates.getIncreaseElement(), row.getKey());
            setText(element, templates.getPairLabelElement(), row.getPairLabel());
            setText(element, templates.getPairValueElement(), row.getPairValue());
            setKey(element, templates.getPairValueButtonElement(), row.getPairKey());
            setKey(element, templates.getEyeElement(), row.getKey());

            ElementInstance eyeIcon = element.findChildById(templates.getEyeIconElement());

            if (eyeIcon != null && !row.isShown())
                eyeIcon.setColorOverride(hiddenColor);

            ElementInstance track = element.findChildById(templates.getTrackElement());

            if (track != null)
                track.setOnDragArgOverride(row.getKey());

            if (header)
                setText(element, EditorSetting.CHARACTER_VIEW_FOLD_ELEMENT, panel.isFolded(row.getLabel())
                        ? EngineSetting.HIERARCHY_COLLAPSED_MARKER
                        : EngineSetting.HIERARCHY_EXPANDED_MARKER);
        });
    }

    // Every value that moved set on its row, the rows themselves kept
    private void refreshValues(CharacterViewPanelStruct panel, ObjectArrayList<CharacterViewRowStruct> rows) {

        String valueElement = panel.getTemplates().getValueElement();
        String pairValueElement = panel.getTemplates().getPairValueElement();

        for (int i = 0; i < rows.size(); i++) {

            CharacterViewRowStruct row = rows.get(i);

            if (!row.getValue().equals(panel.getRow(i).getValue()))
                setText(panel.getElement(i), valueElement, row.getValue());

            if (!row.getPairValue().equals(panel.getRow(i).getPairValue()))
                setText(panel.getElement(i), pairValueElement, row.getPairValue());

            panel.setRow(i, row);
            placeRangedKnob(panel, i);
        }
    }

    private String resolveTemplate(CharacterViewRowStruct row, CharacterViewTemplateStruct templates) {
        return switch (row.getKind()) {
            case HEADER -> templates.getHeaderTemplate();
            case ITEM -> row.isSelected() ? templates.getSelectedItemTemplate() : templates.getItemTemplate();
            case EYE_ITEM -> resolveEyeItemTemplate(row, templates);
            case NUMBER -> templates.getNumberTemplate();
            case TEXT -> resolveTextTemplate(templates);
            case CHOICE -> templates.getChoiceTemplate();
            case ACTION -> templates.getActionTemplate();
            case PAIR -> resolvePairTemplate(templates);
        };
    }

    private String resolveTextTemplate(CharacterViewTemplateStruct templates) {

        if (templates.getTextTemplate() == null)
            throwException("A character tool listed a text row but its menu declares no text row template.");

        return templates.getTextTemplate();
    }

    private String resolveEyeItemTemplate(CharacterViewRowStruct row, CharacterViewTemplateStruct templates) {

        if (templates.getEyeItemTemplate() == null)
            throwException("A character tool listed an eye item row but its menu declares no eye item row template.");

        return row.isSelected() ? templates.getSelectedEyeItemTemplate() : templates.getEyeItemTemplate();
    }

    private String resolvePairTemplate(CharacterViewTemplateStruct templates) {

        if (templates.getPairTemplate() == null)
            throwException("A character tool listed a pair row but its menu declares no pair row template.");

        return templates.getPairTemplate();
    }

    // Slider \\

    // The value stepped to where the pointer stands along its row's slider, the knob following
    public void dragNumber(String key, WindowInstance window, ElementInstance track) {

        for (int i = 0; i < panels.size(); i++) {

            CharacterViewPanelStruct panel = panels.get(i);
            int index = findTrackRow(panel, track);

            if (index != EngineSetting.INDEX_NOT_FOUND) {
                slide(panel, index, key, SliderUtility.resolveFraction(inputManager.getHoverMouseX(window), track));
                return;
            }
        }
    }

    private int findTrackRow(CharacterViewPanelStruct panel, ElementInstance track) {

        String trackElement = panel.getTemplates() != null ? panel.getTemplates().getTrackElement() : null;

        for (int i = 0; i < panel.getRowCount(); i++)
            if (panel.getRow(i).getKind() == CharacterViewRowKind.NUMBER
                    && panel.getElement(i).findChildById(trackElement) == track)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private void slide(CharacterViewPanelStruct panel, int index, String key, float fraction) {

        if (panel.getRow(index).hasRange()) {
            slideRanged(panel, index, key, fraction);
            return;
        }

        int span = EditorSetting.CHARACTER_VIEW_SLIDER_STEPS;
        int steps = Math.round((fraction - 0.5f) * span);
        int moved = steps - panel.getSliderSteps(index);

        if (moved == 0)
            return;

        panel.setSliderSteps(index, steps);
        SliderUtility.placeKnob(
                panel.getElement(index).findChildById(panel.getTemplates().getTrackElement()),
                panel.getTemplates().getKnobElement(),
                SliderUtility.resolveStep(0.5f + steps / (float) span));
        stepTarget.stepRow(key, moved);
    }

    // A ranged value stepped to where the pointer stands across its range
    private void slideRanged(CharacterViewPanelStruct panel, int index, String key, float fraction) {

        CharacterViewRowStruct row = panel.getRow(index);
        int steps = row.toRangeSteps(fraction);
        int moved = steps - panel.getSliderSteps(index);

        if (moved == 0)
            return;

        panel.setSliderSteps(index, steps);
        SliderUtility.placeKnob(
                panel.getElement(index).findChildById(panel.getTemplates().getTrackElement()),
                panel.getTemplates().getKnobElement(),
                SliderUtility.resolveStep(row.toRangeFraction(steps)));
        stepTarget.stepRow(key, moved);
    }

    // A ranged row's knob set where its value stands, and the steps it stands at kept for the next drag
    private void placeRangedKnob(CharacterViewPanelStruct panel, int index) {

        CharacterViewRowStruct row = panel.getRow(index);

        if (!row.hasRange())
            return;

        ElementInstance track = panel.getElement(index).findChildById(panel.getTemplates().getTrackElement());

        panel.setSliderSteps(index, row.getAmountSteps());

        if (track != null)
            SliderUtility.placeKnob(track, panel.getTemplates().getKnobElement(), SliderUtility.resolveStep(
                    row.getFraction()));
    }

    // Utility \\

    private void setText(ElementInstance row, String elementID, String text) {

        ElementInstance element = row.findChildById(elementID);

        if (element != null)
            element.setFontText(text);
    }

    private void setKey(ElementInstance row, String elementID, String key) {

        ElementInstance element = row.findChildById(elementID);

        if (element != null)
            element.setActionArgOverride(key);
    }
}
