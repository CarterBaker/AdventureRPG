package com.AdventureRPG.core.geometrypipeline.modelmanager;

import com.AdventureRPG.core.kernel.ManagerFrame;
import com.AdventureRPG.core.geometrypipeline.modelbatchsystem.ModelBatchSystem;
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
    private Object2IntOpenHashMap<String> meshDataName2MeshDataID;
    private Int2ObjectOpenHashMap<MeshHandle> meshDataID2MeshHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = (InternalLoadManager) register(new InternalLoadManager());

        this.loadedModels = new Int2ObjectOpenHashMap<>();
        this.unloadedModels = new IntOpenHashSet();
        this.modelCount = 0;

        // Retrieval Mapping
        this.meshDataName2MeshDataID = new Object2IntOpenHashMap<>();
        this.meshDataID2MeshHandle = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void init() {

        // Internal
        this.modelBatchSystem = gameEngine.get(ModelBatchSystem.class);
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

    void addMeshHandle(String meshName, int meshID, MeshHandle meshHandle) {
        meshDataName2MeshDataID.put(meshName, meshID);
        meshDataID2MeshHandle.put(meshID, meshHandle);
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

    public int getMeshDataIDFromMeshName(String meshName) {
        return meshDataName2MeshDataID.getInt(meshName);
    }

    public MeshHandle getMeshHandleFromMeshID(int meshID) {
        return meshDataID2MeshHandle.get(meshID);
    }

    public ModelData requestModelData(VAOHandle vaoHandle) {
        return createModelData(vaoHandle);
    }

    public ModelHandle pushModelData(ModelData modelData) {
        return modelBatchSystem.pushModelData(modelData);
    }

    public void pullModelData(ModelHandle modelHandle) {
        modelBatchSystem.pullModelData(modelHandle);
    }
}
