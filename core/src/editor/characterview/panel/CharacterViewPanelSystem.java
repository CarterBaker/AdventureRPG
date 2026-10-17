package editor.characterview.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterViewPanelSystem extends SystemPackage {

    /*
     * Fills a character tool's side panel from the rows the tool lists. The
     * panel is emptied and refilled as a whole, one injected template per
     * row, with the row's key handed to every button inside it so a click
     * names the row it came from. Tools refill a panel only when their state
     * has moved, never per frame.
     */

    // Internal
    private MenuManager menuManager;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    // Panel \\

    public void fill(
            MenuInstance menu,
            int entryPoint,
            ObjectArrayList<CharacterViewRowStruct> rows,
            CharacterViewTemplateStruct templates) {

        menuManager.ejectAll(menu, entryPoint);

        for (int i = 0; i < rows.size(); i++)
            injectRow(menu, entryPoint, rows.get(i), templates);
    }

    private void injectRow(
            MenuInstance menu,
            int entryPoint,
            CharacterViewRowStruct row,
            CharacterViewTemplateStruct templates) {

        menuManager.inject(menu, entryPoint, resolveTemplate(row, templates), element -> {

            element.setActionArgOverride(row.getKey());
            setText(element, templates.getLabelElement(), row.getLabel());
            setText(element, templates.getValueElement(), row.getValue());
            setKey(element, templates.getValueButtonElement(), row.getKey());
            setKey(element, templates.getDecreaseElement(), row.getKey());
            setKey(element, templates.getIncreaseElement(), row.getKey());
        });
    }

    private String resolveTemplate(CharacterViewRowStruct row, CharacterViewTemplateStruct templates) {
        return switch (row.getKind()) {
            case HEADER -> templates.getHeaderTemplate();
            case ITEM -> row.isSelected() ? templates.getSelectedItemTemplate() : templates.getItemTemplate();
            case NUMBER -> templates.getNumberTemplate();
            case CHOICE -> templates.getChoiceTemplate();
            case ACTION -> templates.getActionTemplate();
        };
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
