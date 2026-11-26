package editor.runtime.menueventsmanager.menus.search;

import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menu.MenuNodeStruct;
import application.bootstrap.menupipeline.menumanager.ElementHitSystem;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.kernel.inputpipeline.inputmanager.InputManager;
import editor.runtime.EditorSetting;
import engine.input.Buttons;
import engine.input.Input;
import engine.input.InputListener;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorSearchBranch extends BranchPackage implements InputListener {

    /*
     * The search box centred in the main window's toolbar, between the menus
     * and Layouts, as an IDE's search everywhere: it finds every item of every
     * toolbar menu by its name, its menu or the section it stands under, read
     * from the menus' own data the first time it is used, so a menu item added
     * is found with nothing more to write. A click focuses it; typing lists
     * the matches below it, those whose names open with the query first, Up
     * and Down move the one marked, Enter or a click runs it through the menu
     * item's own action, and Escape or a click elsewhere puts the box down.
     * While it is focused the shortcuts hold off, as they do for every dialog
     * that takes typing.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private ElementHitSystem elementHitSystem;

    // Index — every menu item the toolbar's menus hold, and the section the walk stands in
    private ObjectArrayList<EditorSearchEntryStruct> entries;
    private String walkSection;

    // Field
    private SearchFieldStruct searchField;
    private MenuInstance toolbarMenu;
    private Input textInput;

    // Results — the entries listed, in order, the one marked and the panel showing them
    private IntArrayList shownEntries;
    private int markedResult;
    private ElementInstance resultsPanel;
    private ObjectArrayList<ElementInstance> resultRows;

    // Colors
    private MenuColorStruct markedColor;

    // Base \\

    @Override
    protected void create() {

        // Index
        this.entries = new ObjectArrayList<>();

        // Field
        this.searchField = new SearchFieldStruct(EditorSetting.SEARCH_EDITOR_PLACEHOLDER);

        // Results
        this.shownEntries = new IntArrayList();
        this.resultRows = new ObjectArrayList<>();

        // Colors
        this.markedColor = new MenuColorStruct(ThemeColor.CONTROL_HOVER, EngineSetting.MENU_THEME_ALPHA_DEFAULT);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.elementHitSystem = get(ElementHitSystem.class);
    }

    @Override
    protected void update() {

        if (!searchField.isFocused())
            return;

        if (!toolbarMenu.getWindow().hasCompositeTarget()) {
            blur();
            return;
        }

        Input rawInput = inputManager.getRawInput(toolbarMenu.getWindow());

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)) {
            blur();
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_ENTER) || rawInput.isKeyClicked(EditorSetting.KEY_ENTER_NUMPAD)) {
            runEntry(markedResult);
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_DOWN))
            markResult(markedResult + 1);
        else if (rawInput.isKeyClicked(EditorSetting.KEY_UP))
            markResult(markedResult - 1);

        if (rawInput.isKeyClicked(EditorSetting.KEY_BACKSPACE) && searchField.erase())
            showResults();

        if (rawInput.isMouseClicked(Buttons.LEFT) && !isPointerOverSearch())
            blur();
    }

    // Field \\

    // The toolbar's search box pressed: it takes the typing until it is put down
    public void focus(MenuInstance menu) {

        if (searchField.isFocused())
            return;

        this.toolbarMenu = menu;

        if (entries.isEmpty())
            indexMenus(menu);

        searchField.bind(findElement(menu, EditorSetting.ELEMENT_SEARCH_LABEL));
        searchField.focus();

        this.textInput = inputManager.getRawInput(menu.getWindow());
        textInput.addListener(this);

        showResults();
    }

    private void blur() {

        if (textInput != null) {
            textInput.removeListener(this);
            textInput = null;
        }

        searchField.clear();
        searchField.blur();
        hideResults();
    }

    @Override
    public void onChar(char character) {

        if (searchField.type(character))
            showResults();
    }

    public boolean isFocused() {
        return searchField.isFocused();
    }

    // Whether the search's results stand open over the given toolbar
    public boolean isShowingResults(MenuInstance menu) {
        return resultsPanel != null && menu == toolbarMenu;
    }

    // Index \\

    // Every item of every toolbar menu, read from the dropdown each menu opens and named by the menu's own label
    private void indexMenus(MenuInstance menu) {

        for (String menuElement : EditorSetting.MENU_BAR_MENU_ELEMENTS) {

            ElementInstance barMenu = findElement(menu, menuElement);

            if (barMenu == null)
                continue;

            ElementHandle dropdown = findDropdown(barMenu);

            if (dropdown == null)
                continue;

            this.walkSection = null;
            indexNodes(dropdown.getChildren(), findText(barMenu));
        }
    }

    private ElementHandle findDropdown(ElementInstance element) {

        ElementHandle handle = element.getHandle();

        if (handle.hasHoverEnterState() && handle.getHoverEnterState().hasMaster())
            return handle.getHoverEnterState().getMaster();

        ObjectArrayList<ElementInstance> children = element.getChildren();

        for (int i = 0; i < children.size(); i++) {

            ElementHandle found = findDropdown(children.get(i));

            if (found != null)
                return found;
        }

        return null;
    }

    // A section header names the items below it; an item with an action and a name is an entry; anything else is
    // looked through
    private void indexNodes(ObjectArrayList<MenuNodeStruct> nodes, String menuTitle) {

        for (int i = 0; i < nodes.size(); i++) {

            MenuNodeStruct node = nodes.get(i);
            ElementHandle master = node.getMaster();
            String actionClass = node.getActionClassOverride() != null
                    ? node.getActionClassOverride()
                    : master.getElementData().getActionClass();
            String actionMethod = node.getActionMethodOverride() != null
                    ? node.getActionMethodOverride()
                    : master.getElementData().getActionMethod();

            if (master.getId().endsWith(EditorSetting.SEARCH_SECTION_SUFFIX)) {
                this.walkSection = findText(node);
                continue;
            }

            String label = actionClass != null && actionMethod != null ? findText(node) : null;

            if (label == null) {
                indexNodes(node.getChildren(), menuTitle);
                continue;
            }

            String actionArg = node.getActionArgOverride() != null
                    ? node.getActionArgOverride()
                    : master.getElementData().getActionArg();
            String path = walkSection != null
                    ? menuTitle + EditorSetting.SEARCH_PATH_SEPARATOR + walkSection
                    : menuTitle;

            entries.add(new EditorSearchEntryStruct(label, path, actionClass, actionMethod, actionArg));
        }
    }

    // The first text a node or any node below it shows
    private String findText(MenuNodeStruct node) {

        String text = node.getTextOverride() != null ? node.getTextOverride() : node.getMaster().getText();

        if (text != null && !text.isEmpty())
            return text;

        ObjectArrayList<MenuNodeStruct> children = node.getChildren();

        for (int i = 0; i < children.size(); i++) {

            String found = findText(children.get(i));

            if (found != null)
                return found;
        }

        return null;
    }

    private String findText(ElementInstance element) {

        String text = element.getText();

        if (text != null && !text.isEmpty())
            return text;

        ObjectArrayList<ElementInstance> children = element.getChildren();

        for (int i = 0; i < children.size(); i++) {

            String found = findText(children.get(i));

            if (found != null)
                return found;
        }

        return null;
    }

    // Results \\

    // The matches listed again for the query as it stands: names that open with it, then names holding it, then
    // items whose menu or section holds it
    private void showResults() {

        shownEntries.clear();

        if (!searchField.isEmpty()) {
            collectResults(0);
            collectResults(1);
            collectResults(2);
        }

        this.markedResult = 0;
        layoutResults();
    }

    private void collectResults(int rank) {

        for (int i = 0; i < entries.size() && shownEntries.size() < EditorSetting.SEARCH_RESULTS_MAX; i++) {

            EditorSearchEntryStruct entry = entries.get(i);

            if (rankOf(entry) == rank)
                shownEntries.add(i);
        }
    }

    private int rankOf(EditorSearchEntryStruct entry) {

        if (searchField.opens(entry.getLabel()))
            return 0;

        if (searchField.matches(entry.getLabel()))
            return 1;

        return searchField.matches(entry.getPath()) ? 2 : EngineSetting.INDEX_NOT_FOUND;
    }

    private void layoutResults() {

        hideResults();

        if (shownEntries.isEmpty())
            return;

        ElementInstance anchor = toolbarMenu.getEntryPoint(EditorSetting.ENTRY_SEARCH_RESULTS);

        this.resultsPanel = menuManager.inject(
                toolbarMenu, EditorSetting.ENTRY_SEARCH_RESULTS, EditorSetting.MENU_EDITOR_SEARCH_RESULTS_PANEL);
        resultsPanel.setSizeOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(
                        Math.max(anchor.getComputedW(), EditorSetting.SEARCH_RESULTS_MIN_WIDTH_PIXELS)),
                DimensionValueStruct.ofAbsolute(shownEntries.size() * EditorSetting.SEARCH_RESULT_ROW_PIXELS
                        + EditorSetting.SEARCH_RESULTS_PADDING_PIXELS)));

        ElementInstance list = resultsPanel.findChildById(EditorSetting.ELEMENT_SEARCH_RESULTS_LIST);

        for (int i = 0; i < shownEntries.size(); i++)
            resultRows.add(injectResult(list, i, entries.get(shownEntries.getInt(i))));

        markResult(0);
    }

    private ElementInstance injectResult(ElementInstance list, int result, EditorSearchEntryStruct entry) {
        return menuManager.inject(
                list, EditorSetting.MENU_EDITOR_SEARCH_RESULT_TEMPLATE,
                row -> {
                    row.setActionArgOverride(Integer.toString(result));
                    row.findChildById(EditorSetting.ELEMENT_SEARCH_RESULT_LABEL).setFontText(entry.getLabel());
                    row.findChildById(EditorSetting.ELEMENT_SEARCH_RESULT_PATH).setFontText(entry.getPath());
                });
    }

    private void hideResults() {

        resultRows.clear();

        if (resultsPanel == null)
            return;

        menuManager.eject(toolbarMenu, EditorSetting.ENTRY_SEARCH_RESULTS, resultsPanel);
        this.resultsPanel = null;
    }

    private void markResult(int result) {

        if (resultRows.isEmpty())
            return;

        int marked = Math.floorMod(result, resultRows.size());

        if (markedResult < resultRows.size())
            resultRows.get(markedResult).setColorOverride(null);

        this.markedResult = marked;
        resultRows.get(marked).setColorOverride(markedColor);
    }

    // A result clicked
    public void runResult(String result) {
        runEntry(Integer.parseInt(result));
    }

    private void runEntry(int result) {

        if (result < 0 || result >= shownEntries.size())
            return;

        EditorSearchEntryStruct entry = entries.get(shownEntries.getInt(result));
        MenuInstance menu = toolbarMenu;

        blur();
        elementHitSystem.executeAction(
                entry.getActionClass(),
                entry.getActionMethod(),
                entry.getActionArg(),
                menu.getWindow());
    }

    // Utility \\

    private boolean isPointerOverSearch() {

        float x = inputManager.getHoverMouseX(toolbarMenu.getWindow());
        float y = inputManager.getHoverMouseY(toolbarMenu.getWindow());

        return contains(findElement(toolbarMenu, EditorSetting.ELEMENT_SEARCH_MENU), x, y)
                || resultsPanel != null && contains(
                        resultsPanel.findChildById(EditorSetting.ELEMENT_SEARCH_RESULTS_CONTENT), x, y);
    }

    private boolean contains(ElementInstance element, float x, float y) {
        return element != null
                && x >= element.getComputedLeft()
                && x <= element.getComputedLeft() + element.getComputedW()
                && y >= element.getComputedTop()
                && y <= element.getComputedTop() + element.getComputedH();
    }

    private ElementInstance findElement(MenuInstance menu, String id) {

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
