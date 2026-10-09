package application.bootstrap.shaderpipeline.spritemanager;

import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.sprite.SpriteData;
import application.bootstrap.shaderpipeline.sprite.SpriteHandle;
import application.bootstrap.shaderpipeline.sprite.SpriteInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SpriteManager extends ManagerPackage {

    /*
     * Owns all loaded SpriteHandles, with IDs assigned in registration order.
     * Drives loading via SpriteLoader and exposes cloneSprite() for runtime
     * instance creation. On palette miss, triggers an immediate on-demand
     * load. GPU textures are released on dispose.
     */

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private UBOManager uboManager;

    // Palette
    private Object2IntOpenHashMap<String> spriteName2SpriteID;
    private ObjectArrayList<SpriteHandle> spriteID2SpriteHandle;

    // Base \\

    @Override
    protected void create() {

        this.spriteName2SpriteID = RegistryUtility.createNameIndex();
        this.spriteID2SpriteHandle = RegistryUtility.createPalette();

        create(SpriteLoader.class);
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void dispose() {

        for (int spriteID = 0; spriteID < spriteID2SpriteHandle.size(); spriteID++) {

            SpriteHandle handle = spriteID2SpriteHandle.get(spriteID);

            if (handle != null)
                SpriteGLSLUtility.deleteSprite(handle.getGpuHandle());
        }

        RegistryUtility.clearPalette(spriteName2SpriteID, spriteID2SpriteHandle);
    }

    // Management \\

    void addSpriteHandle(String spriteName, SpriteHandle handle) {
        RegistryUtility.registerHandle(
                spriteName2SpriteID, spriteID2SpriteHandle, spriteName, handle, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    // Accessible \\

    public void request(String spriteName) {
        ((SpriteLoader) internalLoader).request(spriteName);
    }

    private boolean isSpriteRegistered(String spriteName) {
        return RegistryUtility.getHandle(spriteName2SpriteID, spriteID2SpriteHandle, spriteName) != null;
    }

    public boolean hasSprite(String spriteName) {

        if (!isSpriteRegistered(spriteName))
            request(spriteName);

        return isSpriteRegistered(spriteName);
    }

    public int getSpriteIDFromSpriteName(String spriteName) {

        if (!isSpriteRegistered(spriteName))
            request(spriteName);

        return spriteName2SpriteID.getInt(spriteName);
    }

    public SpriteHandle getSpriteHandleFromSpriteID(int spriteID) {

        SpriteHandle handle = RegistryUtility.getHandle(spriteID2SpriteHandle, spriteID);

        if (handle == null)
            throwException("Sprite ID not found: " + spriteID);

        return handle;
    }

    public SpriteHandle getSpriteHandleFromSpriteName(String spriteName) {
        return getSpriteHandleFromSpriteID(getSpriteIDFromSpriteName(spriteName));
    }

    public SpriteInstance cloneSprite(String spriteName) {

        SpriteHandle handle = getSpriteHandleFromSpriteName(spriteName);

        SpriteLoader loader = (SpriteLoader) internalLoader;

        MaterialInstance material = materialManager.cloneMaterial(loader.getDefaultMaterialID());
        material.setUniform(EngineSetting.UNIFORM_SPRITE, handle.getGpuHandle());

        UBOHandle sliceHandle = uboManager.getUBOHandleFromUBOName(EngineSetting.SLICE_DATA_UBO);
        UBOInstance sliceData = uboManager.createUBOInstance(sliceHandle);

        sliceData.updateUniform(EngineSetting.UNIFORM_BORDER, new Vector4(
                handle.getBorderLeft(),
                handle.getBorderBottom(),
                handle.getBorderRight(),
                handle.getBorderTop()));
        sliceData.updateUniform(EngineSetting.UNIFORM_TEX_SIZE, new Vector2(
                (float) handle.getWidth(),
                (float) handle.getHeight()));
        sliceData.updateUniform(EngineSetting.SPRITE_STRETCH_UNIFORM, handle.isStretch() ? 1f : 0f);

        uboManager.push(sliceData);

        material.setUBO(sliceData);

        ModelInstance modelInstance = modelManager.createModel(
                loader.getDefaultMeshHandle(),
                material);

        SpriteData clonedData = new SpriteData(
                handle.getSpriteData(),
                modelInstance,
                sliceData);

        SpriteInstance instance = create(SpriteInstance.class);
        instance.constructor(clonedData);

        return instance;
    }
}