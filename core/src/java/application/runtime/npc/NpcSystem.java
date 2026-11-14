package application.runtime.npc;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.npcmanager.NpcRenderSystem;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;

public class NpcSystem extends SystemPackage {

    /*
     * Draws the NPCs into this window's world target, placed relative to the
     * chunk this window's player stands in — the frame its camera sees the
     * world from, free camera included. All NPC drawing is NpcRenderSystem's;
     * this only resolves the window.
     */

    // Internal
    private PlayerManager playerManager;
    private NpcRenderSystem npcRenderSystem;
    private WorldSystem worldSystem;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.npcRenderSystem = get(NpcRenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
    }

    // Render \\

    @Override
    protected void render() {

        int windowID = context.getWindow().getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        npcRenderSystem.pushResidents(
                player.getWorldHandle(),
                player.getWorldPositionStruct().getChunkCoordinate(),
                worldSystem.getWorldFbo(),
                context.getWindow());
    }
}
