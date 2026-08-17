package application.bootstrap.renderpipeline.entityrendersystem;

import application.bootstrap.entitypipeline.animation.AnimationStateHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
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
     * Owns every piece of shared, engine-side skinned CHARACTER rendering —
     * the one place any window/context, for any animated entity (player or
     * NPC alike), submits a character model for this frame. Two
     * responsibilities:
     *
     * 1. Global, once-per-frame reset for the shared skinned instance
     * buffers. Runs during the RENDER phase's global pass — before any
     * window/context gets to push its own characters into those buffers
     * (contexts render after global systems each frame, per
     * EnginePackage.internalRender()) — so every buffer starts this frame
     * empty exactly once, no matter how many windows are open.
     *
     * 2. pushCharacter() — builds the model matrix from the entity's own
     * world position, facing direction, and size, then forwards the draw
     * to RenderManager.pushSkinnedCall(). Scale is never entity.getSize()
     * directly — the mesh is authored at its own raw width/height/length
     * (see MeshHandle/MeshData), so the per-axis scale is the entity's
     * actual size divided by that raw extent, stretching whatever the mesh
     * was modeled at onto the entity's real bounding box with no
     * distortion. Position is centered on the entity's own footprint
     * (worldPosition + size.x/2, size.z/2 on the horizontal plane) — the
     * exact same centering PlayerManager already uses to place the
     * camera's eye position — so the rendered body and the point the
     * camera orbits/aims from always agree; worldPosition itself is the
     * entity's bounding-box min corner, never its center. Runtime code
     * never builds this matrix, and never touches SkinnedBufferManager or
     * RenderManager's skinned entry points directly — it only ever calls
     * pushCharacter().
     */

    // Internal
    private RenderManager renderManager;

    // Scratch — reused every pushCharacter() call, never reallocated
    private Vector3 positionScratch;
    private Vector3 rotationScratch;
    private Vector3 scaleScratch;
    private Matrix4 modelMatrixScratch;
    private Matrix4 matrixScratchA;
    private Matrix4 matrixScratchB;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.positionScratch = new Vector3();
        this.rotationScratch = new Vector3();
        this.scaleScratch = new Vector3();
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

    /*
     * Submits one animated entity's character model for rendering this
     * frame, targeting the given FBO/window. No-ops for any entity with no
     * character model (entity.hasAnimationState() == false) — safe to call
     * unconditionally for any EntityInstance, player or NPC.
     *
     * viewDirection drives yaw-only facing — pitch is intentionally
     * excluded, a body shouldn't tip forward/back just because whatever
     * camera is looking at it points up or down.
     *
     * hiddenBoneName, when non-null, resolves that bone against this
     * entity's own rig and zeroes its vertices via the shared Skinned
     * shader's u_hiddenBone uniform (see Skinned.vsh) — used by first-person
     * view to hide the head without a second mesh or draw call. Pass null
     * to render every bone normally.
     *
     * The character material is shared per EntityData template (see
     * EntityData's own doc comment) — setting u_hiddenBone here affects
     * every instance of that template sharing the material. Fine while a
     * template has at most one rendered instance at a time; a future
     * multi-instance template (several NPCs, or split-screen players)
     * sharing one template would need this uniform moved to per-instance
     * material state instead.
     */
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
        MeshHandle characterMesh = entity.getEntityData().getCharacterMesh();

        float yawRadians = (float) Math.atan2(viewDirection.x, viewDirection.z);

        // Center the footprint on the entity's own bounding box, exactly like
        // PlayerManager centers its eye position — position is the box's
        // min corner, not its center.
        positionScratch.set(
                position.x + size.x * 0.5f,
                position.y,
                position.z + size.z * 0.5f);
        rotationScratch.set(0f, (float) Math.toDegrees(yawRadians), 0f);

        // The mesh's own raw model-space extent is never assumed to be 1:1 —
        // dividing the entity's actual size by it gives the per-axis ratio
        // that stretches the authored mesh onto this entity's real bounding
        // box with no distortion.
        scaleScratch.set(
                size.x / characterMesh.getWidth(),
                size.y / characterMesh.getHeight(),
                size.z / characterMesh.getLength());

        RigMathUtility.composeLocal(
                positionScratch, rotationScratch, scaleScratch,
                modelMatrixScratch, matrixScratchA, matrixScratchB);

        MaterialInstance material = entity.getEntityData().getCharacterMaterial();
        applyHiddenBone(entity, material, hiddenBoneName);

        renderManager.pushSkinnedCall(
                characterMesh,
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