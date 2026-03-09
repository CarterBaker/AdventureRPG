package com.internal.bootstrap.shaderpipeline.spritemanager;

import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.mathematics.vectors.Vector2;
import com.internal.core.util.mathematics.vectors.Vector4;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Owns all loaded SpriteHandles and provides runtime access and cloning.
 * Cloning produces a SpriteInstance with independent material state backed
 * by the same shared GPU texture. On accessor miss, triggers an immediate
 * synchronous load through the active InternalLoadManager.
 * GPU resources are released on dispose.
 */
public class SpriteManager extends ManagerPackage {

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private UBOManager uboManager;

    // Data
    private Object2ObjectOpenHashMap<String, SpriteHandle> spriteName2SpriteHandle;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoader.class);
        this.spriteName2SpriteHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void dispose() {
        SpriteHandle[] handles = spriteName2SpriteHandle.values().toArray(new SpriteHandle[0]);
        for (int i = 0; i < handles.length; i++)
            GLSLUtility.deleteSprite(handles[i].getGPUHandle());
        spriteName2SpriteHandle.clear();
    }

    // On-Demand Loading \\

    public void request(String spriteName) {
        ((InternalLoader) internalLoader).request(spriteName);
    }

    // Sprite Management \\

    void addSprite(String spriteName, SpriteHandle spriteHandle) {
        spriteName2SpriteHandle.put(spriteName, spriteHandle);
    }

    // Accessible \\

    public boolean hasSprite(String spriteName) {
        if (!spriteName2SpriteHandle.containsKey(spriteName))
            request(spriteName);
        return spriteName2SpriteHandle.containsKey(spriteName);
    }

    public SpriteInstance cloneSprite(String spriteName) {

        SpriteHandle original = spriteName2SpriteHandle.get(spriteName);
        if (original == null) {
            request(spriteName);
            original = spriteName2SpriteHandle.get(spriteName);
        }
        if (original == null)
            throwException("Sprite not found after load: '" + spriteName + "'");

        InternalLoader loader = (InternalLoader) internalLoader;

        MaterialInstance material = materialManager.cloneMaterial(loader.getDefaultMaterialID());
        material.setUniform("u_sprite", original.getGPUHandle());

        UBOHandle sliceHandle = uboManager.getUBOHandleFromUBOName("SliceData");
        UBOInstance sliceData = uboManager.cloneUBO(sliceHandle);
        sliceData.updateUniform("u_border", new Vector4(
                original.getBorderLeft(),
                original.getBorderBottom(),
                original.getBorderRight(),
                original.getBorderTop()));
        sliceData.updateUniform("u_texSize", new Vector2(
                (float) original.getWidth(),
                (float) original.getHeight()));
        sliceData.push();

        material.setUBO(sliceData);

        ModelInstance clonedModel = modelManager.createModel(loader.getDefaultMeshHandle(), material);

        SpriteInstance instance = create(SpriteInstance.class);
        instance.constructor(
                original.getName(),
                original.getGPUHandle(),
                original.getWidth(),
                original.getHeight(),
                clonedModel,
                sliceData);

        return instance;
    }
}