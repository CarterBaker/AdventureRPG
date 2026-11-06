package editor.runtime.characterview.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterViewPanelStruct extends StructPackage {

    /*
     * One side panel as it is shown: the menu and entry point it fills, the
     * templates it was filled with, the rows it lists, the element injected
     * for each, and for each the steps its slider has moved its value since
     * the row was listed. Owned by CharacterViewPanelSystem.
     */

    // Target
    private final MenuInstance menu;
    private final int entryPoint;
    private CharacterViewTemplateStruct templates;

    // Rows
    private final ObjectArrayList<CharacterViewRowStruct> rows;
    private final ObjectArrayList<ElementInstance> elements;
    private final IntArrayList sliderSteps;

    // Constructor \\

    public CharacterViewPanelStruct(MenuInstance menu, int entryPoint) {

        // Target
        this.menu = menu;
        this.entryPoint = entryPoint;

        // Rows
        this.rows = new ObjectArrayList<>();
        this.elements = new ObjectArrayList<>();
        this.sliderSteps = new IntArrayList();
    }

    // Rows \\

    // True when a list holds the same rows as shown, by kind, key, label and selection, whatever their values
    public boolean lists(ObjectArrayList<CharacterViewRowStruct> listed, CharacterViewTemplateStruct listedTemplates) {

        if (listedTemplates != templates || listed.size() != rows.size())
            return false;

        for (int i = 0; i < rows.size(); i++) {

            CharacterViewRowStruct shown = rows.get(i);
            CharacterViewRowStruct row = listed.get(i);

            if (shown.getKind() != row.getKind()
                    || shown.isSelected() != row.isSelected()
                    || !shown.getKey().equals(row.getKey())
                    || !shown.getLabel().equals(row.getLabel()))
                return false;
        }

        return true;
    }

    public void clear(CharacterViewTemplateStruct listedTemplates) {

        this.templates = listedTemplates;
        rows.clear();
        elements.clear();
        sliderSteps.clear();
    }

    public void add(CharacterViewRowStruct row, ElementInstance element) {
        rows.add(row);
        elements.add(element);
        sliderSteps.add(0);
    }

    public void setRow(int index, CharacterViewRowStruct row) {
        rows.set(index, row);
    }

    public void setSliderSteps(int index, int steps) {
        sliderSteps.set(index, steps);
    }

    // Accessible \\

    public MenuInstance getMenu() {
        return menu;
    }

    public int getEntryPoint() {
        return entryPoint;
    }

    public CharacterViewTemplateStruct getTemplates() {
        return templates;
    }

    public int getRowCount() {
        return rows.size();
    }

    public CharacterViewRowStruct getRow(int index) {
        return rows.get(index);
    }

    public ElementInstance getElement(int index) {
        return elements.get(index);
    }

    public int getSliderSteps(int index) {
        return sliderSteps.getInt(index);
    }
}
