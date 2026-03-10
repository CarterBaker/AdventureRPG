package com.internal.bootstrap.worldpipeline.worldrendermanager;

import com.internal.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import com.internal.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketState;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.itempipeline.itemrotationmanager.ItemRotationManager;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.worlditemrendersystem.WorldItemRenderSystem;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldRenderManager extends ManagerPackage {

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private RenderSystem renderSystem;
    private GridManager gridManager;
    private FrustumCullingSystem frustumCullingSystem;
    private ItemRotationManager itemRenderRotationSystem;
    private WorldItemRenderSystem itemRenderManager;

    private int BATCHED_CHUNKS = EngineSetting.MEGA_CHUNK_SIZE * EngineSetting.MEGA_CHUNK_SIZE;

    // GPU Data
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelInstance>> chunkModels;
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelInstance>> megaModels;

    // Render Queues
    private LongLinkedOpenHashSet chunkRenderQueue;
    private LongLinkedOpenHashSet megaRenderQueue;

    // Base \\

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
        this.itemRenderRotationSystem = get(ItemRotationManager.class);
        this.itemRenderManager = get(WorldItemRenderSystem.class);
    }

    @Override
    protected void update() {
        renderWorld();
        itemRenderManager.renderItems();
    }

    // Render \\

    private void renderWorld() {

        frustumCullingSystem.refresh();

        GridInstance grid = gridManager.getGrid();

        LongIterator megaIt = megaRenderQueue.iterator();
        while (megaIt.hasNext()) {
            long coordinate = megaIt.nextLong();
            GridSlotHandle slot = grid.getGridSlotForChunk(coordinate);
            if (slot != null)
                renderMega(coordinate, slot);
        }

        LongIterator chunkIt = chunkRenderQueue.iterator();
        while (chunkIt.hasNext()) {
            long coordinate = chunkIt.nextLong();
            GridSlotHandle slot = grid.getGridSlotForChunk(coordinate);
            if (slot != null)
                renderChunk(coordinate, slot);
        }
    }

    private void renderMega(long megaCoordinate, GridSlotHandle slot) {

        if (!frustumCullingSystem.isMegaVisible(slot))
            return;

        ObjectArrayList<ModelInstance> models = megaModels.get(megaCoordinate);
        if (models == null)
            return;

        UBOInstance slotUBO = slot.getSlotUBO();

        for (int i = 0; i < models.size(); i++) {
            ModelInstance model = models.get(i);
            model.getMaterial().setUBO(slotUBO);
            renderSystem.pushRenderCall(model, 0);
        }
    }

    private void renderChunk(long chunkCoordinate, GridSlotHandle slot) {

        if (megaRenderQueue.contains(slot.getMegaCoordinate()))
            return;

        if (!frustumCullingSystem.isChunkVisible(slot))
            return;

        ObjectArrayList<ModelInstance> models = chunkModels.get(chunkCoordinate);
        if (models == null)
            return;

        UBOInstance slotUBO = slot.getSlotUBO();

        for (int i = 0; i < models.size(); i++) {
            ModelInstance model = models.get(i);
            model.getMaterial().setUBO(slotUBO);
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
        ObjectArrayList<GridSlotHandle> coveredSlots = slot.getCoveredSlots();
        if (coveredSlots.size() != BATCHED_CHUNKS)
            return;
        megaRenderQueue.add(megaCoordinate);
        for (int i = 0; i < coveredSlots.size(); i++)
            chunkRenderQueue.remove(coveredSlots.get(i).getChunkCoordinate());
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

        ObjectArrayList<ModelInstance> modelList = buildModelList(worldRenderInstance);
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

        ObjectArrayList<ModelInstance> modelList = buildModelList(worldRenderInstance);
        if (modelList == null)
            return false;

        megaModels.put(coordinate, modelList);
        return true;
    }

    private ObjectArrayList<ModelInstance> buildModelList(WorldRenderInstance worldRenderInstance) {

        DynamicPacketInstance dynamicPacket = worldRenderInstance.getDynamicPacketInstance();

        if (dynamicPacket.getState() != DynamicPacketState.READY)
            return null;

        ObjectArrayList<ModelInstance> modelList = new ObjectArrayList<>();

        for (Int2ObjectMap.Entry<ObjectArrayList<DynamicModelHandle>> entry : dynamicPacket
                .getMaterialID2ModelCollection().int2ObjectEntrySet()) {

            int materialID = entry.getIntKey();
            ObjectArrayList<DynamicModelHandle> dynamicModels = entry.getValue();

            for (int i = 0; i < dynamicModels.size(); i++) {

                DynamicModelHandle dynamicModel = dynamicModels.get(i);

                if (dynamicModel.isEmpty())
                    continue;

                MaterialInstance clonedMaterial = materialManager.cloneMaterial(materialID);

                modelList.add(modelManager.createModel(
                        dynamicModel.getVAOHandle(),
                        dynamicModel.getVertices(),
                        dynamicModel.getIndices(),
                        clonedMaterial));
            }
        }

        return modelList.isEmpty() ? null : modelList;
    }

    public void removeChunkInstance(long coordinate) {

        ObjectArrayList<ModelInstance> modelList = chunkModels.get(coordinate);
        if (modelList == null)
            return;

        for (int i = 0; i < modelList.size(); i++)
            modelManager.removeMesh(modelList.get(i));

        modelList.clear();
        chunkModels.remove(coordinate);
    }

    public void removeMegaInstance(long coordinate) {

        ObjectArrayList<ModelInstance> modelList = megaModels.get(coordinate);
        if (modelList == null)
            return;

        for (int i = 0; i < modelList.size(); i++)
            modelManager.removeMesh(modelList.get(i));

        modelList.clear();
        megaModels.remove(coordinate);
    }
}