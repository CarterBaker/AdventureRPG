package editor.runtime;

import application.bootstrap.menupipeline.menumanager.MenuManager;
import engine.root.SystemPackage;

public class CanvasBridge extends SystemPackage {

    private MenuManager menuManager;
    private int[] canvasBounds;

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    public int[] getCanvasBounds() {
        if (canvasBounds == null)
            canvasBounds = menuManager.getCanvas(context.getWindow());
        return canvasBounds;
    }
}
