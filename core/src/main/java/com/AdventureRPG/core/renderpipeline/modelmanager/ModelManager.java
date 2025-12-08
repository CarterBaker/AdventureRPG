package com.AdventureRPG.core.renderpipeline.modelmanager;

import com.AdventureRPG.core.kernel.ManagerFrame;
import com.AdventureRPG.core.renderpipeline.vaomanager.VAOManager;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

// TODO: A lot of half implemented functionality here. In particular VAO and dynamic mesh
public class ModelManager extends ManagerFrame {

    // Internal
    private InternalLoadManager internalLoadManager;
    private ModelBatchSystem modelBatchSystem;

    private Int2ObjectOpenHashMap<ModelDataInstance> loadedModels;
    private IntSet unloadedModels;
    private int modelCount;

    private int staticVAO;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> meshDataName2MeshDataID;
    private Int2ObjectOpenHashMap<MeshHandle> meshDataID2GPUHandle;

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
        this.meshDataID2GPUHandle = new Int2IntOpenHashMap();
    }

    @Override
    protected void init() {

        // Internal
        VAOManager vaoManager = get(VAOManager.class);
        int staticVAOID = vaoManager.getVAOIDFromName("static");
        this.staticVAO = vaoManager.getVAOHandleFromID(staticVAOID);
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
        int gpuHandle = 0;
        meshDataName2MeshDataID.put(meshDataInstance.meshName, meshDataInstance.meshID);
        meshDataID2GPUHandle.put(meshDataInstance.meshID, gpuHandle);
    }

    private ModelDataInstance createNewStaticModelData() {

        int modelID = createModelID();
        ModelDataInstance model = (ModelDataInstance) create(new StaticModelDataInstance(modelID));

        loadedModels.put(modelID, model);

        return model;
    }

    private ModelDataInstance createNewDynamicModelData() {

        // TODO: Add dynamic model data
        int modelID = createModelID();
        ModelDataInstance model = (ModelDataInstance) create(new StaticModelDataInstance(modelID));

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

    public int getGPUHandleFromMeshID(int meshID) {
        return meshDataID2GPUHandle.get(meshID);
    }

    public ModelDataInstance requestModelData(ModelData modelData) {
        return switch (modelData) {
            case STATIC -> createNewStaticModelData();
            case DYNAMIC -> createNewDynamicModelData();
        };
    }

    public void pushModelData(int modelID) {

        ModelDataInstance modelData = loadedModels.get(modelID);

        if (modelData == null)
            throw new IllegalArgumentException("Model ID " + modelID + " not found");

        if (modelData instanceof StaticModelDataInstance staticModel)
            modelBatchSystem.pushModelData(staticVAO, staticModel);

        // TODO: Handle dynamic models when implemented
    }

    public void pullModelData(int modelID) {

        ModelDataInstance modelData = loadedModels.get(modelID);

        if (modelData == null)
            return;

        if (modelData instanceof StaticModelDataInstance staticModel)
            modelBatchSystem.pullModelData(staticModel);

        // TODO: Handle dynamic models when implemented

        // Mark ID as available for reuse
        unloadedModels.add(modelID);
        loadedModels.remove(modelID);
    }

}
