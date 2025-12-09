package com.AdventureRPG.core.geometrypipeline.modelmanager;

import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOHandle;
import com.AdventureRPG.core.kernel.SystemFrame;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

// TODO: This needs to be looked at closely
public class ModelBatchSystem extends SystemFrame {

    // Internal
    private Int2ObjectOpenHashMap<IntArrayList> materialID2MeshIDs;
    private Int2ObjectOpenHashMap<MeshHandle> meshID2MeshHandle;
    private Int2ObjectOpenHashMap<ModelBatchHandle> modelDataID2ModelBatchHandle;

    private IntOpenHashSet availableMeshIDs;
    private int meshIDCounter;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.materialID2MeshIDs = new Int2ObjectOpenHashMap<>();
        this.meshID2MeshHandle = new Int2ObjectOpenHashMap<>();
        this.modelDataID2ModelBatchHandle = new Int2ObjectOpenHashMap<>();

        this.availableMeshIDs = new IntOpenHashSet();
        this.meshIDCounter = 0;
    }

    // Model Batch \\

    void draw() {

        for (var entry : materialID2MeshIDs.int2ObjectEntrySet()) {

            int materialID = entry.getIntKey();
            IntArrayList meshIDs = entry.getValue();

            // TODO: Bind material once
            // bindMaterial(materialID);

            for (int i = 0; i < meshIDs.size(); i++) {

                int meshID = meshIDs.getInt(i);
                MeshHandle handle = meshID2MeshHandle.get(meshID);

                if (handle == null)
                    continue;

                // TODO: Draw mesh
                // drawMesh(handle);

            }
        }
    }

    // Utility \\

    void pushModelData(VAOHandle vaoHandle, ModelDataInstance modelData) {

        int modelID = modelData.getModelID();

        // If already on GPU, pull first then re-push
        if (modelDataID2ModelBatchHandle.containsKey(modelID))
            pullModelData(modelData);

        // Create batch handle for this model
        ModelBatchHandle batchHandle = (ModelBatchHandle) create(new ModelBatchHandle(modelID));

        // Process each material's meshes
        Int2ObjectOpenHashMap<ObjectArrayList<MeshDataInstance>> meshCollections = modelData
                .getMaterialID2MeshCollection();

        for (var entry : meshCollections.int2ObjectEntrySet()) {

            int materialID = entry.getIntKey();
            ObjectArrayList<MeshDataInstance> meshList = entry.getValue();

            // Get or create the mesh ID list for this material
            IntArrayList materialMeshIDs = materialID2MeshIDs.computeIfAbsent(
                    materialID,
                    k -> new IntArrayList());

            // Track mesh IDs for this model
            IntArrayList modelMeshIDs = new IntArrayList();

            // Upload each mesh to GPU
            for (MeshDataInstance meshData : meshList) {

                if (meshData.isEmpty())
                    continue;

                // Assign a new mesh ID
                int meshID = allocateMeshID();

                // Upload mesh data to GPU
                float[] vertices = meshData.getVerticesArray();
                short[] indices = meshData.getIndicesArray();

                MeshHandle meshHandle = GLSLUtility.uploadMeshData(vaoHandle, vertices, indices);

                // Store mesh handle with O(1) lookup
                meshID2MeshHandle.put(meshID, meshHandle);

                // Add to material's mesh list
                materialMeshIDs.add(meshID);

                // Track for this model
                modelMeshIDs.add(meshID);
            }

            // Store the mesh IDs in the batch handle
            if (!modelMeshIDs.isEmpty())
                batchHandle.materialID2MeshHandle.put(materialID, modelMeshIDs);
        }

        // Store the batch handle
        modelDataID2ModelBatchHandle.put(modelID, batchHandle);
    }

    void pullModelData(ModelDataInstance modelData) {

        int modelID = modelData.getModelID();

        // Get the batch handle
        ModelBatchHandle batchHandle = modelDataID2ModelBatchHandle.get(modelID);

        if (batchHandle == null)
            return; // Already removed or never pushed

        // Free each mesh's GPU resources and remove from material collections
        for (var entry : batchHandle.materialID2MeshHandle.int2ObjectEntrySet()) {

            int materialID = entry.getIntKey();
            IntArrayList modelMeshIDs = entry.getValue();

            IntArrayList materialMeshIDs = materialID2MeshIDs.get(materialID);

            if (materialMeshIDs == null)
                continue;

            // Free GPU resources and remove from material collection
            for (int i = 0; i < modelMeshIDs.size(); i++) {

                int meshID = modelMeshIDs.getInt(i);
                MeshHandle meshHandle = meshID2MeshHandle.get(meshID);

                if (meshHandle != null) {
                    GLSLUtility.freeMeshData(meshHandle);
                    meshID2MeshHandle.remove(meshID);
                }

                // Remove mesh ID from material's list
                materialMeshIDs.rem(meshID);

                // Return mesh ID to pool for reuse
                freeMeshID(meshID);
            }

            // Clean up empty material collections
            if (materialMeshIDs.isEmpty())
                materialID2MeshIDs.remove(materialID);
        }

        // Remove and cleanup the batch handle
        modelDataID2ModelBatchHandle.remove(modelID);
    }

    private int allocateMeshID() {

        if (!availableMeshIDs.isEmpty()) {
            int id = availableMeshIDs.iterator().nextInt();
            availableMeshIDs.remove(id);
            return id;
        }

        return meshIDCounter++;
    }

    private void freeMeshID(int meshID) {
        availableMeshIDs.add(meshID);
    }
}