package application.bootstrap.renderpipeline.entityrendersystem;

import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigMathUtility;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedAppearanceStruct;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
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
     * world position, facing direction, and height, then forwards one
     * skinned draw per character part to RenderManager.pushSkinnedCall():
     * the body mesh, and for an entity with an appearance its worn head,
     * nose, and hair meshes too, all sharing one model matrix, one pose, and one
     * material. Scale is uniform — the entity's size.y divided by the
     * model's full authored height (EntityData.getModelHeight()) — so the
     * character keeps its own proportions at any height; width and girth
     * come from the appearance's build, never from stretching the mesh onto
     * the physics box. Position is centered on the entity's own footprint
     * (worldPosition + size.x/2, size.z/2 on the horizontal plane) — the
     * exact same centering PlayerManager already uses to place the
     * camera's eye position — so the rendered body and the point the
     * camera orbits/aims from always agree; worldPosition itself is the
     * entity's bounding-box min corner, never its center. Everything that
     * differs between two characters of one template — skin and hair tint,
     * face overlays, the hidden bone — is packed into the per-instance
     * SkinnedAppearanceStruct row, so they still draw instanced together.
     * Runtime code never builds this matrix or that row, and never touches
     * SkinnedBufferManager or RenderManager's skinned entry points directly
     * — it only ever calls pushCharacter().
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
    private SkinnedAppearanceStruct appearanceScratch;

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
        this.appearanceScratch = new SkinnedAppearanceStruct();
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
     * entity's own rig and collapses every vertex weighted mostly to it —
     * used by first-person view to hide the head (and the hair skinned to
     * it) without a second mesh or draw call. It rides in this entity's own
     * appearance row, so hiding one player's head never hides anyone else's.
     * Pass null to render every bone normally.
     */
    public void pushCharacter(
            EntityInstance entity,
            Vector3 viewDirection,
            String hiddenBoneName,
            FboInstance targetFbo,
            WindowInstance window) {

        if (!entity.hasAnimationState())
            return;

        EntityData entityData = entity.getEntityData();
        MaterialInstance material = entityData.getCharacterMaterial();
        Matrix4[] skinningMatrices = entity.getAnimationStateHandle().getSkinningMatrices();

        composeModelMatrix(entity, viewDirection);
        resolveAppearance(entity, hiddenBoneName);

        if (!entity.hasAppearance()) {
            pushCharacterPart(entityData.getCharacterMesh(), material, skinningMatrices, targetFbo, window);
            return;
        }

        AppearanceHandle appearance = entity.getAppearanceHandle();

        appearanceScratch.setTint(appearance.getSkinColor());
        pushCharacterPart(entityData.getCharacterMesh(), material, skinningMatrices, targetFbo, window);
        pushCharacterPart(appearance.getHeadMesh(), material, skinningMatrices, targetFbo, window);
        pushFeaturePart(appearance, FeatureSlot.NOSE, material, skinningMatrices, targetFbo, window);

        appearanceScratch.setTint(appearance.getHairColor());
        pushFeaturePart(appearance, FeatureSlot.HAIR, material, skinningMatrices, targetFbo, window);
    }

    private void pushFeaturePart(
            AppearanceHandle appearance,
            FeatureSlot featureSlot,
            MaterialInstance material,
            Matrix4[] skinningMatrices,
            FboInstance targetFbo,
            WindowInstance window) {

        if (!appearance.hasFeature(featureSlot))
            return;

        pushCharacterPart(
                appearance.getFeature(featureSlot).getMeshHandle(),
                material,
                skinningMatrices,
                targetFbo,
                window);
    }

    private void pushCharacterPart(
            MeshHandle meshHandle,
            MaterialInstance material,
            Matrix4[] skinningMatrices,
            FboInstance targetFbo,
            WindowInstance window) {

        renderManager.pushSkinnedCall(
                meshHandle,
                material,
                modelMatrixScratch,
                appearanceScratch,
                skinningMatrices,
                targetFbo,
                window);
    }

    // Model Matrix \\

    private void composeModelMatrix(EntityInstance entity, Vector3 viewDirection) {

        Vector3 position = entity.getWorldPositionStruct().getPosition();
        Vector3 size = entity.getSize();
        float yawRadians = (float) Math.atan2(viewDirection.x, viewDirection.z);
        float scale = size.y / entity.getEntityData().getModelHeight();

        // Center the footprint on the entity's own bounding box, exactly like
        // PlayerManager centers its eye position — position is the box's
        // min corner, not its center.
        positionScratch.set(
                position.x + size.x * 0.5f,
                position.y,
                position.z + size.z * 0.5f);
        rotationScratch.set(0f, (float) Math.toDegrees(yawRadians), 0f);
        scaleScratch.set(scale, scale, scale);

        RigMathUtility.composeLocal(
                positionScratch, rotationScratch, scaleScratch,
                modelMatrixScratch, matrixScratchA, matrixScratchB);
    }

    // Appearance \\

    private void resolveAppearance(EntityInstance entity, String hiddenBoneName) {

        appearanceScratch.reset();
        appearanceScratch.setHiddenBone(hiddenBoneName != null
                ? (float) entity.getEntityData().getRigHandle().getBoneIndex(hiddenBoneName)
                : EngineSetting.SKINNED_HIDDEN_BONE_NONE);

        if (!entity.hasAppearance())
            return;

        AppearanceHandle appearance = entity.getAppearanceHandle();

        appearanceScratch.setDetailTint(appearance.getHairColor());
        appearanceScratch.setFaceRegion(appearance.getFaceTexture());
        appearanceScratch.setEyesRegion(appearance.getFeatureTexture(FeatureSlot.EYES));
        appearanceScratch.setBrowsRegion(appearance.getFeatureTexture(FeatureSlot.BROWS));
        appearanceScratch.setMouthRegion(appearance.getFeatureTexture(FeatureSlot.MOUTH));
    }
}
