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
     * Menu event handlers for the editor toolbar. One instance serves the
     * toolbar of every editor window: handlers that act on a window take the
     * WindowInstance the click happened in and resolve its OS window, so a
     * preview or dialog always opens in the window that asked for it.
     *
     * Tab/window operations delegate to TabManager with no policy here.
     *
     * Layout operations:
     * refreshLayoutList(MenuInstance) — fired when the layouts dropdown opens;
     * repopulates its list fresh from disk so it is always current.
     * openCreateLayoutDialog(WindowInstance) — opens the create dialog through
     * MenuManager.openMenuWindow() on the invoking window.
     * update() — polls keyboard input each frame while the dialog is open,
     * appending typed characters to nameBuffer and pushing the result to the
     * name display label via setFontText(). Backspace trims, Enter confirms.
     * confirmCreate() — saves nameBuffer content if non-empty, closes.
     * cancelCreate() — closes without saving; called by the dialog close button.
     * loadLayout(String) — loads a named layout via LayoutManager.
     *
     * updateNameInput() polls the dialog window's own raw input via
     * InputManager.getRawInput() — never through the engine-wide
     * EngineContext.input global, which reflects whichever window currently
     * owns focus and is very often not this dialog.
     *
     * Key constants are defined in EditorSetting as raw GLFW values — the same
     * integers the engine backend populates into each window's Input. No new
     * binding type is needed since the dialog is editor-only and not remappable.
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

    public void openSecondaryWindow() {
        tabManager.openSecondaryOsWindow();
    }

    // Layout Dropdown \\

    /*
     * Ejects every item currently in the layouts list, then injects one button
     * per layout found on disk. Each button's label child has its font text set
     * to the layout name and its click arg set to the same name so loadLayout()
     * receives it on click.
     */
    public void refreshLayoutList(MenuInstance menu) {

        ElementInstance list = menu.getEntryPoint(EditorSetting.ENTRY_LAYOUTS_LIST);

        if (list == null)
            return;

        ObjectArrayList<ElementInstance> staleItems = new ObjectArrayList<>(list.getChildren());

        for (int i = 0; i < staleItems.size(); i++)
            menuManager.eject(menu, EditorSetting.ENTRY_LAYOUTS_LIST, staleItems.get(i));

        ObjectArrayList<String> layouts = layoutManager.listLayouts();

        for (int i = 0; i < layouts.size(); i++) {

            String name = layouts.get(i);
            menuManager.inject(
                    menu, EditorSetting.ENTRY_LAYOUTS_LIST, EditorSetting.MENU_EDITOR_LAYOUT_ITEM_TEMPLATE,
                    el -> {
                        el.setActionArgOverride(name);
                        ElementInstance label = el.findChildById(EditorSetting.ELEMENT_LAYOUT_ITEM_LABEL);
                        if (label != null)
                            label.setFontText(name);
                    });
        }
    }

    // Create Layout Dialog \\
    /*
     * Opens the create dialog over the OS window the request came from.
     * Guards against opening a second instance if one is already active.
     * Clears the name buffer so the dialog always starts empty. The dialog
     * closes itself if the OS window it sits on is closed underneath it.
     */

    public void openCreateLayoutDialog(WindowInstance window) {

        if (createDialog != null)
            return;

        nameBuffer.setLength(0);
        createDialog = menuManager.openMenuWindow(EditorSetting.MENU_EDITOR_CREATE_LAYOUT_DIALOG, window.getGLWindow());
        refreshNameLabel();
    }

    /*
     * Saves the current nameBuffer content if non-empty then closes. Called by
     * the OK button on_click and by Enter key in updateNameInput().
     */

    public void confirmCreate() {

        String name = nameBuffer.toString().trim();

        if (name.isEmpty())
            return;

        layoutManager.saveLayout(name);
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

    /*
     * Polls the dialog window's own raw input each frame while the dialog is
     * open. Appends printable characters to nameBuffer and calls
     * refreshNameLabel() so the display label reflects the current buffer on
     * the next render frame. Backspace trims the last character. Enter
     * confirms. Escape cancels. Space is stored as underscore to keep layout
     * file names path-safe.
     */

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
            if (nameBuffer.length() > 0)
                nameBuffer.deleteCharAt(nameBuffer.length() - 1);
            refreshNameLabel();
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_SPACE)) {
            nameBuffer.append('_');
            refreshNameLabel();
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_MINUS)) {
            nameBuffer.append('-');
            refreshNameLabel();
            return;
        }

        boolean shift = rawInput.isKeyDown(EditorSetting.KEY_LEFT_SHIFT)
                || rawInput.isKeyDown(EditorSetting.KEY_RIGHT_SHIFT);

        for (int key = EditorSetting.KEY_A; key <= EditorSetting.KEY_Z; key++)
            if (rawInput.isKeyClicked(key)) {
                nameBuffer.append(shift ? (char) key : (char) (key + 32));
                refreshNameLabel();
                return;
            }

        for (int key = EditorSetting.KEY_0; key <= EditorSetting.KEY_9; key++)
            if (rawInput.isKeyClicked(key)) {
                nameBuffer.append((char) key);
                refreshNameLabel();
                return;
            }
    }

    private void refreshNameLabel() {

        ElementInstance label = createDialog.getEntryPoint(EditorSetting.ENTRY_CREATE_NAME_LABEL);

        if (label != null)
            label.setFontText(nameBuffer.toString());
    }

    // Load \\

    /*
     * Receives the layout name as the click action arg wired by the injected
     * button in refreshLayoutList(). Delegates directly to LayoutManager.
     */

    public void loadLayout(String name) {
        layoutManager.loadLayout(name);
    }
}
