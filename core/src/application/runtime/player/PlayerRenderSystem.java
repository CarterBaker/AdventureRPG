package application.runtime.player;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.renderpipeline.entityrendersystem.EntityRenderSystem;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.runtime.world.WorldSystem;
import engine.assets.camera.CameraInstance;
import engine.root.SystemPackage;

public class PlayerRenderSystem extends SystemPackage {

    /*
     * Runtime-thin: resolves which entity, camera, and hidden bone apply to
     * this window's player, then hands off to the shared engine-side
     * EntityRenderSystem.pushCharacter() — the single place model-matrix
     * construction (position, facing, and entity-size scale) actually
     * happens, identical for the player and any NPC. No matrix math and no
     * SkinnedBufferManager/RenderManager skinned entry points live here
     * anymore — see EntityRenderSystem for that. Mirrors how
     * WorldItemPlacementSystem/WorldItemRenderSystem keep all composite-item
     * logic in bootstrap and runtime only ever calls the entry points.
     *
     * "head" is hardcoded as the bone hidden in first person — every
     * humanoid rig this engine loads is expected to name its head bone
     * exactly that (see Humanoid.json).
     */

    private static final String HEAD_BONE_NAME = "head";

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

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        CameraInstance camera = playerManager.getCameraForWindow(windowID);

        if (camera == null)
            return;

        String hiddenBone = playerManager.isFirstPerson(windowID) ? HEAD_BONE_NAME : null;
        FboInstance worldFbo = worldSystem.getWorldFbo();

        entityRenderSystem.pushCharacter(
                player,
                camera.getDirection(),
                hiddenBone,
                worldFbo,
                context.getWindow());
    }
}