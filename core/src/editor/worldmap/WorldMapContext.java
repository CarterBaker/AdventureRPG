package editor.worldmap;

import editor.runtime.EditorInputSystem;
import editor.worldmap.menu.WorldMapMenuSystem;
import editor.worldmap.render.WorldMapRenderSystem;
import editor.worldmap.view.WorldMapViewSystem;
import engine.root.ContextPackage;

public class WorldMapContext extends ContextPackage {

    /*
     * Editor tab showing the whole world from above, like a game's map. It
     * only views and draws: the engine's map streams from a few tiles for the
     * whole world down to single blocks as it zooms, generated on the fly from
     * the terrain, with the real chunks a preview has loaded drawn in once it
     * reaches them. An arrow always marks the character's position and
     * facing, and the view follows it until dragged.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private WorldMapViewSystem worldMapViewSystem;
    private WorldMapRenderSystem worldMapRenderSystem;
    private WorldMapMenuSystem worldMapMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.worldMapViewSystem = create(WorldMapViewSystem.class);
        this.worldMapRenderSystem = create(WorldMapRenderSystem.class);
        this.worldMapMenuSystem = create(WorldMapMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Management \\

    public void toggleFollow() {
        worldMapViewSystem.toggleFollow();
    }
}
