package application.bootstrap.menupipeline.canvassystem;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class CanvasAreaSystem extends SystemPackage {

    private Object2ObjectOpenHashMap<WindowInstance, Object2ObjectOpenHashMap<String, int[]>> windowAreas;

    @Override
    protected void create() {
        this.windowAreas = new Object2ObjectOpenHashMap<>();
    }

    public void register(WindowInstance window, String id, int x, int y, int w, int h) {
        Object2ObjectOpenHashMap<String, int[]> areas = windowAreas.get(window);

        if (areas != null && !areas.isEmpty())
            throwException("Window already has a canvas area: " + window);

        if (areas == null) {
            areas = new Object2ObjectOpenHashMap<>();
            windowAreas.put(window, areas);
        }

        areas.put(id, new int[] { x, y, w, h });
    }

    public int[] get(WindowInstance window, String id) {
        Object2ObjectOpenHashMap<String, int[]> areas = windowAreas.get(window);
        return areas == null ? null : areas.get(id);
    }

    public int[] get(WindowInstance window) {
        Object2ObjectOpenHashMap<String, int[]> areas = windowAreas.get(window);

        if (areas == null || areas.isEmpty())
            throwException("No canvas area found for window: " + window);

        return areas.values().iterator().next();
    }
}
