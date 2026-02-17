package com.internal.bootstrap.worldpipeline.worldrendersystem;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicModelHandle;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketState;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldRenderSystem extends SystemPackage {

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private RenderSystem renderSystem;

    // Separate GPU data storage by type
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelHandle>> chunkModels;
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelHandle>> megaModels;

    LongOpenHashSet renderedMegas;
    LongOpenHashSet chunksOwnedByMegas;

    // Unified wish list - coordinates to render (chunks and megas mixed)
    private LongLinkedOpenHashSet desiredCoordinates;

    // References to active chunks/megas
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;

    // Mega settings
    private int MEGA_CHUNK_SIZE;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.chunkModels = new Long2ObjectOpenHashMap<>();
        this.megaModels = new Long2ObjectOpenHashMap<>();
        this.desiredCoordinates = new LongLinkedOpenHashSet();

        this.renderedMegas = new LongOpenHashSet();
        this.chunksOwnedByMegas = new LongOpenHashSet();
    }

    @Override
    protected void get() {

        // Internal
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.renderSystem = get(RenderSystem.class);

        this.MEGA_CHUNK_SIZE = EngineSetting.MEGA_CHUNK_SIZE;
    }

    @Override
    protected void update() {

        renderedMegas.clear();
        chunksOwnedByMegas.clear();

        // First pass: identify which megas will render and mark their children
        for (long coordinate : desiredCoordinates) {
            if (megaModels.containsKey(coordinate)) {
                renderedMegas.add(coordinate);

                // Mark all child chunks
                int megaX = Coordinate2Long.unpackX(coordinate);
                int megaZ = Coordinate2Long.unpackY(coordinate);

                for (int x = 0; x < MEGA_CHUNK_SIZE; x++) {
                    for (int z = 0; z < MEGA_CHUNK_SIZE; z++) {
                        long childChunk = Coordinate2Long.pack(megaX + x, megaZ + z);
                        chunksOwnedByMegas.add(childChunk);
                    }
                }
            }
        }

        // Second pass: render everything
        for (long coordinate : desiredCoordinates) {

            // Try to render as mega
            if (megaModels.containsKey(coordinate)) {
                MegaChunkInstance mega = activeMegaChunks.get(coordinate);
                if (mega != null && mega.getGridSlotHandle() != null) {
                    UBOHandle uboHandle = mega.getGridSlotHandle().getSlotUBO();

                    ObjectArrayList<ModelHandle> models = megaModels.get(coordinate);
                    for (ModelHandle model : models) {
                        model.getMaterial().setUBO(EngineSetting.GRID_COORDINATE_UBO, uboHandle);
                        renderSystem.pushRenderCall(model, 0);
                    }
                }
                continue;
            }

            // Try to render as individual chunk (skip if owned by mega)
            if (chunksOwnedByMegas.contains(coordinate))
                continue;

            if (chunkModels.containsKey(coordinate)) {
                ChunkInstance chunk = activeChunks.get(coordinate);
                if (chunk != null && chunk.getGridSlotHandle() != null) {
                    UBOHandle uboHandle = chunk.getGridSlotHandle().getSlotUBO();

                    ObjectArrayList<ModelHandle> models = chunkModels.get(coordinate);
                    for (ModelHandle model : models) {
                        model.getMaterial().setUBO(EngineSetting.GRID_COORDINATE_UBO, uboHandle);
                        renderSystem.pushRenderCall(model, 0);
                    }
                }
            }
        }
    }

    // Render Queue System \\

    public void clearRenderQueue() {
        desiredCoordinates.clear();
    }

    public void queueForRender(long coordinate) {
        desiredCoordinates.add(coordinate);
    }

    // World Render System \\

    public boolean renderWorldInstance(WorldRenderInstance worldRenderInstance) {

        if (worldRenderInstance.getGridSlotHandle() == null)
            return false;

        long coordinate = worldRenderInstance.getCoordinate();
        RenderType renderType = worldRenderInstance.renderType;

        // Check if already uploaded and remove it first
        if (renderType == RenderType.INDIVIDUAL && chunkModels.containsKey(coordinate))
            removeWorldInstance(coordinate, renderType);
        else if (renderType == RenderType.BATCHED && megaModels.containsKey(coordinate))
            removeWorldInstance(coordinate, renderType);

        DynamicPacketInstance dynamicPacket = worldRenderInstance.getDynamicPacketInstance();

        if (dynamicPacket.getState() != DynamicPacketState.READY)
            return false;

        // Create list to store all models
        ObjectArrayList<ModelHandle> modelList = new ObjectArrayList<>();

        // Iterate through each material in the dynamic packet
        var materialCollection = dynamicPacket.getMaterialID2ModelCollection();

        for (var entry : materialCollection.int2ObjectEntrySet()) {

            int materialID = entry.getIntKey();
            ObjectArrayList<DynamicModelHandle> dynamicModels = entry.getValue();

            // Process each dynamic model for this material
            for (DynamicModelHandle dynamicModel : dynamicModels) {

                if (dynamicModel.isEmpty())
                    continue;

                // Clone the VAO template to get a unique VAO for this mesh
                VAOHandle cloneVaoHandle = modelManager.cloneVAO(dynamicModel.getVAOHandle());

                // Clone the material to get a unique instance
                MaterialHandle clonedMaterial = materialManager.cloneMaterial(materialID);

                // Create model from dynamic model data (uploads to GPU)
                ModelHandle modelHandle = modelManager.createModel(
                        cloneVaoHandle,
                        dynamicModel.getVertices(),
                        dynamicModel.getIndices(),
                        clonedMaterial);

                // Add to model list
                modelList.add(modelHandle);
            }
        }

        if (modelList.isEmpty())
            return false;

        // Store in the appropriate map based on render type
        if (renderType == RenderType.INDIVIDUAL) {
            chunkModels.put(coordinate, modelList);
        } else {
            megaModels.put(coordinate, modelList);
        }

        return true;
    }

    public void removeWorldInstance(long coordinate) {
        // Try removing from both maps (one will succeed, one will no-op)
        removeWorldInstance(coordinate, RenderType.INDIVIDUAL);
        removeWorldInstance(coordinate, RenderType.BATCHED);
    }

    private void removeWorldInstance(long coordinate, RenderType renderType) {

        // Get from appropriate map
        Long2ObjectOpenHashMap<ObjectArrayList<ModelHandle>> sourceMap = (renderType == RenderType.INDIVIDUAL)
                ? chunkModels
                : megaModels;

        ObjectArrayList<ModelHandle> modelList = sourceMap.get(coordinate);

        if (modelList == null)
            return;

        // Remove each model (this unloads VBO/IBO from GPU)
        for (ModelHandle model : modelList)
            modelManager.removeMesh(model);

        // Clear the list and remove from map
        modelList.clear();
        sourceMap.remove(coordinate);
    }

    // Utility \\

    public void setActiveChunks(Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {
        this.activeChunks = activeChunks;
    }

    public void setActiveMegaChunks(Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks) {
        this.activeMegaChunks = activeMegaChunks;
    }
}