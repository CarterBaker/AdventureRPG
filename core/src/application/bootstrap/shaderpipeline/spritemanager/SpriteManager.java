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
import engine.root.ManagerPackage;
import engine.util.RegistryUtility;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class SpriteManager extends ManagerPackage {

    /*
     * Owns all loaded SpriteHandles. Drives loading via InternalLoader and
     * exposes cloneSprite() for runtime instance creation. On palette miss,
     * triggers an immediate on-demand load. GPU textures are released on dispose.
     */

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private UBOManager uboManager;

    // Palette
    private Object2IntOpenHashMap<String> spriteName2SpriteID;
    private Int2ObjectOpenHashMap<SpriteHandle> spriteID2SpriteHandle;

    // Base \\

    @Override
    protected void create() {

        this.spriteName2SpriteID = new Object2IntOpenHashMap<>();
        this.spriteID2SpriteHandle = new Int2ObjectOpenHashMap<>();

        create(InternalLoader.class);
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void dispose() {

        for (SpriteHandle handle : spriteID2SpriteHandle.values())
            GLSLUtility.deleteSprite(handle.getGpuHandle());

        spriteName2SpriteID.clear();
        spriteID2SpriteHandle.clear();
    }

    // Management \\

    void addSpriteHandle(String spriteName, SpriteHandle handle) {
        int id = RegistryUtility.toIntID(spriteName);
        spriteName2SpriteID.put(spriteName, id);
        spriteID2SpriteHandle.put(id, handle);
    }

    // Accessible \\

    public void request(String spriteName) {
        ((InternalLoader) internalLoader).request(spriteName);
    }

    public boolean hasSprite(String spriteName) {

        if (!spriteName2SpriteID.containsKey(spriteName))
            request(spriteName);

        return spriteName2SpriteID.containsKey(spriteName);
    }

    public int getSpriteIDFromSpriteName(String spriteName) {

        if (!spriteName2SpriteID.containsKey(spriteName))
            request(spriteName);

        return spriteName2SpriteID.getInt(spriteName);
    }

    public SpriteHandle getSpriteHandleFromSpriteID(int spriteID) {

        SpriteHandle handle = spriteID2SpriteHandle.get(spriteID);

        if (handle == null)
            throwException("Sprite ID not found: " + spriteID);

        return handle;
    }

    public SpriteHandle getSpriteHandleFromSpriteName(String spriteName) {
        return getSpriteHandleFromSpriteID(getSpriteIDFromSpriteName(spriteName));
    }

    public SpriteInstance cloneSprite(String spriteName) {

        SpriteHandle handle = getSpriteHandleFromSpriteName(spriteName);

        InternalLoader loader = (InternalLoader) internalLoader;

        MaterialInstance material = materialManager.cloneMaterial(loader.getDefaultMaterialID());
        material.setUniform("u_sprite", handle.getGpuHandle());

        UBOHandle sliceHandle = uboManager.getUBOHandleFromUBOName("SliceData");
        UBOInstance sliceData = uboManager.createUBOInstance(sliceHandle);

        sliceData.updateUniform("u_border", new Vector4(
                handle.getBorderLeft(),
                handle.getBorderBottom(),
                handle.getBorderRight(),
                handle.getBorderTop()));
        sliceData.updateUniform("u_texSize", new Vector2(
                (float) handle.getWidth(),
                (float) handle.getHeight()));

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