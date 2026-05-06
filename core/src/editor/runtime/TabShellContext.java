package editor.runtime;

import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ContextPackage;

public class TabShellContext extends ContextPackage {

    private CanvasBridge canvasBridge;
    private MenuManager menuManager;
    private WindowInstance contentWindow;
    private ContextPackage contentContext;

    @Override
    protected void create() {
        create(MenuTargetFboSystem.class);
        create(CanvasBridge.class);
    }

    @Override
    protected void get() {
        this.canvasBridge = get(CanvasBridge.class);
        this.menuManager = get(MenuManager.class);
    }

    @Override
    protected void start() {
        menuManager.openMenu("editor/TabShell", getWindow());
    }

    public void mountContent(WindowInstance contentWin, ContextPackage content) {
        this.contentWindow = contentWin;
        this.contentContext = content;
    }

    public int[] getCanvasBounds() {
        return canvasBridge.getCanvasBounds();
    }

    private void syncCanvasToContent() {
        int[] bounds = canvasBridge.getCanvasBounds();
        contentWindow.setCompositeRect(bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    @Override
    protected void update() {
        if (contentWindow == null)
            return;
        syncCanvasToContent();
    }
}
