package engine.editor.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.tabpipeline.layoutmanager.LayoutManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.editor.EditorSetting;
import engine.input.Input;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the editor toolbar. Window operations open tabs
     * in the OS window the click came from. Layout operations list saved
     * layouts when the dropdown opens, load one on click, and run the create
     * dialog, which reads typed keys from the dialog window's own raw input
     * and saves the current arrangement under the entered name.
     */

    // Internal
    private MenuManager menuManager;
    private TabManager tabManager;
    private LayoutManager layoutManager;
    private InputManager inputManager;

    // Create Layout Dialog
    private MenuInstance createDialog;
    private StringBuilder nameBuffer;

    // Base \\

    @Override
    protected void create() {
        this.nameBuffer = new StringBuilder();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.tabManager = get(TabManager.class);
        this.layoutManager = get(LayoutManager.class);
        this.inputManager = get(InputManager.class);
    }

    @Override
    protected void update() {

        if (createDialog == null)
            return;

        if (!createDialog.getWindow().hasCompositeTarget()) {
            closeCreateDialog();
            return;
        }

        updateNameInput();
    }

    // Window Operations \\

    public void openPreview(WindowInstance window) {
        tabManager.openPreview(window.getGLWindow());
    }

    public void openDebug(WindowInstance window) {
        tabManager.openDebug(window.getGLWindow());
    }

    // Layout Dropdown \\

    public void refreshLayoutList(MenuInstance menu) {

        ElementInstance list = menu.getEntryPoint(EditorSetting.ENTRY_LAYOUTS_LIST);

        if (list == null)
            return;

        ObjectArrayList<ElementInstance> staleItems = new ObjectArrayList<>(list.getChildren());

        for (int i = 0; i < staleItems.size(); i++)
            menuManager.eject(menu, EditorSetting.ENTRY_LAYOUTS_LIST, staleItems.get(i));

        ObjectArrayList<String> layoutNames = layoutManager.getLayoutNames();

        for (int i = 0; i < layoutNames.size(); i++)
            injectLayoutItem(menu, layoutNames.get(i));
    }

    private void injectLayoutItem(MenuInstance menu, String layoutName) {
        menuManager.inject(
                menu, EditorSetting.ENTRY_LAYOUTS_LIST, EditorSetting.MENU_EDITOR_LAYOUT_ITEM_TEMPLATE,
                item -> {
                    item.setActionArgOverride(layoutName);
                    ElementInstance label = item.findChildById(EditorSetting.ELEMENT_LAYOUT_ITEM_LABEL);
                    if (label != null)
                        label.setFontText(layoutName);
                });
    }

    public void loadLayout(String layoutName) {
        layoutManager.loadLayout(layoutName);
    }

    // Create Layout Dialog \\

    public void openCreateLayoutDialog(WindowInstance window) {

        if (createDialog != null)
            return;

        nameBuffer.setLength(0);
        createDialog = menuManager.openMenuWindow(EditorSetting.MENU_EDITOR_CREATE_LAYOUT_DIALOG, window.getGLWindow());
        refreshNameLabel();
    }

    public void confirmCreate() {

        String layoutName = nameBuffer.toString();

        if (!layoutManager.isLayoutNameValid(layoutName))
            return;

        layoutManager.saveLayout(layoutName);
        closeCreateDialog();
    }

    public void cancelCreate() {
        closeCreateDialog();
    }

    private void closeCreateDialog() {
        menuManager.closeMenuWindow(createDialog);
        createDialog = null;
        nameBuffer.setLength(0);
    }

    // Name Input \\

    private void updateNameInput() {

        Input rawInput = inputManager.getRawInput(createDialog.getWindow());

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)) {
            closeCreateDialog();
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_ENTER)
                || rawInput.isKeyClicked(EditorSetting.KEY_ENTER_NUMPAD)) {
            confirmCreate();
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_BACKSPACE)) {
            removeNameCharacter();
            return;
        }

        char typed = resolveTypedCharacter(rawInput);

        if (typed != 0)
            appendNameCharacter(typed);
    }

    private char resolveTypedCharacter(Input rawInput) {

        if (rawInput.isKeyClicked(EditorSetting.KEY_SPACE))
            return '_';

        if (rawInput.isKeyClicked(EditorSetting.KEY_MINUS))
            return '-';

        boolean shift = rawInput.isKeyDown(EditorSetting.KEY_LEFT_SHIFT)
                || rawInput.isKeyDown(EditorSetting.KEY_RIGHT_SHIFT);

        for (int key = EditorSetting.KEY_A; key <= EditorSetting.KEY_Z; key++)
            if (rawInput.isKeyClicked(key))
                return shift ? (char) key : Character.toLowerCase((char) key);

        for (int key = EditorSetting.KEY_0; key <= EditorSetting.KEY_9; key++)
            if (rawInput.isKeyClicked(key))
                return (char) key;

        return 0;
    }

    private void appendNameCharacter(char character) {

        if (nameBuffer.length() >= EditorSetting.LAYOUT_NAME_MAX_LENGTH)
            return;

        nameBuffer.append(character);
        refreshNameLabel();
    }

    private void removeNameCharacter() {

        if (nameBuffer.length() == 0)
            return;

        nameBuffer.deleteCharAt(nameBuffer.length() - 1);
        refreshNameLabel();
    }

    private void refreshNameLabel() {

        ElementInstance label = createDialog.getEntryPoint(EditorSetting.ENTRY_CREATE_NAME_LABEL);

        if (label != null)
            label.setFontText(nameBuffer.toString());
    }
}
