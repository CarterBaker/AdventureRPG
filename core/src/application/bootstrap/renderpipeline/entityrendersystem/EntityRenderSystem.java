package application.bootstrap.renderpipeline.entityrendersystem;

import application.bootstrap.entitypipeline.animation.AnimationStateHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EntityRenderSystem extends SystemPackage {

    /*
     * Gathers every window's live player entity into the shared skinned
     * instance buffers once per frame, during RENDER — after PlayerManager
     * has already advanced that entity's animation state during UPDATE, so
     * the skinning matrices consumed here are always current for this
     * frame. NPC rendering can reuse pushCharacter() the moment NPC AI owns
     * a per-frame update loop of its own — nothing here is player-specific
     * except how the entity list is discovered.
     *
     * Targets EngineSetting.SCENE_FBO_NAME for every window — the fbo name
     * world geometry itself resolves to was not available to verify this
     * against; if your real scene fbo has a different registered name,
     * only that one constant needs to change.
     */

    // Internal
    private PlayerManager playerManager;
    private WindowManager windowManager;
    private RenderManager renderManager;
    private FboManager fboManager;

    // Scratch — reused every render(), never reallocated
    private Matrix4 modelMatrixScratch;

    // Internal \\

    @Override
    protected void create() {
        this.modelMatrixScratch = new Matrix4();
    }

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.windowManager = get(WindowManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FboManager.class);
    }

    // Render \\

    @Override
    protected void render() {

        renderManager.clearSkinnedBuffers();

        ObjectArrayList<WindowInstance> windows = windowManager.getWindows();
        Object[] elements = windows.elements();
        int count = windows.size();

        FboInstance sceneFbo = fboManager.getFbo(EngineSetting.SCENE_FBO_NAME);

        for (int i = 0; i < count; i++) {

            WindowInstance window = (WindowInstance) elements[i];
            int windowID = window.getWindowID();

            if (!playerManager.hasPlayerForWindow(windowID))
                continue;

            EntityInstance player = playerManager.getPlayerForWindow(windowID);

            if (!player.hasAnimationState())
                continue;

            pushCharacter(player, sceneFbo, window);
        }
    }

    private void pushCharacter(EntityInstance entity, FboInstance fbo, WindowInstance window) {

        AnimationStateHandle animationState = entity.getAnimationStateHandle();
        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();

        // Translation-only model matrix — facing-direction rotation and
        // entity-size scaling are both deliberately out of scope here; the
        // rig currently always renders at its authored bind-pose scale,
        // facing its own +Z.
        modelMatrixScratch.set(
                1, 0, 0, position.x,
                0, 1, 0, position.y,
                0, 0, 1, position.z,
                0, 0, 0, 1);

        renderManager.pushSkinnedCall(
                entity.getEntityData().getCharacterMesh(),
                entity.getEntityData().getCharacterMaterial(),
                modelMatrixScratch,
                animationState.getSkinningMatrices(),
                fbo,
                window);
    }
}