package com.internal.bootstrap.shaderpipeline.spritemanager;

import com.internal.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class SpriteManager extends ManagerPackage {

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;

    private InternalLoadManager internalLoadManager;

    // Data
    private Object2ObjectOpenHashMap<String, SpriteHandle> spriteName2SpriteHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = create(InternalLoadManager.class);

        // Data
        this.spriteName2SpriteHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
    }

    @Override
    protected void awake() {
        internalLoadManager.loadSprites();
    }

    @Override
    protected void release() {
        this.internalLoadManager = release(InternalLoadManager.class);
    }

    @Override
    public void dispose() {
        for (SpriteHandle handle : spriteName2SpriteHandle.values())
            GLSLUtility.deleteSprite(handle.getGPUHandle());
        spriteName2SpriteHandle.clear();
    }

    // Sprite Management \\

    void addSprite(String spriteName, SpriteHandle spriteHandle) {
        spriteName2SpriteHandle.put(spriteName, spriteHandle);
    }

    // Accessible \\

    public SpriteHandle getSprite(String spriteName) {

        SpriteHandle handle = spriteName2SpriteHandle.get(spriteName);

        if (handle == null)
            throwException("Sprite not found: '" + spriteName + "'");

        return handle;
    }

    public boolean hasSprite(String spriteName) {
        return spriteName2SpriteHandle.containsKey(spriteName);
    }

    public SpriteHandle cloneSprite(String spriteName) {
        SpriteHandle original = getSprite(spriteName); // already throws if missing

        MaterialHandle clonedMaterial = materialManager.cloneMaterial(
                original.getModelHandle().getMaterial().getMaterialID());

        // Preserve the texture binding on the fresh material
        clonedMaterial.setUniform("u_sprite", original.getGPUHandle());

        ModelHandle clonedModel = modelManager.createModel(
                original.getModelHandle().getMeshHandle(),
                clonedMaterial);

        SpriteHandle clone = create(SpriteHandle.class);
        clone.constructor(
                original.getName(),
                original.getGPUHandle(), // shared — same GPU texture
                original.getWidth(),
                original.getHeight(),
                clonedModel); // owns its own material/uniform state

        return clone;
    }
}