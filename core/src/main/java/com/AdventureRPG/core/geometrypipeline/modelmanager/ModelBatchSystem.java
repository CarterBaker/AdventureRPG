package com.AdventureRPG.core.geometrypipeline.modelmanager;

import com.AdventureRPG.core.geometrypipeline.Models.ModelHandle;
import com.AdventureRPG.core.geometrypipeline.mesh.MeshHandle;
import com.AdventureRPG.core.kernel.SystemFrame;
import com.AdventureRPG.core.shaderpipeline.materialmanager.MaterialManager;
import com.AdventureRPG.core.shaderpipeline.materials.Material;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ModelBatchSystem extends SystemFrame {

    // Internal
    private MaterialManager materialManager;

    private IntSet loadedModels;
    private IntSet unloadedMesh;
    private int meshCount;

    // Retrieval Mapping
    private Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<IntSet>> modelID2MaterialID2MeshIDCollection;
    private Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<ModelHandle>> materialID2MeshID2ModelHandle;

    @Override
    protected void create() {

        // Internal
        this.loadedModels = new IntOpenHashSet();
        this.unloadedMesh = new IntOpenHashSet();
        this.meshCount = 0;

        // Retrieval Mapping
        this.modelID2MaterialID2MeshIDCollection = new Int2ObjectOpenHashMap<>();
        this.materialID2MeshID2ModelHandle = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void init() {

        // Internal
        this.materialManager = gameEngine.get(MaterialManager.class);
    }

    // Model Management \\

    void draw() {

    }

    void pushModel(MeshPacketData meshPacketData) {

        int modelID = meshPacketData.getModelID();

        if (loadedModels.contains(modelID))
            pullModel(meshPacketData);

        Int2ObjectOpenHashMap<IntSet> materialID2MeshID = modelID2MaterialID2MeshIDCollection.computeIfAbsent(
                modelID,
                k -> new Int2ObjectOpenHashMap<>());

        var iterator = meshPacketData.getMaterialID2MeshCollection()
                .int2ObjectEntrySet().fastIterator();

        while (iterator.hasNext()) {

            var entry = iterator.next();
            int materialID = entry.getIntKey();

            ObjectArrayList<MeshData> meshDatas = entry.getValue();
            IntSet meshIDCollection = materialID2MeshID.computeIfAbsent(
                    materialID,
                    k -> new IntOpenHashSet());

            Int2ObjectOpenHashMap<ModelHandle> meshID2ModelHandle = materialID2MeshID2ModelHandle.computeIfAbsent(
                    materialID,
                    k -> new Int2ObjectOpenHashMap<>());

            for (MeshData meshData : meshDatas) {

                if (meshData.isEmpty())
                    continue;

                int meshID;

                if (!unloadedMesh.isEmpty()) {
                    meshID = unloadedMesh.iterator().nextInt();
                    unloadedMesh.remove(meshID);
                }

                else
                    meshID = meshCount++;

                meshIDCollection.add(meshID);

                MeshHandle meshHandle = meshData.meshHandle;
                Material material = materialManager.getMaterialFromMaterialID(materialID);

                ModelHandle modelHandle = new ModelHandle(
                        meshHandle.vao,
                        meshHandle.vertStride,
                        meshHandle.vbo,
                        meshHandle.vertCount,
                        meshHandle.ibo,
                        meshHandle.indexCount,
                        material);

                meshID2ModelHandle.put(meshID, modelHandle);
            }
        }

        loadedModels.add(modelID);
    }

    void pullModel(MeshPacketData meshPacketData) {

        int modelID = meshPacketData.getModelID();

        Int2ObjectOpenHashMap<IntSet> removedModel = modelID2MaterialID2MeshIDCollection
                .remove(modelID);

        if (removedModel == null)
            return;

        loadedModels.remove(modelID);

        var iterator = removedModel.int2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {

            var entry = iterator.next();
            int materialID = entry.getIntKey();
            IntSet meshIDs = entry.getValue();

            // Remove all meshes from the material to mesh map
            Int2ObjectOpenHashMap<ModelHandle> meshID2ModelHandle = materialID2MeshID2ModelHandle.get(materialID);

            if (meshID2ModelHandle != null) {

                for (int meshID : meshIDs) {
                    meshID2ModelHandle.remove(meshID);
                    unloadedMesh.add(meshID);
                }

                // If this material no longer has any meshes, remove it entirely
                if (meshID2ModelHandle.isEmpty())
                    materialID2MeshID2ModelHandle.remove(materialID);
            }
        }
    }

    int getModelCount() {
        return loadedModels.size();
    }

    int getMeshCount() {
        return (meshCount - unloadedMesh.size());
    }

    int getMaterialGroupCount() {
        return materialID2MeshID2ModelHandle.size();
    }
}
