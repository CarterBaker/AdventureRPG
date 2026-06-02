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
     * openSaveDialog() — opens the modal save dialog in its own logical window
     * at toolbar depth so it composites above tabs.
     * update() — polls keyboard input each frame while a dialog is open,
     * appending typed characters to nameBuffer and pushing the result to the
     * name display label via setFontText(). Backspace trims, Enter confirms.
     * confirmCreate(MenuInstance) — saves nameBuffer content if non-empty, closes.
     * cancelCreate(MenuInstance) — closes without saving; called by the toolbar
     * close button (arg: $parent).
     * confirmSave(MenuInstance) — saves nameBuffer content if non-empty, closes.
     * cancelSave(MenuInstance) — closes without saving.
     * loadLayout(String) — loads a named layout via LayoutManager.
     *
     * nameBuffer is shared between the two dialogs. They are mutually exclusive
     * — openCreateLayoutDialog guards on createDialog != null and openSaveDialog
     * guards on saveDialog != null — so only one can hold the buffer at a time.
     *
     * Key constants are raw GLFW values — the same integers the engine backend
     * populates into EngineContext.input. No new binding type is needed since
     * the dialogs are editor-only and not remappable.
     */
    // Menu paths
    private static final String SAVE_LAYOUT_DIALOG = "editor/EditorWindow/SaveLayoutDialog";
    private static final String CREATE_LAYOUT_DIALOG = "editor/EditorWindow/CreateLayoutDialog";
    private static final String LAYOUT_ITEM_TEMPLATE = "editor/EditorWindow/layout_item_template";
    private static final String LAYOUT_ITEM_LABEL_ID = "layout_item_label";
    // Entry point indices — Toolbar menu
    private static final int ENTRY_TESTING_DROPDOWN = 0;
    private static final int ENTRY_LAYOUTS_DROPDOWN = 1;
    private static final int ENTRY_LAYOUTS_LIST = 2;
    // Entry point indices — SaveLayoutDialog menu
    private static final int ENTRY_SAVE_NAME_LABEL = 0;
    // Entry point indices — CreateLayoutDialog menu
    private static final int ENTRY_CREATE_NAME_LABEL = 0;
    // GLFW key codes
    private static final int KEY_BACKSPACE = 259;
    private static final int KEY_ENTER = 257;
    private static final int KEY_ENTER_NUMPAD = 335;
    private static final int KEY_ESCAPE = 256;
    private static final int KEY_SPACE = 32;
    private static final int KEY_MINUS = 45;
    private static final int KEY_A = 65;
    private static final int KEY_Z = 90;
    private static final int KEY_0 = 48;
    private static final int KEY_9 = 57;
    private static final int KEY_LEFT_SHIFT = 340;
    private static final int KEY_RIGHT_SHIFT = 344;
    // Internal
    private MenuManager menuManager;
    private WindowManager windowManager;
    private TabManager tabManager;
    private LayoutManager layoutManager;
    private FboManager fboManager;
    // State
    private MenuInstance saveDialog;
    private MenuInstance createDialog;
    private WindowInstance saveDialogWindow;
    private WindowInstance createDialogWindow;
    private FboInstance saveDialogFbo;
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
        if (saveDialog != null)
            updateNameInput(saveDialog, false);
        if (createDialog != null)
            updateNameInput(createDialog, true);
    }

    // Tab Operations \\
    public void toggleTestingDropdown(MenuInstance parent) {
        ElementInstance dropdown = parent.getEntryPoint(ENTRY_TESTING_DROPDOWN);
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
        ElementInstance dropdown = parent.getEntryPoint(ENTRY_LAYOUTS_DROPDOWN);
        if (dropdown == null)
            return;
        if (!dropdown.isExpanded()) {
            for (int i = 0; i < injectedLayoutItems.size(); i++)
                menuManager.eject(parent, ENTRY_LAYOUTS_LIST, injectedLayoutItems.get(i));
            injectedLayoutItems.clear();
            ObjectArrayList<String> layouts = layoutManager.listLayouts();
            for (int i = 0; i < layouts.size(); i++) {
                String name = layouts.get(i);
                ElementInstance item = menuManager.inject(
                        parent, ENTRY_LAYOUTS_LIST, LAYOUT_ITEM_TEMPLATE,
                        el -> {
                            el.setActionArgOverride(name);
                            ElementInstance label = el.findChildById(LAYOUT_ITEM_LABEL_ID);
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
        createDialogWindow = windowManager.createLogicalWindow("CreateLayoutDialog", windowManager.getMainWindow());
        createDialogWindow.setDepth(EditorSetting.TOOLBAR_WINDOW_DEPTH);
        createDialogWindow.setCaptureEligible(false);
        createDialogWindow.setFocusIndependent(true);
        int w = windowManager.getMainWindow().getWidth();
        int h = windowManager.getMainWindow().getHeight();
        createDialogWindow.setCompositeRect(0, 0, w, h);
        createDialogWindow.resize(w, h);
        createDialogFbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, createDialogWindow);
        menuManager.setMenuTargetFbo(createDialogWindow, createDialogFbo);
        createDialog = menuManager.openMenu(CREATE_LAYOUT_DIALOG, createDialogWindow);
        refreshNameLabel(createDialog, ENTRY_CREATE_NAME_LABEL);
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

    // Save Layout Dialog \\
    /*
     * Opens the modal save dialog in its own logical window cloned from the
     * UI FBO at toolbar depth so it composites above tabs. Guards against
     * opening a second instance if one is already active. Clears the name
     * buffer so the dialog always starts empty.
     */
    public void openSaveDialog() {
        if (saveDialog != null)
            return;
        nameBuffer.setLength(0);
        saveDialogWindow = windowManager.createLogicalWindow("SaveLayoutDialog", windowManager.getMainWindow());
        saveDialogWindow.setDepth(EditorSetting.TOOLBAR_WINDOW_DEPTH);
        createDialogWindow.setCaptureEligible(false);
        createDialogWindow.setFocusIndependent(true);
        int w = windowManager.getMainWindow().getWidth();
        int h = windowManager.getMainWindow().getHeight();
        saveDialogWindow.setCompositeRect(0, 0, w, h);
        saveDialogWindow.resize(w, h);
        saveDialogFbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, saveDialogWindow);
        menuManager.setMenuTargetFbo(saveDialogWindow, saveDialogFbo);
        saveDialog = menuManager.openMenu(SAVE_LAYOUT_DIALOG, saveDialogWindow);
        refreshNameLabel(saveDialog, ENTRY_SAVE_NAME_LABEL);
    }

    /*
     * Saves the current nameBuffer content if non-empty then closes. Called by
     * the confirm button on_click and by Enter key in updateNameInput().
     */
    public void confirmSave(MenuInstance parent) {
        String name = nameBuffer.toString().trim();
        if (name.isEmpty())
            return;
        layoutManager.saveLayout(name);
        closeSaveDialog(parent);
    }

    public void cancelSave(MenuInstance parent) {
        closeSaveDialog(parent);
    }

    private void closeSaveDialog(MenuInstance parent) {
        menuManager.closeMenu(parent);
        menuManager.setMenuTargetFbo(saveDialogWindow, null);
        windowManager.removeWindow(saveDialogWindow);
        saveDialogWindow = null;
        saveDialogFbo = null;
        saveDialog = null;
        nameBuffer.setLength(0);
    }

    // Name Input \\
    /*
     * Polls EngineContext.input each frame while a dialog is open.
     * Appends printable characters to nameBuffer and calls refreshNameLabel()
     * so the display label reflects the current buffer on the next render frame.
     * Backspace trims the last character. Enter confirms. Escape cancels.
     * Space is stored as underscore to keep layout file names path-safe.
     * isCreate distinguishes which dialog to confirm or cancel.
     */
    private void updateNameInput(MenuInstance dialog, boolean isCreate) {
        int nameLabel = isCreate ? ENTRY_CREATE_NAME_LABEL : ENTRY_SAVE_NAME_LABEL;
        if (EngineContext.input.isKeyClicked(KEY_ESCAPE)) {
            if (isCreate)
                closeCreateDialog(dialog);
            else
                closeSaveDialog(dialog);
            return;
        }
        if (EngineContext.input.isKeyClicked(KEY_ENTER)
                || EngineContext.input.isKeyClicked(KEY_ENTER_NUMPAD)) {
            if (isCreate)
                confirmCreate(dialog);
            else
                confirmSave(dialog);
            return;
        }
        if (EngineContext.input.isKeyClicked(KEY_BACKSPACE)) {
            if (nameBuffer.length() > 0)
                nameBuffer.deleteCharAt(nameBuffer.length() - 1);
            refreshNameLabel(dialog, nameLabel);
            return;
        }
        if (EngineContext.input.isKeyClicked(KEY_SPACE)) {
            nameBuffer.append('_');
            refreshNameLabel(dialog, nameLabel);
            return;
        }
        if (EngineContext.input.isKeyClicked(KEY_MINUS)) {
            nameBuffer.append('-');
            refreshNameLabel(dialog, nameLabel);
            return;
        }
        boolean shift = EngineContext.input.isKeyDown(KEY_LEFT_SHIFT)
                || EngineContext.input.isKeyDown(KEY_RIGHT_SHIFT);
        for (int key = KEY_A; key <= KEY_Z; key++) {
            if (EngineContext.input.isKeyClicked(key)) {
                nameBuffer.append(shift ? (char) key : (char) (key + 32));
                refreshNameLabel(dialog, nameLabel);
                return;
            }
        }
        for (int key = KEY_0; key <= KEY_9; key++) {
            if (EngineContext.input.isKeyClicked(key)) {
                nameBuffer.append((char) key);
                refreshNameLabel(dialog, nameLabel);
                return;
            }
        }
    }

    private void refreshNameLabel(MenuInstance dialog, int entryPoint) {
        if (dialog == null)
            return;
        ElementInstance label = dialog.getEntryPoint(entryPoint);
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