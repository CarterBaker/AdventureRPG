package application.runtime.player;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.EntityRenderSystem;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;

public class PlayerRenderSystem extends SystemPackage {

    /*
     * Resolves this window's player entity, camera and hidden head bone, and
     * hands them to EntityRenderSystem.pushCharacter(), which does all
     * character rendering. A player flying as a free camera is not drawn.
     */

    // Internal
    private PlayerManager playerManager;
    private EntityRenderSystem entityRenderSystem;
    private WorldSystem worldSystem;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.entityRenderSystem = get(EntityRenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
    }

    // Render \\

    @Override
    protected void render() {

        int windowID = context.getWindow().getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID) || playerManager.isFreeCameraForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        FBOInstance worldFbo = worldSystem.getWorldFbo();

        entityRenderSystem.pushCharacter(
                player,
                playerManager.isFirstPerson(windowID),
                worldFbo,
                context.getWindow());
    }
}