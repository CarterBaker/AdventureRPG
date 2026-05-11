// core/src/editor/bootstrap/tab/TabContext.java
package editor.bootstrap.tab;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.MenuTargetFboSystem;
import engine.root.ContextPackage;
import engine.root.EngineSetting;

public class TabContext extends ContextPackage {

    private MenuManager menuManager;
    private MenuInstance tabShellMenu;
    private WindowInstance contentWindow;
    private ContextPackage contentContext;

    @Override
    protected void create() {
        create(MenuTargetFboSystem.class);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    @Override
    protected void start() {
        this.tabShellMenu = menuManager.openMenu(EngineSetting.MENU_TAB_SHELL, getWindow());
    }

    @Override
    protected void update() {
        if (contentWindow == null)
            return;

        int[] bounds = getCanvasBounds();
        if (bounds == null)
            return;

        contentWindow.setCompositeRect(
                (float) bounds[0],
                (float) bounds[1],
                (float) bounds[2],
                (float) bounds[3]);
    }

    public void mountContent(WindowInstance contentWindow, ContextPackage contentContext) {
        this.contentWindow = contentWindow;
        this.contentContext = contentContext;
    }

    public int[] getCanvasBounds() {
        return menuManager.getCanvas(tabShellMenu);
    }

    public void unmountContent() {
        if (contentWindow != null)
            contentWindow.clearCompositeRect();

        this.contentWindow = null;
        this.contentContext = null;
    }
}
