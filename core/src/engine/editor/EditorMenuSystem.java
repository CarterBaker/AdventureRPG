package engine.editor;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class EditorMenuSystem extends SystemPackage {

    /*
     * Opens two menus against two logical windows on the main window's GL context.
     *
     * Base window (depth 0) — background sprite and dock canvas area. This is
     * what EditorTabCompositorSystem reads for dock bounds.
     *
     * Toolbar window (depth 3) — toolbar chrome only. Sits above tabs (depth 1)
     * and content (depth 2) so dropdowns and toolbar elements always composite
     * on top regardless of what the tab stack is doing. Each window gets its
     * own cloned FBO so they write to separate render targets and neither
     * overwrites the other.
     *
     * Toolbar rect is mirrored from the main window dimensions each frame.
     * The OS window has no compositeRect — its source of truth is getWidth()
     * and getHeight(). Fires only when dimensions change.
     *
     * Toolbar window is marked alwaysInputActive so InputSystem passes input
     * through regardless of OS focus state.
     */

    // Internal
    private WindowManager windowManager;
    private MenuManager menuManager;
    private FboManager fboManager;

    // Windows
    private WindowInstance mainWindow;
    private WindowInstance toolbarWindow;

    // Menus
    private MenuInstance baseMenu;
    private MenuInstance toolbarMenu;

    // Cached Toolbar Rect
    private int lastW;
    private int lastH;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        windowManager = get(WindowManager.class);
        menuManager = get(MenuManager.class);
        fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {

        mainWindow = windowManager.getMainWindow();

        // Base — background + dock canvas, depth 0
        FboInstance baseFbo = fboManager.getFbo(RuntimeSetting.FBO_UI);
        menuManager.setMenuTargetFbo(mainWindow, baseFbo);
        baseMenu = menuManager.openMenu(EditorSetting.MENU_EDITOR_BASE, mainWindow);

        // Toolbar — own logical window at depth 3, own cloned FBO, always receives
        // input, never eligible for cursor capture or engine-wide focus
        toolbarWindow = windowManager.createLogicalWindow(
                EditorSetting.WINDOW_TITLE_EDITOR_TOOLBAR, mainWindow);
        toolbarWindow.setDepth(EditorSetting.TOOLBAR_WINDOW_DEPTH);
        toolbarWindow.setCaptureEligible(false);
        toolbarWindow.setFocusIndependent(true);

        FboInstance toolbarFbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, toolbarWindow);
        menuManager.setMenuTargetFbo(toolbarWindow, toolbarFbo);
        toolbarMenu = menuManager.openMenu(EditorSetting.MENU_EDITOR_TOOLBAR, toolbarWindow);
    }

    // Update \\

    @Override
    protected void update() {

        int w = mainWindow.getWidth();
        int h = mainWindow.getHeight();

        if (w <= 0 || h <= 0)
            return;

        if (w == lastW && h == lastH)
            return;

        lastW = w;
        lastH = h;

        toolbarWindow.setCompositeRect(0, 0, w, h);
        toolbarWindow.resize(w, h);
    }

    // Accessible \\

    public MenuInstance getBaseMenu() {
        return baseMenu;
    }

    public MenuInstance getToolbarMenu() {
        return toolbarMenu;
    }

    public WindowInstance getToolbarWindow() {
        return toolbarWindow;
    }
}