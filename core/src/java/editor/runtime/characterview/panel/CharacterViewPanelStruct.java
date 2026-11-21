package editor.runtime.characterview.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class CharacterViewPanelStruct extends StructPackage {

    /*
     * One side panel as it is shown: the menu and entry point it fills, the
     * templates it was filled with, every row it was last handed and the
     * headers folded shut, the rows it lists (those under no folded header),
     * the element injected for each, and for each the steps its slider has
     * moved its value since the row was listed. Owned by
     * CharacterViewPanelSystem.
     */

    // Target
    private final MenuInstance menu;
    private final int entryPoint;
    private CharacterViewTemplateStruct templates;

    // Listed — every row handed in, and the labels of the headers folded shut
    private final ObjectArrayList<CharacterViewRowStruct> listed;
    private final ObjectOpenHashSet<String> foldedHeaders;

    // Rows
    private final ObjectArrayList<CharacterViewRowStruct> rows;
    private final ObjectArrayList<ElementInstance> elements;
    private final IntArrayList sliderSteps;

    // Constructor \\

    public CharacterViewPanelStruct(MenuInstance menu, int entryPoint) {

        // Target
        this.menu = menu;
        this.entryPoint = entryPoint;

        // Listed
        this.listed = new ObjectArrayList<>();
        this.foldedHeaders = new ObjectOpenHashSet<>();

        // Rows
        this.rows = new ObjectArrayList<>();
        this.elements = new ObjectArrayList<>();
        this.sliderSteps = new IntArrayList();
    }

    // Listed \\

    public void keep(ObjectArrayList<CharacterViewRowStruct> handed) {
        listed.clear();
        listed.addAll(handed);
    }

    public ObjectArrayList<CharacterViewRowStruct> getListed() {
        return listed;
    }

    public boolean isFolded(String headerLabel) {
        return foldedHeaders.contains(headerLabel);
    }

    public void toggleFolded(String headerLabel) {
        if (!foldedHeaders.remove(headerLabel))
            foldedHeaders.add(headerLabel);
    }

    // Rows \\

    // True when a list holds the same rows as shown, by kind, keys, labels and selection, whatever their values
    public boolean lists(ObjectArrayList<CharacterViewRowStruct> listed, CharacterViewTemplateStruct listedTemplates) {

        if (listedTemplates != templates || listed.size() != rows.size())
            return false;

        for (int i = 0; i < rows.size(); i++) {

            CharacterViewRowStruct shown = rows.get(i);
            CharacterViewRowStruct row = listed.get(i);

            if (shown.getKind() != row.getKind()
                    || shown.isSelected() != row.isSelected()
                    || shown.isShown() != row.isShown()
                    || !shown.getKey().equals(row.getKey())
                    || !shown.getLabel().equals(row.getLabel())
                    || !shown.getPairKey().equals(row.getPairKey())
                    || !shown.getPairLabel().equals(row.getPairLabel()))
                return false;
        }

        return true;
    }

    // Every row and element let go of, as when another view takes the entry point over; the folds are kept
    public void forget() {
        clear(null);
        listed.clear();
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

    public boolean holds(ElementInstance element) {
        return elements.contains(element);
    }

    public int getSliderSteps(int index) {
        return sliderSteps.getInt(index);
    }
}
