package com.AdventureRPG.core.geometrypipeline.modelmanager;

import com.AdventureRPG.core.kernel.ManagerFrame;
import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOHandle;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ModelManager extends ManagerFrame {

    // Internal
    private InternalLoadManager internalLoadManager;
    private ModelBatchSystem modelBatchSystem;

    private Int2ObjectOpenHashMap<ModelDataInstance> loadedModels;
    private IntSet unloadedModels;
    private int modelCount;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> meshDataName2MeshDataID;
    private Int2ObjectOpenHashMap<MeshHandle> meshDataID2MeshHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.modelBatchSystem = (ModelBatchSystem) register(new ModelBatchSystem());
        this.internalLoadManager = (InternalLoadManager) register(new InternalLoadManager());

        this.loadedModels = new Int2ObjectOpenHashMap<>();
        this.unloadedModels = new IntOpenHashSet();
        this.modelCount = 0;

        // Retrieval Mapping
        this.meshDataName2MeshDataID = new Object2IntOpenHashMap<>();
        this.meshDataID2MeshHandle = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void awake() {
        compileMeshData();
    }

    @Override
    protected void freeMemory() {
        internalLoadManager = (InternalLoadManager) release(internalLoadManager);
    }

    // Model Management \\

    void compileMeshData() {
        internalLoadManager.loadMeshData();
    }

    void addMeshData(MeshDataInstance meshDataInstance) {

        MeshHandle meshHandle = GLSLUtility.uploadMeshData(meshDataInstance.vaoHandle,
                meshDataInstance.getVerticesArray(),
                meshDataInstance.getIndicesArray());

        meshDataName2MeshDataID.put(meshDataInstance.meshName, meshDataInstance.meshID);
        meshDataID2MeshHandle.put(meshDataInstance.meshID, meshHandle);
    }

    private ModelDataInstance createModelData(VAOHandle vaoHandle) {

        int modelID = createModelID();
        ModelDataInstance model = (ModelDataInstance) create(new ModelDataInstance(modelID, vaoHandle));

        loadedModels.put(modelID, model);

        return model;
    }

    // Utility \\

    private int createModelID() {

        if (!unloadedModels.isEmpty()) {

            int id = unloadedModels.iterator().nextInt();
            unloadedModels.remove(id);

            return id;
        }

        return modelCount++;
    }

    // Accessible \\

    public int getMeshDataIDFromMeshName(String meshName) {
        return meshDataName2MeshDataID.getInt(meshName);
    }

    public MeshHandle getMeshHandleFromMeshID(int meshID) {
        return meshDataID2MeshHandle.get(meshID);
    }

    public ModelDataInstance requestModelData(VAOHandle vaoHandle) {
        return createModelData(vaoHandle);
    }

    public void pushModelData(int modelID) {

        ModelDataInstance modelData = loadedModels.get(modelID);

        if (modelData == null) // TODO: Add my own error
            throw new IllegalArgumentException("Model ID " + modelID + " not found");

        modelBatchSystem.pushModelData(modelData.vaoHandle, modelData);
    }

    public void pullModelData(int modelID) {

        ModelDataInstance modelData = loadedModels.get(modelID);

        if (modelData == null)
            return;

        modelBatchSystem.pullModelData(modelData);

        // Mark ID as available for reuse
        unloadedModels.add(modelID);
        loadedModels.remove(modelID);
    }

}
