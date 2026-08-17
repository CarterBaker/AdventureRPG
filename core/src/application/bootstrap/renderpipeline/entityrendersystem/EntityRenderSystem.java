package application.bootstrap.renderpipeline.entityrendersystem;

import application.bootstrap.entitypipeline.animation.AnimationStateHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.geometrypipeline.rig.RigMathUtility;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class EntityRenderSystem extends SystemPackage {

    /*
     * Shared, engine-side skinned character rendering used by every
     * window/context to submit an animated entity's model for the frame.
     * Resets the shared skinned instance buffers once per frame during the
     * global RENDER pass, before any window/context pushes its own
     * characters, then exposes pushCharacter() to build the per-entity
     * model matrix — scaled via entity.getModelScale(), the entity's actual
     * size divided by its character mesh's authored model dimensions and
     * cached on EntityInstance, so every rendered character matches its
     * real size regardless of how the source mesh was modeled — and
     * forwards the draw to RenderManager.pushSkinnedCall(). Runtime code
     * never touches SkinnedBufferManager or RenderManager's skinned entry
     * points directly, only ever calling pushCharacter().
     */

    // Internal
    private RenderManager renderManager;

    // Scratch — reused every pushCharacter() call, never reallocated
    private Vector3 positionScratch;
    private Vector3 rotationScratch;
    private Matrix4 modelMatrixScratch;
    private Matrix4 matrixScratchA;
    private Matrix4 matrixScratchB;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.positionScratch = new Vector3();
        this.rotationScratch = new Vector3();
        this.modelMatrixScratch = new Matrix4();
        this.matrixScratchA = new Matrix4();
        this.matrixScratchB = new Matrix4();
    }

    @Override
    protected void get() {
        this.renderManager = get(RenderManager.class);
    }

    @Override
    protected void render() {
        renderManager.clearSkinnedBuffers();
    }

    // Character Push \\

    public void pushCharacter(
            EntityInstance entity,
            Vector3 viewDirection,
            String hiddenBoneName,
            FboInstance targetFbo,
            WindowInstance window) {

        if (!entity.hasAnimationState())
            return;

        AnimationStateHandle animationState = entity.getAnimationStateHandle();
        Vector3 position = entity.getWorldPositionStruct().getPosition();
        Vector3 size = entity.getSize();
        Vector3 modelScale = entity.getModelScale();

        float yawRadians = (float) Math.atan2(viewDirection.x, viewDirection.z);

        positionScratch.set(
                position.x + size.x * 0.5f,
                position.y,
                position.z + size.z * 0.5f);
        rotationScratch.set(0f, (float) Math.toDegrees(yawRadians), 0f);

        RigMathUtility.composeLocal(
                positionScratch, rotationScratch, modelScale,
                modelMatrixScratch, matrixScratchA, matrixScratchB);

        MaterialInstance material = entity.getEntityData().getCharacterMaterial();
        applyHiddenBone(entity, material, hiddenBoneName);

        renderManager.pushSkinnedCall(
                entity.getEntityData().getCharacterMesh(),
                material,
                modelMatrixScratch,
                animationState.getSkinningMatrices(),
                targetFbo,
                window);
    }

    // Hidden Bone \\

    private void applyHiddenBone(EntityInstance entity, MaterialInstance material, String hiddenBoneName) {

        float hiddenBone = hiddenBoneName != null
                ? (float) entity.getEntityData().getRigHandle().getBoneIndex(hiddenBoneName)
                : -1f;

        material.setUniform("u_hiddenBone", hiddenBone);
    }
}