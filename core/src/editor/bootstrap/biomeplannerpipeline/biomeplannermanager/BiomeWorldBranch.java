package editor.bootstrap.biomeplannerpipeline.biomeplannermanager;

import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;

class BiomeWorldBranch extends BranchPackage {

    /*
     * Owns the planner's world: a small image, one square of pixels per grid
     * cell, made a virtual world of WorldManager's the first time it is asked
     * for, so it generates and streams as any world does. Painting a cell
     * writes its biome's color over its square, and a world already made is
     * restreamed through the World Editor, which regenerates its map and
     * every preview flying over it, but only when a pixel actually changed.
     */

    // Internal
    private WorldManager worldManager;
    private WorldEditorManager worldEditorManager;

    // World
    private Pixmap world;
    private WorldHandle worldHandle;

    // Base \\

    @Override
    protected void create() {

        int side = EditorSetting.BIOME_PLANNER_GRID_SIDE * EditorSetting.BIOME_PLANNER_CELL_PIXELS;

        // World
        this.world = new Pixmap(side, side, Pixmap.Format.RGBA8888);
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    // Paint \\

    // Every cell's square in its image color, cells laid row by row from the top left
    void paint(int[] cellColors) {

        boolean changed = false;

        for (int cell = 0; cell < cellColors.length; cell++)
            changed |= paintCell(cell, cellColors[cell]);

        if (changed && worldHandle != null)
            worldEditorManager.restreamWorld(worldHandle);
    }

    private boolean paintCell(int cell, int color) {

        int pixels = EditorSetting.BIOME_PLANNER_CELL_PIXELS;
        int left = cell % EditorSetting.BIOME_PLANNER_GRID_SIDE * pixels;
        int top = cell / EditorSetting.BIOME_PLANNER_GRID_SIDE * pixels;

        if (world.getPixel(left, top) == color)
            return false;

        for (int y = top; y < top + pixels; y++)
            for (int x = left; x < left + pixels; x++)
                world.drawPixel(x, y, color);

        return true;
    }

    // Accessible \\

    WorldHandle getWorldHandle() {

        if (worldHandle == null)
            this.worldHandle = worldManager.createVirtualWorld(EditorSetting.BIOME_PLANNER_WORLD_NAME, world);

        return worldHandle;
    }
}
