package application.bootstrap.menupipeline.canvassystem;

import application.bootstrap.menupipeline.menu.MenuInstance;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class CanvasAreaSystem extends SystemPackage {

    /*
     * Tracks the computed screen rect for the single canvas_area element each
     * menu is permitted to define. Keyed by MenuInstance — completely independent
     * of window. Updated every frame by MenuRenderSystem; callers retrieve the
     * rect by menu, never by window.
     */

    // Internal
    private Object2ObjectOpenHashMap<MenuInstance, int[]> menuCanvas;

    // Internal \\

    @Override
    protected void create() {
        this.menuCanvas = new Object2ObjectOpenHashMap<>();
    }

    // Management \\

    public void register(MenuInstance menu, int x, int y, int w, int h) {
        menuCanvas.put(menu, new int[] { x, y, w, h });
    }

    public void unregister(MenuInstance menu) {
        menuCanvas.remove(menu);
    }

    // Accessible \\

    public int[] get(MenuInstance menu) {
        return menuCanvas.get(menu);
    }
}