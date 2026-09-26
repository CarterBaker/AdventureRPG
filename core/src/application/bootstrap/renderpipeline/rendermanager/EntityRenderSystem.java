package application.bootstrap.renderpipeline.rendermanager;

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
import application.bootstrap.renderpipeline.fbo.FBOInstance;
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
     * The single entry point for drawing animated characters. Resets the shared
     * skinned buffers once per frame, then pushCharacter() builds a character's
     * model matrix from its footprint, yaw and size, packs its appearance into
     * one instance row, and submits its body, head parts and worn equipment as
     * skinned and rigid draws. Runtime code never touches the skinned buffers
     * directly.
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

    public void pushCharacter(
            EntityInstance entity,
            boolean hideHead,
            FBOInstance targetFbo,
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
            FBOInstance targetFbo,
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
            FBOInstance targetFbo,
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
            FBOInstance targetFbo,
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
