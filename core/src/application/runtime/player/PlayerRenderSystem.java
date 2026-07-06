package application.runtime.player;

import application.bootstrap.entitypipeline.animation.AnimationStateHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.runtime.world.WorldSystem;
import engine.assets.camera.CameraInstance;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class PlayerRenderSystem extends SystemPackage {

    /*
     * Pushes this window's own player character into the shared skinned
     * instance buffers each frame, targeting THIS window's own world FBO —
     * the same live, per-window-cloned FboInstance WorldSystem created and
     * LightingSystem/SSAOSystem already read from, which is the one that
     * actually gets composited to the screen.
     *
     * Model matrix applies a yaw-only rotation sourced from this window's
     * camera direction so the body turns to face wherever the player looks.
     * Pitch is intentionally excluded — a body shouldn't tip forward/back
     * just because the camera looks up or down.
     *
     * First-person head hiding: when PlayerManager reports this window is at
     * (or below) the first-person zoom threshold, the head bone's index is
     * written into the shared character material's "u_hiddenBone" uniform
     * every frame before the draw call is pushed. Skinned.vsh collapses any
     * vertex whose dominant bone matches that index to a single clip-space
     * point, producing a zero-area, unrasterized triangle — the head simply
     * doesn't draw. Passing -1 (any negative) disables hiding entirely.
     *
     * This material is currently shared by every instance of the Humanoid
     * template (EntityData clones one MaterialInstance per template at
     * bootstrap, not per spawned entity) — fine while there is exactly one
     * player. If NPCs or additional players ever share this template, this
     * uniform would need to move to per-instance material state instead of
     * living on the shared template material.
     *
     * There is no shadow-casting pass anywhere in this engine yet, so "hide
     * the head but keep its shadow" simplifies for now to just hiding it in
     * this one color draw call. Whenever a shadow pass exists, giving that
     * pass its own draw call that always sets u_hiddenBone back to -1 before
     * it submits is all that's needed to keep the head casting a shadow while
     * staying invisible in the color pass.
     */

    // Internal
    private PlayerManager playerManager;
    private RenderManager renderManager;
    private WorldSystem worldSystem;

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
        this.renderManager = get(RenderManager.class);
        this.worldSystem = get(WorldSystem.class);
    }

    // Render \\

    @Override
    protected void render() {

        int windowID = context.getWindow().getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        if (!player.hasAnimationState())
            return;

        CameraInstance camera = playerManager.getCameraForWindow(windowID);

        if (camera == null)
            return;

        AnimationStateHandle animationState = player.getAnimationStateHandle();
        Vector3 position = player.getWorldPositionStruct().getPosition();
        Vector3 direction = camera.getDirection();

        float yaw = (float) Math.atan2(direction.x, direction.z);
        float cosYaw = (float) Math.cos(yaw);
        float sinYaw = (float) Math.sin(yaw);

        modelMatrixScratch.set(
                cosYaw, 0, sinYaw, position.x,
                0, 1, 0, position.y,
                -sinYaw, 0, cosYaw, position.z,
                0, 0, 0, 1);

        MaterialInstance material = player.getEntityData().getCharacterMaterial();
        applyHeadVisibility(windowID, player, material);

        FboInstance worldFbo = worldSystem.getWorldFbo();

        renderManager.pushSkinnedCall(
                player.getEntityData().getCharacterMesh(),
                material,
                modelMatrixScratch,
                animationState.getSkinningMatrices(),
                worldFbo,
                context.getWindow());
    }

    private void applyHeadVisibility(int windowID, EntityInstance player, MaterialInstance material) {

        boolean firstPerson = playerManager.isFirstPerson(windowID);

        float hiddenBone = firstPerson
                ? (float) player.getEntityData().getRigHandle().getBoneIndex("head")
                : -1f;

        material.setUniform("u_hiddenBone", hiddenBone);
    }
}