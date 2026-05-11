package editor.bootstrap.tab;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class TabContext extends ContextPackage {

    /*
     * Shell context for one editor tab. Opens the tab shell menu on its logical
     * window, tracks the shell canvas bounds each frame, and routes the mounted
     * child context window into that canvas by updating its composite rect.
     */

    // Shell
    private MenuInstance shellMenu;

    // Content
    private WindowInstance contentWindow;
    private ContextPackage contentContext;

    // Internal
    private MenuManager menuManager;

    // Internal \\

    @Override
    protected void create() {
        createMenuTargetFboSystemIfPresent();
    }

    @Override
    protected void get() {

        // Internal
        this.menuManager = get(MenuManager.class);
    }

    @Override
    protected void start() {

        // Shell
        this.shellMenu = menuManager.openMenu(EngineSetting.MENU_TAB_SHELL, getWindow());
    }

    @Override
    protected void update() {

        if (contentWindow == null)
            return;

        int[] bounds = menuManager.getCanvas(shellMenu);

        if (bounds == null)
            return;

        if (bounds.length != EngineSetting.TAB_CANVAS_BOUNDS_LENGTH)
            throwException("Tab shell canvas bounds were invalid.");

        contentWindow.setCompositeRect(
                bounds[EngineSetting.TAB_CANVAS_X_INDEX],
                bounds[EngineSetting.TAB_CANVAS_Y_INDEX],
                bounds[EngineSetting.TAB_CANVAS_W_INDEX],
                bounds[EngineSetting.TAB_CANVAS_H_INDEX]);
    }

    @Override
    protected void dispose() {

        unmountContent();

        if (shellMenu != null)
            this.shellMenu = menuManager.closeMenu(shellMenu);
    }

    private void createMenuTargetFboSystemIfPresent() {

        try {
            Class<?> targetClass = Class.forName(EngineSetting.TAB_MENU_TARGET_FBO_SYSTEM_CLASS);

            if (!SystemPackage.class.isAssignableFrom(targetClass))
                throwException("Tab menu target FBO class does not extend SystemPackage.");

            create(targetClass.asSubclass(SystemPackage.class));
        } catch (ClassNotFoundException e) {
            return;
        }
    }

    // Management \\

    public void mountContent(WindowInstance contentWindow, ContextPackage contentContext) {

        if (contentWindow == null)
            throwException("Cannot mount tab content without a content window.");

        if (contentContext == null)
            throwException("Cannot mount tab content without a content context.");

        // Content
        this.contentWindow = contentWindow;
        this.contentContext = contentContext;
    }

    public void unmountContent() {

        if (contentWindow != null)
            contentWindow.clearCompositeRect();

        // Content
        this.contentWindow = null;
        this.contentContext = null;
    }

    // Accessible \\

    public WindowInstance getContentWindow() {
        return contentWindow;
    }

    public ContextPackage getContentContext() {
        return contentContext;
    }

    public MenuInstance getShellMenu() {
        return shellMenu;
    }
}
