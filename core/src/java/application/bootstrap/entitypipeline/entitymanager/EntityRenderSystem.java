package application.bootstrap.entitypipeline.entitymanager;

import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.appearance.AppearanceIsolationStruct;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.inventory.EquipmentAnchorStruct;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.entitypipeline.race.RaceGenderStruct;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.rig.RigMathUtility;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedAppearanceStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBoneMaskStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlay;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlayStruct;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.texture.TextureTileSetStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EntityRenderSystem extends SystemPackage {

    /*
     * The single entry point for drawing animated characters. Resets the shared
     * skinned buffers once per frame, then pushCharacter() builds a character's
     * model matrix from its footprint, yaw and size, placed relative to the
     * chunk the viewer stands in across the world's wrap, plays its face,
     * packs its appearance and the overlays its model places into its palette
     * row, and submits its body, worn features and equipment as skinned and
     * rigid draws, the body and its material those of the gender it wears,
     * each feature with its own material when it names one; seen from its own
     * eyes, its head and everything the head carries is masked away.
     * pushPreview() draws a posed character outside any world for editors,
     * as much of it as the editor isolates. Runtime code never touches the
     * skinned buffers directly.
     */

    // Internal
    private RenderManager renderManager;
    private MaterialManager materialManager;
    private ItemModelManager itemModelManager;
    private TextureManager textureManager;

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
    private SkinnedBoneMaskStruct noMask;
    private AppearanceIsolationStruct wholeIsolation;
    private Matrix4 anchorScratch;
    private Matrix4 equipmentScratch;

    // Texture — the tiles a character draws gathered each push, and the screen user every editor preview shares
    private TextureTileSetStruct tileScratch;
    private TextureTileSetStruct previewTiles;
    private int previewUser;
    private int frame;
    private int previewFrame;

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
        this.noMask = new SkinnedBoneMaskStruct();
        this.wholeIsolation = new AppearanceIsolationStruct();
        this.anchorScratch = new Matrix4();
        this.equipmentScratch = new Matrix4();

        // Texture
        this.tileScratch = new TextureTileSetStruct();
        this.previewTiles = new TextureTileSetStruct();
        this.previewUser = EngineSetting.INDEX_NOT_FOUND;

        // Equipment
        this.unitScale = new Vector3(1f, 1f, 1f);
    }

    @Override
    protected void get() {
        this.renderManager = get(RenderManager.class);
        this.materialManager = get(MaterialManager.class);
        this.itemModelManager = get(ItemModelManager.class);
        this.textureManager = get(TextureManager.class);
    }

    @Override
    protected void awake() {
        this.equipmentMaterialID = materialManager.getMaterialIDFromMaterialName(
                EngineSetting.EQUIPMENT_ITEM_MATERIAL);
    }

    @Override
    protected void render() {

        renderManager.clearSkinnedBuffers();
        frame++;

        if (previewUser != EngineSetting.INDEX_NOT_FOUND && previewFrame < frame - 1) {
            textureManager.removeTextureUser(previewUser);
            this.previewUser = EngineSetting.INDEX_NOT_FOUND;
        }
    }

    // Character Push \\

    public void pushCharacter(
            EntityInstance entity,
            boolean hideHead,
            long viewerChunkCoordinate,
            FBOInstance targetFbo,
            WindowInstance window) {

        RaceData raceData = entity.getEntityData().getRaceData();
        RaceGenderStruct raceGender = entity.getRaceGender();
        Matrix4[] skinningMatrices = entity.getAnimationStateHandle().getSkinningMatrices();

        composeModelMatrix(entity, viewerChunkCoordinate);
        refreshTextureUser(entity);
        resolveAppearance(raceGender, entity.getAppearanceHandle());
        pushBody(
                raceGender,
                entity.getAppearanceHandle(),
                raceGender.getMaterial(),
                hideHead && entity.hasAppearance() ? raceData.getAppearanceData().getHeadMask() : noMask,
                wholeIsolation,
                skinningMatrices,
                targetFbo,
                window);
        pushEquipment(entity, hideHead, skinningMatrices, targetFbo, window);
    }

    // A posed character outside any world — an editor's preview — at its own model matrix and without equipment,
    // wearing the model of the gender given and drawn with its own material so it never batches with the
    // characters of a game view, the body and features the editor isolates drawn through its mask
    public void pushPreview(
            RaceGenderStruct raceGender,
            AppearanceHandle appearance,
            Matrix4[] skinningMatrices,
            Matrix4 modelMatrix,
            MaterialInstance material,
            AppearanceIsolationStruct isolation,
            FBOInstance targetFbo,
            WindowInstance window) {

        modelMatrixScratch.set(modelMatrix);
        refreshPreviewTiles(raceGender, appearance);
        resolveAppearance(raceGender, appearance);
        pushBody(
                raceGender,
                appearance,
                material,
                isolation.getBoneMask(),
                isolation,
                skinningMatrices,
                targetFbo,
                window);
    }

    // The body the gender wears through the mask, then every feature its appearance wears that the isolation
    // shows, through the mask where it masks that feature, each with its own material when it names one and in the
    // variant the appearance picks
    private void pushBody(
            RaceGenderStruct raceGender,
            AppearanceHandle appearance,
            MaterialInstance material,
            SkinnedBoneMaskStruct boneMask,
            AppearanceIsolationStruct isolation,
            Matrix4[] skinningMatrices,
            FBOInstance targetFbo,
            WindowInstance window) {

        appearanceScratch.setBoneMask(boneMask);
        appearanceScratch.setVariant(null, EngineSetting.INDEX_NOT_FOUND);

        if (isolation.isBodyShown())
            pushCharacterPart(raceGender.getMeshHandle(), material, skinningMatrices, targetFbo, window);

        if (appearance == null)
            return;

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            if (!appearance.hasFeature(featureSlot) || !isolation.isFeatureShown(featureSlot))
                continue;

            FeatureHandle feature = appearance.getFeature(featureSlot);

            appearanceScratch.setBoneMask(isolation.isFeatureMasked(featureSlot) ? boneMask : noMask);
            appearanceScratch.setVariant(feature.getVariants(), appearance.getFeatureVariant(featureSlot));
            pushCharacterPart(
                    feature.getMeshHandle(),
                    feature.hasMaterial() ? feature.getMaterial() : material,
                    skinningMatrices,
                    targetFbo,
                    window);
        }
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

        ObjectArrayList<EquipmentAnchorStruct> anchors = entity.getEntityData().getRaceData().getEquipmentAnchors();
        InventoryHandle inventory = entity.getInventoryHandle();
        int hiddenBone = hideHead && entity.hasAppearance()
                ? entity.getEntityData().getRaceData().getAppearanceData().getHeadBoneIndex()
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

    // Moves the grip — the shape's near end, centred across it — onto the anchor, at natural size up to one block,
    // so a shape larger than a block is carried shrunk evenly to fit one
    private void multiplyGripPlacement(Matrix4 out, ItemShapeStruct shape) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        float largest = Math.max(shape.getSizeX(), Math.max(shape.getSizeY(), shape.getSizeZ()));
        float scale = Math.min(1f, resolution / largest) / resolution;

        out.multiply(
                scale * resolution, 0, 0, -(shape.getOffsetX() + shape.getSizeX() * 0.5f) * scale,
                0, scale * resolution, 0, -(shape.getOffsetY() + shape.getSizeY() * 0.5f) * scale,
                0, 0, scale * resolution, -shape.getOffsetZ() * scale,
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

    private void composeModelMatrix(EntityInstance entity, long viewerChunkCoordinate) {

        EntityStateHandle state = entity.getEntityStateHandle();
        Vector3 position = entity.getWorldPositionStruct().getPosition();
        Vector3 size = entity.getSize();
        float scale = size.y / entity.getEntityData().getRaceData().getModelHeight();
        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                entity.getWorldHandle(),
                viewerChunkCoordinate,
                entity.getWorldPositionStruct().getChunkCoordinate());

        // Center the footprint on the entity's own bounding box, exactly like
        // PlayerManager centers its eye position — position is the box's
        // min corner, not its center.
        positionScratch.set(
                Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE + position.x + size.x * 0.5f,
                position.y + state.getGroundOffset(),
                Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE + position.z + size.z * 0.5f);
        rotationScratch.set(0f, state.getBodyYaw(), 0f);
        scaleScratch.set(scale, scale, scale);

        RigMathUtility.composeLocal(
                positionScratch, rotationScratch, scaleScratch,
                modelMatrixScratch, matrixScratchA, matrixScratchB);
    }

    // Texture \\

    // The entity's texture user given the tiles it draws now and where it stands, its tiles sent only when they change
    private void refreshTextureUser(EntityInstance entity) {

        int user = entity.getTextureUser();

        if (user == EngineSetting.INDEX_NOT_FOUND)
            return;

        tileScratch.clear();
        collectCharacterTiles(entity.getRaceGender(), entity.getAppearanceHandle(), tileScratch);
        collectEquipmentTiles(entity, tileScratch);

        TextureTileSetStruct tiles = entity.getTextureTiles();

        if (!tileScratch.getTileIDs().equals(tiles.getTileIDs())) {
            tiles.clear();
            tiles.addAll(tileScratch);
            textureManager.setTextureUserTiles(user, tiles.getTileIDs());
        }

        WorldPositionStruct position = entity.getWorldPositionStruct();
        float reach = EngineSetting.TEXTURE_ENTITY_REACH_BLOCKS;
        float x = Coordinate2Long.unpackX(position.getChunkCoordinate()) * (float) EngineSetting.CHUNK_SIZE
                + position.getPosition().x;
        float z = Coordinate2Long.unpackY(position.getChunkCoordinate()) * (float) EngineSetting.CHUNK_SIZE
                + position.getPosition().z;

        textureManager.setTextureUserBounds(user, x - reach, z - reach, x + reach, z + reach);
    }

    // Every character an editor previews this frame, held as one screen user and let go the frame after none is
    private void refreshPreviewTiles(RaceGenderStruct raceGender, AppearanceHandle appearance) {

        if (previewFrame != frame)
            previewTiles.clear();

        this.previewFrame = frame;
        tileScratch.clear();
        tileScratch.addAll(previewTiles);
        collectCharacterTiles(raceGender, appearance, tileScratch);

        if (previewUser == EngineSetting.INDEX_NOT_FOUND) {
            previewTiles.addAll(tileScratch);
            this.previewUser = textureManager.registerScreenTextureUser(previewTiles.getTileIDs());
            return;
        }

        if (tileScratch.getTileIDs().equals(previewTiles.getTileIDs()))
            return;

        previewTiles.clear();
        previewTiles.addAll(tileScratch);
        textureManager.setTextureUserTiles(previewUser, previewTiles.getTileIDs());
    }

    // The body's tiles, and those of every feature worn, its variant and every overlay in the style worn
    private void collectCharacterTiles(
            RaceGenderStruct raceGender,
            AppearanceHandle appearance,
            TextureTileSetStruct tiles) {

        tiles.addAll(raceGender.getMeshHandle().getTiles());

        for (SkinnedOverlay skinnedOverlay : SkinnedOverlay.VALUES) {

            SkinnedOverlayStruct overlay = raceGender.getOverlay(skinnedOverlay);

            if (overlay != null)
                tiles.add(overlay.getTile(appearance != null
                        ? appearance.getOverlayStyle(skinnedOverlay)
                        : EngineSetting.INDEX_NOT_FOUND));
        }

        if (appearance == null)
            return;

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            if (!appearance.hasFeature(featureSlot))
                continue;

            FeatureHandle feature = appearance.getFeature(featureSlot);
            tiles.addAll(feature.getMeshHandle().getTiles());

            if (feature.getVariants() != null)
                tiles.add(feature.getVariants().getTile(appearance.getFeatureVariant(featureSlot)));
        }
    }

    private void collectEquipmentTiles(EntityInstance entity, TextureTileSetStruct tiles) {

        ObjectArrayList<EquipmentAnchorStruct> anchors = entity.getEntityData().getRaceData().getEquipmentAnchors();
        InventoryHandle inventory = entity.getInventoryHandle();

        for (int i = 0; i < anchors.size(); i++)
            if (inventory.isShown(anchors.get(i).getEquipmentSlot()))
                tiles.addAll(inventory.getItem(anchors.get(i).getEquipmentSlot()).getItemDefinitionHandle()
                        .getMeshHandle().getTiles());
    }

    // Appearance \\

    // The appearance worn and every overlay the gender's model places in the style worn, the mask left to each draw
    private void resolveAppearance(RaceGenderStruct raceGender, AppearanceHandle appearance) {

        appearanceScratch.reset();

        for (SkinnedOverlay skinnedOverlay : SkinnedOverlay.VALUES)
            appearanceScratch.setOverlay(
                    skinnedOverlay,
                    raceGender.getOverlay(skinnedOverlay),
                    appearance != null ? appearance.getOverlayStyle(skinnedOverlay) : EngineSetting.INDEX_NOT_FOUND);

        if (appearance == null)
            return;

        appearance.updateFace(internal.getTime() / EngineSetting.MILLIS_PER_SECOND);
        appearance.writeAppearance(appearanceScratch);
    }
}
