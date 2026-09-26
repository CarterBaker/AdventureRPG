package application.bootstrap.renderpipeline.entityrendersystem;

import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.inventory.EquipmentAnchorStruct;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.rig.RigMathUtility;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedAppearanceStruct;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

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
     * world position, smoothed body yaw, and height, then forwards one
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
     * entity's bounding-box min corner, never its center. The body is
     * lifted by the entity's cosmetic ground offset so it stands on the
     * jittered surface of natural blocks. Everything that differs between
     * two characters of one template — skin and hair tint,
     * face overlays, the hidden bone — is packed into the per-instance
     * SkinnedAppearanceStruct row, so they still draw instanced together.
     * Runtime code never builds this matrix or that row, and never touches
     * SkinnedBufferManager or RenderManager's skinned entry points directly
     * — it only ever calls pushCharacter().
     *
     * 3. Worn equipment — every shown item in an equipment slot is drawn as
     * a rigid item model at each of that slot's anchors: a worn item's shape
     * stretched to fill the anchor's bind-pose box, a held item at its
     * natural size gripped at the anchor, then carried by the anchor bone's
     * skinning matrix and the character's model matrix, so gear follows the
     * pose exactly like the skin it covers. Gear on the hidden head bone is
     * hidden with it.
     */

    // Internal
    private RenderManager renderManager;
    private MaterialManager materialManager;
    private ItemModelManager itemModelManager;

    // Equipment
    private int equipmentMaterialID;
    private Vector3 unitScale;

    // Scratch — reused every pushCharacter() call, never reallocated
    private Vector3 positionScratch;
    private Vector3 rotationScratch;
    private Vector3 scaleScratch;
    private Matrix4 modelMatrixScratch;
    private Matrix4 matrixScratchA;
    private Matrix4 matrixScratchB;
    private SkinnedAppearanceStruct appearanceScratch;
    private Matrix4 anchorScratch;
    private Matrix4 equipmentScratch;

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
        this.anchorScratch = new Matrix4();
        this.equipmentScratch = new Matrix4();

        // Equipment
        this.unitScale = new Vector3(1f, 1f, 1f);
    }

    @Override
    protected void get() {
        this.renderManager = get(RenderManager.class);
        this.materialManager = get(MaterialManager.class);
        this.itemModelManager = get(ItemModelManager.class);
    }

    @Override
    protected void awake() {
        this.equipmentMaterialID = materialManager.getMaterialIDFromMaterialName(
                EngineSetting.EQUIPMENT_ITEM_MATERIAL);
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
     * Facing is the body's own yaw from EntityStateHandle — yaw only, a
     * body never tips forward/back because a camera points up or down.
     *
     * hideHead collapses every vertex weighted mostly to the head bone the
     * entity's appearance names — used by first-person view to hide the
     * head (and the hair skinned to it) without a second mesh or draw call.
     * It rides in this entity's own appearance row, so hiding one player's
     * head never hides anyone else's. An entity with no appearance has no
     * head bone to hide and always renders every bone.
     */
    public void pushCharacter(
            EntityInstance entity,
            boolean hideHead,
            FboInstance targetFbo,
            WindowInstance window) {

        if (!entity.hasAnimationState())
            return;

        EntityData entityData = entity.getEntityData();
        MaterialInstance material = entityData.getCharacterMaterial();
        Matrix4[] skinningMatrices = entity.getAnimationStateHandle().getSkinningMatrices();

        composeModelMatrix(entity);
        resolveAppearance(entity, hideHead);

        if (!entity.hasAppearance()) {
            pushCharacterPart(entityData.getCharacterMesh(), material, skinningMatrices, targetFbo, window);
            pushEquipment(entity, hideHead, skinningMatrices, targetFbo, window);
            return;
        }

        AppearanceHandle appearance = entity.getAppearanceHandle();

        appearanceScratch.setTint(appearance.getSkinColor());
        pushCharacterPart(entityData.getCharacterMesh(), material, skinningMatrices, targetFbo, window);
        pushCharacterPart(appearance.getHeadMesh(), material, skinningMatrices, targetFbo, window);
        pushFeaturePart(appearance, FeatureSlot.NOSE, material, skinningMatrices, targetFbo, window);

        appearanceScratch.setTint(appearance.getHairColor());
        pushFeaturePart(appearance, FeatureSlot.HAIR, material, skinningMatrices, targetFbo, window);

        pushEquipment(entity, hideHead, skinningMatrices, targetFbo, window);
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

    // Equipment \\

    private void pushEquipment(
            EntityInstance entity,
            boolean hideHead,
            Matrix4[] skinningMatrices,
            FboInstance targetFbo,
            WindowInstance window) {

        ObjectArrayList<EquipmentAnchorStruct> anchors = entity.getEntityData().getEquipmentAnchors();
        InventoryHandle inventory = entity.getInventoryHandle();
        int hiddenBone = hideHead && entity.hasAppearance()
                ? entity.getEntityData().getAppearanceData().getHeadBoneIndex()
                : EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < anchors.size(); i++) {

            EquipmentAnchorStruct anchor = anchors.get(i);

            if (!inventory.isShown(anchor.getEquipmentSlot()) || anchor.getBoneIndex() == hiddenBone)
                continue;

            ItemDefinitionHandle item = inventory.getItem(anchor.getEquipmentSlot()).getItemDefinitionHandle();
            ModelInstance model = itemModelManager.acquireModel(item.getMeshHandle(), equipmentMaterialID);

            composeAnchorMatrix(anchor, item.getShape(), skinningMatrices[anchor.getBoneIndex()]);
            model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, equipmentScratch);

            renderManager.pushRenderCall(model, targetFbo, EngineSetting.EQUIPMENT_RENDER_DEPTH, window);
        }
    }

    // model * skinning * T(anchor) * R(anchor) * S(anchor) * shape placement
    private void composeAnchorMatrix(
            EquipmentAnchorStruct anchor,
            ItemShapeStruct shape,
            Matrix4 skinningMatrix) {

        RigMathUtility.composeLocal(
                anchor.getPosition(), anchor.getRotation(), anchor.isHeld() ? unitScale : anchor.getSize(),
                anchorScratch, matrixScratchA, matrixScratchB);

        if (anchor.isHeld())
            multiplyGripPlacement(anchorScratch, shape);
        else
            multiplyFitPlacement(anchorScratch, shape);

        equipmentScratch
                .set(modelMatrixScratch)
                .multiply(skinningMatrix)
                .multiply(anchorScratch);
    }

    // Moves the grip — the shape's near end, centred across it — onto the anchor, at natural size
    private void multiplyGripPlacement(Matrix4 out, ItemShapeStruct shape) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        out.multiply(
                1, 0, 0, -(shape.getOffsetX() + shape.getSizeX() * 0.5f) / resolution,
                0, 1, 0, -(shape.getOffsetY() + shape.getSizeY() * 0.5f) / resolution,
                0, 0, 1, -shape.getOffsetZ() / resolution,
                0, 0, 0, 1);
    }

    // Maps the shape's bounds onto the unit cube centred on the anchor
    private void multiplyFitPlacement(Matrix4 out, ItemShapeStruct shape) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        float scaleX = resolution / shape.getSizeX();
        float scaleY = resolution / shape.getSizeY();
        float scaleZ = resolution / shape.getSizeZ();

        out.multiply(
                scaleX, 0, 0, -(shape.getOffsetX() + shape.getSizeX() * 0.5f) / shape.getSizeX(),
                0, scaleY, 0, -(shape.getOffsetY() + shape.getSizeY() * 0.5f) / shape.getSizeY(),
                0, 0, scaleZ, -(shape.getOffsetZ() + shape.getSizeZ() * 0.5f) / shape.getSizeZ(),
                0, 0, 0, 1);
    }

    // Model Matrix \\

    private void composeModelMatrix(EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        Vector3 position = entity.getWorldPositionStruct().getPosition();
        Vector3 size = entity.getSize();
        float scale = size.y / entity.getEntityData().getModelHeight();

        // Center the footprint on the entity's own bounding box, exactly like
        // PlayerManager centers its eye position — position is the box's
        // min corner, not its center.
        positionScratch.set(
                position.x + size.x * 0.5f,
                position.y + state.getGroundOffset(),
                position.z + size.z * 0.5f);
        rotationScratch.set(0f, state.getBodyYaw(), 0f);
        scaleScratch.set(scale, scale, scale);

        RigMathUtility.composeLocal(
                positionScratch, rotationScratch, scaleScratch,
                modelMatrixScratch, matrixScratchA, matrixScratchB);
    }

    // Appearance \\

    private void resolveAppearance(EntityInstance entity, boolean hideHead) {

        appearanceScratch.reset();
        appearanceScratch.setHiddenBone(hideHead && entity.hasAppearance()
                ? (float) entity.getEntityData().getAppearanceData().getHeadBoneIndex()
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
