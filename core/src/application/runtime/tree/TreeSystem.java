package application.runtime.tree;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.worldpipeline.treemanager.TreeFallRenderSystem;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;

public class TreeSystem extends SystemPackage {

    /*
     * Draws the falling pieces of felled trees into this window's world
     * target, placed relative to the chunk this window's player stands in —
     * the frame its camera sees the world from, free camera included. Trees
     * still standing are drawn with their chunks; all falling-tree drawing is
     * TreeFallRenderSystem's, and this only resolves the window.
     */

    // Internal
    private PlayerManager playerManager;
    private TreeFallRenderSystem treeFallRenderSystem;
    private WorldSystem worldSystem;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.treeFallRenderSystem = get(TreeFallRenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
    }

    // Render \\

    @Override
    protected void render() {

        int windowID = context.getWindow().getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        treeFallRenderSystem.pushFallingTrees(
                player.getWorldHandle(),
                player.getWorldPositionStruct().getChunkCoordinate(),
                worldSystem.getWorldFbo(),
                context.getWindow());
    }
}
