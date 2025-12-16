package com.AdventureRPG.core.geometrypipeline.modelmanager;

import com.AdventureRPG.core.kernel.ManagerFrame;
import com.AdventureRPG.core.geometrypipeline.Mesh.MeshHandle;
import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOHandle;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ModelManager extends ManagerFrame {

    // Internal
    private InternalLoadManager internalLoadManager;
    private ModelBatchSystem modelBatchSystem;

    private Int2ObjectOpenHashMap<ModelData> loadedModels;
    private IntSet unloadedModels;
    private int modelCount;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> meshHandleName2MeshHandleID;
    private Int2ObjectOpenHashMap<MeshHandle> meshHandleID2MeshHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = (InternalLoadManager) register(new InternalLoadManager());
        this.modelBatchSystem = (ModelBatchSystem) register(new ModelBatchSystem());

        this.loadedModels = new Int2ObjectOpenHashMap<>();
        this.unloadedModels = new IntOpenHashSet();
        this.modelCount = 0;

        // Retrieval Mapping
        this.meshHandleName2MeshHandleID = new Object2IntOpenHashMap<>();
        this.meshHandleID2MeshHandle = new Int2ObjectOpenHashMap<>();
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

    public void draw() {
        modelBatchSystem.draw();
    }

    void compileMeshData() {
        internalLoadManager.loadMeshData();
    }

    void addMeshHandle(String meshName, int meshID, MeshHandle meshHandle) {
        meshHandleName2MeshHandleID.put(meshName, meshID);
        meshHandleID2MeshHandle.put(meshID, meshHandle);
    }

    private ModelData createModelData(VAOHandle vaoHandle) {

        int modelID = createModelID();
        ModelData model = new ModelData(modelID, vaoHandle);

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

    public int getMeshHandleIDFromMeshName(String meshName) {
        return meshHandleName2MeshHandleID.getInt(meshName);
    }

    public MeshHandle getMeshHandleFromMeshHandleID(int meshID) {
        return meshHandleID2MeshHandle.get(meshID);
    }

    public ModelData requestModelData(VAOHandle vaoHandle) {
        return createModelData(vaoHandle);
    }

    public void pushModel(ModelData modelData) {
        modelData.setRendering(true);
        modelBatchSystem.pushModel(modelData);
    }

    public void pullModel(ModelData modelData) {
        modelBatchSystem.pullModel(modelData);
        modelData.setRendering(false);
    }
}
