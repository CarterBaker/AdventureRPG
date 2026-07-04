package engine.editor;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import engine.root.SystemPackage;

public class EditorMenuSystem extends SystemPackage {

    /*
     * Opens two menus against two logical windows on the main window's GL context.
     *
     * Base window (zOrder 0, inherited from the main OS window) — background
     * sprite and dock canvas area. This is what EditorTabCompositorSystem reads
     * for dock bounds.
     *
     * Toolbar window — toolbar chrome only, brought to front once at creation
     * so it starts above anything already open. It never needs to be brought
     * to front again: tabs live in the dock canvas region and the toolbar
     * lives in its own strip, so their rects never overlap and their relative
     * zOrder never matters for rendering or hit-testing. Each window gets its
     * own cloned FBO so they write to separate render targets and neither
     * overwrites the other.
     *
     * Toolbar rect is mirrored from the main window dimensions each frame.
     * The OS window has no compositeRect — its source of truth is getWidth()
     * and getHeight(). Fires only when dimensions change.
     *
     * captureEligible(false) + focusIndependent(true) mean the toolbar never
     * pins the cursor and always receives hover-driven input regardless of
     * which window currently owns engine-wide focus.
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

        // Base — background + dock canvas
        FboInstance baseFbo = fboManager.getFbo(RuntimeSetting.FBO_UI);
        menuManager.setMenuTargetFbo(mainWindow, baseFbo);
        baseMenu = menuManager.openMenu(EditorSetting.MENU_EDITOR_BASE, mainWindow);

        // Toolbar — own logical window, own cloned FBO, always receives input
        toolbarWindow = windowManager.createLogicalWindow(
                EditorSetting.WINDOW_TITLE_EDITOR_TOOLBAR, mainWindow);
        toolbarWindow.setCaptureEligible(false);
        toolbarWindow.setFocusIndependent(true);
        windowManager.bringToFront(toolbarWindow);

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