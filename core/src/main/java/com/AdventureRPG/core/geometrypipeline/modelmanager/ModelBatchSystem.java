package com.AdventureRPG.core.geometrypipeline.modelmanager;

import com.AdventureRPG.core.kernel.SystemFrame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ModelBatchSystem extends SystemFrame {

    // Internal
    private Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<IntSet>> modelID2MaterialID2MeshIDCollection;
    private Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<ModelHandle>> materialID2MeshID2ModelHandle;

    private IntSet loadedModels;
    private IntSet unloadedMesh;
    private int meshCount;

    @Override
    protected void create() {

        // Internal
        this.modelID2MaterialID2MeshIDCollection = new Int2ObjectOpenHashMap<>();
        this.materialID2MeshID2ModelHandle = new Int2ObjectOpenHashMap<>();

        this.loadedModels = new IntOpenHashSet();
        this.unloadedMesh = new IntOpenHashSet();
        this.meshCount = 0;
    }

    // Model Management \\

    void draw() {

    }

    void pushModel(ModelData modelData) {

        int modelID = modelData.getModelID();

        if (loadedModels.contains(modelID))
            pullModel(modelData);

        Int2ObjectOpenHashMap<IntSet> materialID2MeshID = modelID2MaterialID2MeshIDCollection.computeIfAbsent(
                modelID,
                k -> new Int2ObjectOpenHashMap<>());

        var iterator = modelData.getMaterialID2MeshCollection()
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
                ModelHandle modelHandle = new ModelHandle(
                        meshHandle.vao,
                        meshHandle.vertStride,
                        meshHandle.vbo,
                        meshHandle.vertCount,
                        meshHandle.ibo,
                        meshHandle.indexCount,
                        materialID);

                meshID2ModelHandle.put(meshID, modelHandle);
            }
        }

        loadedModels.add(modelID);
    }

    void pullModel(ModelData modelData) {

        int modelID = modelData.getModelID();

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
