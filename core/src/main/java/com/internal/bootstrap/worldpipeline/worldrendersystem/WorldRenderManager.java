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
import com.internal.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldRenderManager extends ManagerPackage {

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private RenderSystem renderSystem;
    private GridManager gridManager;
    private FrustumCullingSystem frustumCullingSystem;

    // GPU data
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelHandle>> chunkModels;
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelHandle>> megaModels;

    // Render queues — coordinates only, slot resolved from grid at render time
    private LongLinkedOpenHashSet chunkRenderQueue;
    private LongLinkedOpenHashSet megaRenderQueue;

    // Internal \\

    @Override
    protected void create() {

        this.chunkModels = new Long2ObjectOpenHashMap<>();
        this.megaModels = new Long2ObjectOpenHashMap<>();
        this.chunkRenderQueue = new LongLinkedOpenHashSet();
        this.megaRenderQueue = new LongLinkedOpenHashSet();

        this.frustumCullingSystem = create(FrustumCullingSystem.class);
    }

    @Override
    protected void get() {

        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.renderSystem = get(RenderSystem.class);
        this.gridManager = get(GridManager.class);
    }

    @Override
    protected void update() {
        renderWorld();
    }

    // Render \\

    private void renderWorld() {

        frustumCullingSystem.refresh();

        GridInstance grid = gridManager.getGrid();

        for (long coordinate : megaRenderQueue) {
            GridSlotHandle slot = grid.getGridSlotForChunk(coordinate);
            if (slot != null)
                renderMega(coordinate, slot);
        }

        for (long coordinate : chunkRenderQueue) {
            GridSlotHandle slot = grid.getGridSlotForChunk(coordinate);
            if (slot != null)
                renderChunk(coordinate, slot);
        }
    }

    private void renderMega(long megaCoordinate, GridSlotHandle slot) {

        if (!frustumCullingSystem.isMegaVisible(slot))
            return;

        ObjectArrayList<ModelHandle> models = megaModels.get(megaCoordinate);
        if (models == null)
            return;

        UBOHandle uboHandle = slot.getSlotUBO();
        for (ModelHandle model : models) {
            model.getMaterial().setUBO(EngineSetting.GRID_COORDINATE_UBO, uboHandle);
            renderSystem.pushRenderCall(model, 0);
        }
    }

    private void renderChunk(long chunkCoordinate, GridSlotHandle slot) {

        if (megaRenderQueue.contains(slot.getMegaCoordinate()))
            return;

        if (!frustumCullingSystem.isChunkVisible(slot))
            return;

        ObjectArrayList<ModelHandle> models = chunkModels.get(chunkCoordinate);
        if (models == null)
            return;

        UBOHandle uboHandle = slot.getSlotUBO();
        for (ModelHandle model : models) {
            model.getMaterial().setUBO(EngineSetting.GRID_COORDINATE_UBO, uboHandle);
            renderSystem.pushRenderCall(model, 0);
        }
    }

    // Render Queue \\

    public void rebuildRenderQueue() {

        clearRenderQueue();

        GridInstance grid = gridManager.getGrid();

        for (int i = 0; i < grid.getTotalSlots(); i++) {

            long gridCoordinate = grid.getGridCoordinate(i);
            GridSlotHandle slot = grid.getGridSlot(gridCoordinate);

            queueChunk(slot);

            if (slot.getDetailLevel().renderMode == RenderType.BATCHED)
                queueMega(slot);
        }
    }

    private void queueChunk(GridSlotHandle slot) {

        long chunkCoordinate = slot.getChunkCoordinate();
        long megaCoordinate = slot.getMegaCoordinate();

        if (megaRenderQueue.contains(megaCoordinate))
            return;

        chunkRenderQueue.add(chunkCoordinate);
    }

    private void queueMega(GridSlotHandle slot) {

        long chunkCoordinate = slot.getChunkCoordinate();
        long megaCoordinate = slot.getMegaCoordinate();

        if (chunkCoordinate != megaCoordinate)
            return;

        megaRenderQueue.add(megaCoordinate);

        for (GridSlotHandle coveredSlot : slot.getCoveredSlots())
            chunkRenderQueue.remove(coveredSlot.getChunkCoordinate());
    }

    private void clearRenderQueue() {
        chunkRenderQueue.clear();
        megaRenderQueue.clear();
    }

    // GPU Instance Management \\

    public boolean addChunkInstance(WorldRenderInstance worldRenderInstance) {

        long coordinate = worldRenderInstance.getCoordinate();

        if (gridManager.getGrid().getGridSlotForChunk(coordinate) == null)
            return false;

        if (chunkModels.containsKey(coordinate))
            removeChunkInstance(coordinate);

        ObjectArrayList<ModelHandle> modelList = buildModelList(worldRenderInstance);
        if (modelList == null)
            return false;

        chunkModels.put(coordinate, modelList);
        return true;
    }

    public boolean addMegaInstance(WorldRenderInstance worldRenderInstance) {

        long coordinate = worldRenderInstance.getCoordinate();

        if (gridManager.getGrid().getGridSlotForChunk(coordinate) == null)
            return false;

        if (megaModels.containsKey(coordinate))
            removeMegaInstance(coordinate);

        ObjectArrayList<ModelHandle> modelList = buildModelList(worldRenderInstance);
        if (modelList == null)
            return false;

        megaModels.put(coordinate, modelList);
        return true;
    }

    private ObjectArrayList<ModelHandle> buildModelList(WorldRenderInstance worldRenderInstance) {

        DynamicPacketInstance dynamicPacket = worldRenderInstance.getDynamicPacketInstance();

        if (dynamicPacket.getState() != DynamicPacketState.READY)
            return null;

        ObjectArrayList<ModelHandle> modelList = new ObjectArrayList<>();

        for (var entry : dynamicPacket.getMaterialID2ModelCollection().int2ObjectEntrySet()) {

            int materialID = entry.getIntKey();
            ObjectArrayList<DynamicModelHandle> dynamicModels = entry.getValue();

            for (DynamicModelHandle dynamicModel : dynamicModels) {

                if (dynamicModel.isEmpty())
                    continue;

                VAOHandle cloneVaoHandle = modelManager.cloneVAO(dynamicModel.getVAOHandle());
                MaterialHandle clonedMaterial = materialManager.cloneMaterial(materialID);

                modelList.add(modelManager.createModel(
                        cloneVaoHandle,
                        dynamicModel.getVertices(),
                        dynamicModel.getIndices(),
                        clonedMaterial));
            }
        }

        return modelList.isEmpty() ? null : modelList;
    }

    public void removeChunkInstance(long coordinate) {

        ObjectArrayList<ModelHandle> modelList = chunkModels.get(coordinate);
        if (modelList == null)
            return;

        for (ModelHandle model : modelList)
            modelManager.removeMesh(model);

        modelList.clear();
        chunkModels.remove(coordinate);
    }

    public void removeMegaInstance(long coordinate) {

        ObjectArrayList<ModelHandle> modelList = megaModels.get(coordinate);
        if (modelList == null)
            return;

        for (ModelHandle model : modelList)
            modelManager.removeMesh(model);

        modelList.clear();
        megaModels.remove(coordinate);
    }
}