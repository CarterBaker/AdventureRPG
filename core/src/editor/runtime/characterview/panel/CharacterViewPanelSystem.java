package editor.runtime.characterview.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.util.CharacterViewRowKind;
import editor.runtime.slider.SliderUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterViewPanelSystem extends SystemPackage {

    /*
     * Fills a character tool's side panels from the rows the tool lists, one
     * injected template per row, with the row's key handed to every button
     * inside it so a click names the row it came from. A panel is emptied
     * and refilled only when what it lists changes; while the same rows are
     * listed, only the values that moved are set in place, so a slider being
     * dragged keeps its row. A number row's slider spans a fixed count of
     * its value's steps centred where the value stood when the row was
     * listed: dragging it hands the tool the steps between where its knob
     * was and where it now is, through the context's step target, the path
     * the step buttons take. Tools refill a panel only when their state has
     * moved, never per frame.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;

    // Target
    private CharacterViewStepTarget stepTarget;

    // Panels
    private ObjectArrayList<CharacterViewPanelStruct> panels;

    // Base \\

    @Override
    protected void create() {

        // Panels
        this.panels = new ObjectArrayList<>();
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

        if (panel.lists(rows, templates)) {
            refreshValues(panel, rows);
            return;
        }

        menuManager.ejectAll(menu, entryPoint);
        panel.clear(templates);

        for (int i = 0; i < rows.size(); i++)
            panel.add(rows.get(i), injectRow(menu, entryPoint, rows.get(i), templates));
    }

    // A panel emptied and forgotten, as when another view takes its entry point over
    public void clear(MenuInstance menu, int entryPoint) {

        menuManager.ejectAll(menu, entryPoint);
        resolvePanel(menu, entryPoint).clear(null);
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
            MenuInstance menu,
            int entryPoint,
            CharacterViewRowStruct row,
            CharacterViewTemplateStruct templates) {

        return menuManager.inject(menu, entryPoint, resolveTemplate(row, templates), element -> {

            element.setActionArgOverride(row.getKey());
            setText(element, templates.getLabelElement(), row.getLabel());
            setText(element, templates.getValueElement(), row.getValue());
            setKey(element, templates.getValueButtonElement(), row.getKey());
            setKey(element, templates.getDecreaseElement(), row.getKey());
            setKey(element, templates.getIncreaseElement(), row.getKey());

            ElementInstance track = element.findChildById(templates.getTrackElement());

            if (track != null)
                track.setOnDragArgOverride(row.getKey());
        });
    }

    // Every value that moved set on its row, the rows themselves kept
    private void refreshValues(CharacterViewPanelStruct panel, ObjectArrayList<CharacterViewRowStruct> rows) {

        String valueElement = panel.getTemplates().getValueElement();

        for (int i = 0; i < rows.size(); i++) {

            CharacterViewRowStruct row = rows.get(i);

            if (!row.getValue().equals(panel.getRow(i).getValue()))
                setText(panel.getElement(i), valueElement, row.getValue());

            panel.setRow(i, row);
        }
    }

    private String resolveTemplate(CharacterViewRowStruct row, CharacterViewTemplateStruct templates) {
        return switch (row.getKind()) {
            case HEADER -> templates.getHeaderTemplate();
            case ITEM -> row.isSelected() ? templates.getSelectedItemTemplate() : templates.getItemTemplate();
            case NUMBER -> templates.getNumberTemplate();
            case TEXT -> resolveTextTemplate(templates);
            case CHOICE -> templates.getChoiceTemplate();
            case ACTION -> templates.getActionTemplate();
        };
    }

    private String resolveTextTemplate(CharacterViewTemplateStruct templates) {

        if (templates.getTextTemplate() == null)
            throwException("A character tool listed a text row but its menu declares no text row template.");

        return templates.getTextTemplate();
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
