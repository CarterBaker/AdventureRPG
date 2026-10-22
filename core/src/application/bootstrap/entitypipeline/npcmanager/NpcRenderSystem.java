package application.bootstrap.entitypipeline.npcmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.npc.NpcInstance;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.EntityRenderSystem;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class NpcRenderSystem extends SystemPackage {

    /*
     * Draws every NPC near a viewer into a window's world target. Each
     * resident within NPC_RENDER_CHUNK_RADIUS chunks of the viewer's chunk,
     * measured across the world's wrap, is handed to
     * EntityRenderSystem.pushCharacter() and placed relative to that chunk,
     * so NPCs are drawn exactly as the player is. Held residents are not
     * drawn.
     */

    // Internal
    private NpcManager npcManager;
    private EntityRenderSystem entityRenderSystem;

    // Base \\

    @Override
    protected void get() {
        this.npcManager = get(NpcManager.class);
        this.entityRenderSystem = get(EntityRenderSystem.class);
    }

    // Render \\

    public void pushResidents(
            WorldHandle worldHandle,
            long viewerChunkCoordinate,
            FBOInstance targetFbo,
            WindowInstance window) {

        if (npcManager.isHeld())
            return;

        ObjectArrayList<NpcInstance> residents = npcManager.getResidents();

        for (int i = 0; i < residents.size(); i++) {

            EntityInstance entity = residents.get(i).getEntityInstance();

            if (entity.getWorldHandle() != worldHandle || !isWithinReach(entity, viewerChunkCoordinate))
                continue;

            entityRenderSystem.pushCharacter(entity, false, viewerChunkCoordinate, targetFbo, window);
        }
    }

    private boolean isWithinReach(EntityInstance entity, long viewerChunkCoordinate) {

        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                entity.getWorldHandle(),
                viewerChunkCoordinate,
                entity.getWorldPositionStruct().getChunkCoordinate());

        return Math.abs(Coordinate2Long.unpackX(delta)) <= EngineSetting.NPC_RENDER_CHUNK_RADIUS
                && Math.abs(Coordinate2Long.unpackY(delta)) <= EngineSetting.NPC_RENDER_CHUNK_RADIUS;
    }
}
