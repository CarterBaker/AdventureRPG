package com.internal.bootstrap.worldpipeline.worlditemrendersystem;

import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import com.internal.bootstrap.itempipeline.itemrotationmanager.ItemRotationManager;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.Extras.Coordinate4Long;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldItemRenderSystem extends ManagerPackage {

    // Internal
    private ChunkStreamManager chunkStreamManager;
    private ItemDefinitionManager itemDefinitionManager;
    private MaterialManager materialManager;
    private ItemRotationManager itemRotationManager;

    private int CHUNK_SIZE;

    // One buffer per item definition ID
    private Int2ObjectOpenHashMap<ItemInstanceBuffer> itemDefID2Buffer;

    // Tracks which chunks have been registered into the buffers
    private LongOpenHashSet registeredChunks;

    // Internal \\

    @Override
    protected void create() {
        this.itemDefID2Buffer = new Int2ObjectOpenHashMap<>();
        this.registeredChunks = new LongOpenHashSet();
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.materialManager = get(MaterialManager.class);
        this.itemRotationManager = get(ItemRotationManager.class);
    }

    // Update \\

    @Override
    protected void update() {
        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = chunkStreamManager.getActiveChunks();

        for (var entry : activeChunks.long2ObjectEntrySet()) {
            long coord = entry.getLongKey();
            ChunkInstance chunk = entry.getValue();
            if (registeredChunks.contains(coord))
                continue;
            if (!chunk.getChunkDataSyncContainer().hasData(ChunkData.ITEM_DATA))
                continue;
            registerChunk(chunk, coord);
        }

        var it = registeredChunks.iterator();
        while (it.hasNext()) {
            long coord = it.nextLong();
            ChunkInstance chunk = activeChunks.get(coord);
            if (chunk == null ||
                    !chunk.getChunkDataSyncContainer().hasData(ChunkData.ITEM_DATA)) {
                unregisterChunk(chunk);
                it.remove();
            }
        }
    }

    // Register / Unregister \\

    private void registerChunk(ChunkInstance chunk, long chunkCoordinate) {
        WorldItemInstancePaletteHandle palette = chunk.getWorldItemInstancePaletteHandle();
        ObjectArrayList<WorldItemInstance> items = palette.getItems();

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        for (int i = 0; i < items.size(); i++)
            addToBuffer(items.get(i), chunkX, chunkZ);

        registeredChunks.add(chunkCoordinate);
    }

    private void unregisterChunk(ChunkInstance chunk) {
        if (chunk == null)
            return;
        ObjectArrayList<WorldItemInstance> items = chunk.getWorldItemInstancePaletteHandle().getItems();
        for (int i = 0; i < items.size(); i++)
            removeItem(items.get(i));
    }

    // Public API — placement and pickup systems call these directly \\

    public void addItem(WorldItemInstance instance, long chunkCoordinate) {
        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);
        addToBuffer(instance, chunkX, chunkZ);
    }

    public void removeItem(WorldItemInstance instance) {
        int itemDefID = instance.getItemDefinitionHandle().getItemID();
        ItemInstanceBuffer buffer = itemDefID2Buffer.get(itemDefID);
        if (buffer != null)
            buffer.removeInstance(instance);
    }

    // Buffer Management \\

    private void addToBuffer(WorldItemInstance instance, int chunkX, int chunkZ) {
        long packed = instance.getPackedPosition();
        int subX = Coordinate4Long.unpackX(packed);
        int subY = Coordinate4Long.unpackY(packed);
        int subZ = Coordinate4Long.unpackZ(packed);
        int orientation = Coordinate4Long.unpackW(packed);

        float localX = subX / 32.0f;
        float localY = subY / 32.0f;
        float localZ = subZ / 32.0f;

        ItemInstanceBuffer buffer = getOrCreateBuffer(instance.getItemDefinitionHandle());
        buffer.addInstance(instance, chunkX, chunkZ, localX, localY, localZ, orientation);
    }

    private ItemInstanceBuffer getOrCreateBuffer(ItemDefinitionHandle def) {
        int itemDefID = def.getItemID();
        ItemInstanceBuffer buffer = itemDefID2Buffer.get(itemDefID);
        if (buffer == null) {
            MaterialInstance material = materialManager.cloneMaterial(def.getMaterialID());
            buffer = create(ItemInstanceBuffer.class);
            buffer.constructor(def.getMeshHandle(), material);
            itemDefID2Buffer.put(itemDefID, buffer);
        }
        return buffer;
    }

    // Render \\

    public void renderItems() {
        if (itemDefID2Buffer.isEmpty())
            return;

        UBOHandle rotationUBO = itemRotationManager.getRotationUBOHandle();

        for (var entry : itemDefID2Buffer.int2ObjectEntrySet()) {
            ItemInstanceBuffer buffer = entry.getValue();
            if (buffer.isEmpty())
                continue;

            MaterialInstance material = buffer.getMaterial();
            if (material == null)
                continue;

            bindMaterial(material, rotationUBO);
            buffer.draw();
        }
    }

    private void bindMaterial(MaterialInstance material, UBOHandle rotationUBO) {
        int shaderHandle = material.getShaderHandle().getShaderHandle();
        GLSLUtility.useShader(shaderHandle);

        var sourceUBOs = material.getSourceUBOs();
        if (sourceUBOs != null) {
            for (UBOHandle ubo : sourceUBOs.values()) {
                GLSLUtility.bindUniformBlock(shaderHandle, ubo.getBufferName(), ubo.getBindingPoint());
                GLSLUtility.bindUniformBuffer(ubo.getBindingPoint(), ubo.getGpuHandle());
            }
        }

        GLSLUtility.bindUniformBlock(shaderHandle, rotationUBO.getBufferName(), rotationUBO.getBindingPoint());
        GLSLUtility.bindUniformBuffer(rotationUBO.getBindingPoint(), rotationUBO.getGpuHandle());

        var instanceUBOs = material.getInstanceUBOs();
        if (instanceUBOs != null) {
            for (UBOInstance ubo : instanceUBOs.values())
                GLSLUtility.bindUniformBuffer(ubo.getBindingPoint(), ubo.getGpuHandle());
        }

        int textureUnit = 0;
        var uniforms = material.getUniforms();
        if (uniforms != null) {
            for (Uniform<?> uniform : uniforms.values()) {
                if (uniform.attribute().isSampler()) {
                    uniform.attribute().bindTexture(textureUnit);
                    textureUnit++;
                }
                uniform.push();
            }
        }
    }
}