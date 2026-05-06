package editor.bootstrap.dockpipeline.tab;

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
        tabShellMenu = menuManager.openMenu(EngineSetting.MENU_TAB_SHELL, getWindow());
    }

    public void mountContent(WindowInstance contentWin, ContextPackage content) {
        this.contentWindow = contentWin;
        this.contentContext = content;
    }

    public int[] getCanvasBounds() {
        return menuManager.getCanvas(tabShellMenu);
    }

    private void syncCanvasToContent() {
        int[] bounds = getCanvasBounds();
        if (bounds == null)
            return;
        contentWindow.setCompositeRect(bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    @Override
    protected void update() {
        if (contentWindow == null)
            return;
        syncCanvasToContent();
    }
}