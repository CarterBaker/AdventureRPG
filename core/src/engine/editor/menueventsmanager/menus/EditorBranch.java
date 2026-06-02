package engine.editor.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import editor.bootstrap.tabpipeline.layoutmanager.LayoutManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorBranch extends BranchPackage {
    /*
     * Menu event handlers for the main editor menu.
     *
     * Tab/window operations delegate to TabManager with no policy here.
     *
     * Layout operations:
     * toggleLayoutDropdown(MenuInstance) — toggles the layout list and populates
     * it fresh from disk each time it opens so the list is always current.
     * openCreateLayoutDialog() — opens the modal create dialog in its own logical
     * window at toolbar depth so it composites above tabs.
     * update() — polls keyboard input each frame while the dialog is open,
     * appending typed characters to nameBuffer and pushing the result to the
     * name display label via setFontText(). Backspace trims, Enter confirms.
     * confirmCreate(MenuInstance) — saves nameBuffer content if non-empty, closes.
     * cancelCreate(MenuInstance) — closes without saving; called by the toolbar
     * close button (arg: $parent).
     * loadLayout(String) — loads a named layout via LayoutManager.
     *
     * Key constants are defined in EditorSetting as raw GLFW values — the same
     * integers the engine backend populates into EngineContext.input. No new
     * binding type is needed since the dialog is editor-only and not remappable.
     */

    // Internal
    private MenuManager menuManager;
    private WindowManager windowManager;
    private TabManager tabManager;
    private LayoutManager layoutManager;
    private FboManager fboManager;

    // State
    private MenuInstance createDialog;
    private WindowInstance createDialogWindow;
    private FboInstance createDialogFbo;
    private StringBuilder nameBuffer;
    private ObjectArrayList<ElementInstance> injectedLayoutItems;

    // Base \\

    @Override
    protected void create() {
        this.nameBuffer = new StringBuilder();
        this.injectedLayoutItems = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.windowManager = get(WindowManager.class);
        this.tabManager = get(TabManager.class);
        this.layoutManager = get(LayoutManager.class);
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void update() {
        if (createDialog != null)
            updateNameInput(createDialog);
    }

    // Tab Operations \\
    public void toggleTestingDropdown(MenuInstance parent) {

        ElementInstance dropdown = parent.getEntryPoint(EditorSetting.ENTRY_TESTING_DROPDOWN);

        if (dropdown == null)
            return;

        dropdown.toggleExpanded();
    }

    public void openPreview() {
        tabManager.openPreview();
    }

    public void openSecondaryWindow() {
        tabManager.openSecondaryWindow();
    }

    // Layout Dropdown \\

    /*
     * Toggles the layout dropdown. When opening, ejects stale injected items
     * then injects one button per layout found on disk. Each button's label
     * child has its font text set to the layout name and its click arg set to
     * the same name so loadLayout() receives it on click.
     */

    public void toggleLayoutDropdown(MenuInstance parent) {

        ElementInstance dropdown = parent.getEntryPoint(EditorSetting.ENTRY_LAYOUTS_DROPDOWN);

        if (dropdown == null)
            return;

        if (!dropdown.isExpanded()) {

            for (int i = 0; i < injectedLayoutItems.size(); i++)
                menuManager.eject(parent, EditorSetting.ENTRY_LAYOUTS_LIST, injectedLayoutItems.get(i));

            injectedLayoutItems.clear();
            ObjectArrayList<String> layouts = layoutManager.listLayouts();

            for (int i = 0; i < layouts.size(); i++) {

                String name = layouts.get(i);
                ElementInstance item = menuManager.inject(
                        parent, EditorSetting.ENTRY_LAYOUTS_LIST, EditorSetting.MENU_EDITOR_LAYOUT_ITEM_TEMPLATE,
                        el -> {
                            el.setActionArgOverride(name);
                            ElementInstance label = el.findChildById(EditorSetting.ELEMENT_LAYOUT_ITEM_LABEL);
                            if (label != null)
                                label.setFontText(name);
                        });
                injectedLayoutItems.add(item);
            }
        }

        dropdown.toggleExpanded();
    }

    // Create Layout Dialog \\
    /*
     * Opens the modal create dialog in its own logical window cloned from the
     * UI FBO at toolbar depth so it composites above tabs. Guards against
     * opening a second instance if one is already active. Clears the name
     * buffer so the dialog always starts empty.
     */

    public void openCreateLayoutDialog() {

        if (createDialog != null)
            return;

        nameBuffer.setLength(0);
        createDialogWindow = windowManager.createLogicalWindow(
                EditorSetting.WINDOW_TITLE_EDITOR_CREATE_LAYOUT_DIALOG,
                windowManager.getMainWindow());

        createDialogWindow.setDepth(EditorSetting.TOOLBAR_WINDOW_DEPTH);
        createDialogWindow.setCaptureEligible(false);
        createDialogWindow.setFocusIndependent(true);

        int w = windowManager.getMainWindow().getWidth();
        int h = windowManager.getMainWindow().getHeight();

        createDialogWindow.setCompositeRect(0, 0, w, h);
        createDialogWindow.resize(w, h);
        createDialogFbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, createDialogWindow);

        menuManager.setMenuTargetFbo(createDialogWindow, createDialogFbo);
        createDialog = menuManager.openMenu(EditorSetting.MENU_EDITOR_CREATE_LAYOUT_DIALOG, createDialogWindow);
        refreshNameLabel(createDialog);
    }

    /*
     * Saves the current nameBuffer content if non-empty then closes. Called by
     * the OK button on_click and by Enter key in updateNameInput().
     */

    public void confirmCreate(MenuInstance parent) {

        String name = nameBuffer.toString().trim();

        if (name.isEmpty())
            return;

        layoutManager.saveLayout(name);
        closeCreateDialog(parent);
    }

    public void cancelCreate(MenuInstance parent) {
        closeCreateDialog(parent);
    }

    private void closeCreateDialog(MenuInstance parent) {
        menuManager.closeMenu(parent);
        menuManager.setMenuTargetFbo(createDialogWindow, null);
        windowManager.removeWindow(createDialogWindow);
        createDialogWindow = null;
        createDialogFbo = null;
        createDialog = null;
        nameBuffer.setLength(0);
    }

    // Name Input \\

    /*
     * Polls EngineContext.input each frame while the dialog is open.
     * Appends printable characters to nameBuffer and calls refreshNameLabel()
     * so the display label reflects the current buffer on the next render frame.
     * Backspace trims the last character. Enter confirms. Escape cancels.
     * Space is stored as underscore to keep layout file names path-safe.
     */

    private void updateNameInput(MenuInstance dialog) {

        if (EngineContext.input.isKeyClicked(EditorSetting.KEY_ESCAPE)) {
            closeCreateDialog(dialog);
            return;
        }

        if (EngineContext.input.isKeyClicked(EditorSetting.KEY_ENTER)
                || EngineContext.input.isKeyClicked(EditorSetting.KEY_ENTER_NUMPAD)) {
            confirmCreate(dialog);
            return;
        }

        if (EngineContext.input.isKeyClicked(EditorSetting.KEY_BACKSPACE)) {
            if (nameBuffer.length() > 0)
                nameBuffer.deleteCharAt(nameBuffer.length() - 1);
            refreshNameLabel(dialog);
            return;
        }

        if (EngineContext.input.isKeyClicked(EditorSetting.KEY_SPACE)) {
            nameBuffer.append('_');
            refreshNameLabel(dialog);
            return;
        }

        if (EngineContext.input.isKeyClicked(EditorSetting.KEY_MINUS)) {
            nameBuffer.append('-');
            refreshNameLabel(dialog);
            return;
        }

        boolean shift = EngineContext.input.isKeyDown(EditorSetting.KEY_LEFT_SHIFT)
                || EngineContext.input.isKeyDown(EditorSetting.KEY_RIGHT_SHIFT);

        for (int key = EditorSetting.KEY_A; key <= EditorSetting.KEY_Z; key++)
            if (EngineContext.input.isKeyClicked(key)) {
                nameBuffer.append(shift ? (char) key : (char) (key + 32));
                refreshNameLabel(dialog);
                return;
            }

        for (int key = EditorSetting.KEY_0; key <= EditorSetting.KEY_9; key++)
            if (EngineContext.input.isKeyClicked(key)) {
                nameBuffer.append((char) key);
                refreshNameLabel(dialog);
                return;
            }
    }

    private void refreshNameLabel(MenuInstance dialog) {

        if (dialog == null)
            return;

        ElementInstance label = dialog.getEntryPoint(EditorSetting.ENTRY_CREATE_NAME_LABEL);

        if (label != null)
            label.setFontText(nameBuffer.toString());
    }

    // Load \\

    /*
     * Receives the layout name as the click action arg wired by the injected
     * button in toggleLayoutDropdown(). Delegates directly to LayoutManager.
     */

    public void loadLayout(String name) {
        layoutManager.loadLayout(name);
    }
}