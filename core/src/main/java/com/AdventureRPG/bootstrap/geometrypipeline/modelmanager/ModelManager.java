package com.AdventureRPG.bootstrap.geometrypipeline.modelmanager;

import com.AdventureRPG.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.AdventureRPG.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ModelManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;
    private ModelBatchSystem modelBatchSystem;

    private Int2ObjectOpenHashMap<MeshPacketData> loadedModels;
    private IntSet unloadedModels;
    private int modelCount;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> meshHandleName2MeshHandleID;
    private Int2ObjectOpenHashMap<MeshHandle> meshHandleID2MeshHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = create(InternalLoadManager.class);
        this.modelBatchSystem = create(ModelBatchSystem.class);

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
    protected void release() {
        internalLoadManager = release(InternalLoadManager.class);
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

    private MeshPacketData createMeshPacketData(VAOHandle vaoHandle) {

        int modelID = createModelID();
        MeshPacketData model = new MeshPacketData(modelID, vaoHandle);

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

    public MeshPacketData requestMeshPacketData(VAOHandle vaoHandle) {
        return createMeshPacketData(vaoHandle);
    }

    public void pushModel(MeshPacketData meshPacketData) {
        meshPacketData.setRendering(true);
        modelBatchSystem.pushModel(meshPacketData);
    }

    public void pullModel(MeshPacketData meshPacketData) {
        modelBatchSystem.pullModel(meshPacketData);
        meshPacketData.setRendering(false);
    }
}
