package com.internal.bootstrap.shaderpipeline.spritemanager;

import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteInstance;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Owns all loaded SpriteHandles and provides runtime access and cloning.
 * Cloning produces a SpriteInstance with independent material state backed
 * by the same shared GPU texture. GPU resources are released on dispose.
 */
public class SpriteManager extends ManagerPackage {

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private InternalLoadManager internalLoadManager;

    // Data
    private Object2ObjectOpenHashMap<String, SpriteHandle> spriteName2SpriteHandle;

    // Internal \\

    @Override
    protected void create() {
        this.internalLoadManager = create(InternalLoadManager.class);
        this.spriteName2SpriteHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
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

        SpriteHandle[] handles = spriteName2SpriteHandle.values().toArray(new SpriteHandle[0]);

        for (int i = 0; i < handles.length; i++)
            GLSLUtility.deleteSprite(handles[i].getGPUHandle());

        spriteName2SpriteHandle.clear();
    }

    // Sprite Management \\

    void addSprite(String spriteName, SpriteHandle spriteHandle) {
        spriteName2SpriteHandle.put(spriteName, spriteHandle);
    }

    // Accessible \\

    public boolean hasSprite(String spriteName) {
        return spriteName2SpriteHandle.containsKey(spriteName);
    }

    public SpriteInstance cloneSprite(String spriteName) {

        SpriteHandle original = spriteName2SpriteHandle.get(spriteName);

        if (original == null)
            throwException("Sprite not found: '" + spriteName + "'");

        MaterialInstance material = materialManager.cloneMaterial(
                original.getModelHandle().getMaterial().getMaterialID());

        material.setUniform("u_sprite", original.getGPUHandle());

        ModelInstance clonedModel = modelManager.createModel(
                original.getModelHandle(),
                material);

        SpriteInstance instance = create(SpriteInstance.class);
        instance.constructor(
                original.getName(),
                original.getGPUHandle(),
                original.getWidth(),
                original.getHeight(),
                clonedModel);

        return instance;
    }
}