package com.internal.bootstrap.worldpipeline.worldrendermanager;

import com.internal.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import com.internal.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketState;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldRenderManager extends ManagerPackage {

    /*
     * Drives the world render pipeline each frame. Maintains GPU model lists for
     * individual chunks and batched megas, manages render queues rebuilt on each
     * player chunk crossing, and delegates frustum culling to FrustumCullingSystem.
     * Loops all active grids — one grid in game, multiple in editor.
     */

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private RenderSystem renderSystem;
    private WorldStreamManager worldStreamManager;
    private FrustumCullingSystem frustumCullingSystem;

    // GPU Data
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelInstance>> chunkModels;
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelInstance>> megaModels;

    // Render Queues
    private LongLinkedOpenHashSet chunkRenderQueue;
    private LongLinkedOpenHashSet megaRenderQueue;

    // Settings
    private int batchedChunks;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.frustumCullingSystem = create(FrustumCullingSystem.class);

        // GPU Data
        this.chunkModels = new Long2ObjectOpenHashMap<>();
        this.megaModels = new Long2ObjectOpenHashMap<>();

        // Render Queues
        this.chunkRenderQueue = new LongLinkedOpenHashSet();
        this.megaRenderQueue = new LongLinkedOpenHashSet();

        // Settings
        this.batchedChunks = EngineSetting.MEGA_CHUNK_SIZE * EngineSetting.MEGA_CHUNK_SIZE;
    }

    @Override
    protected void get() {

        // Internal
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.renderSystem = get(RenderSystem.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void update() {
        renderWorld();
    }

    // Render \\

    private void renderWorld() {

        if (!worldStreamManager.hasGrids())
            return;

        frustumCullingSystem.refresh();

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] gridElements = grids.elements();
        int gridCount = grids.size();

        LongIterator megaIt = megaRenderQueue.iterator();

        while (megaIt.hasNext()) {

            long coordinate = megaIt.nextLong();

            for (int i = 0; i < gridCount; i++) {
                GridSlotHandle slot = ((GridInstance) gridElements[i]).getGridSlotForChunk(coordinate);
                if (slot != null) {
                    renderMega(coordinate, slot);
                    break;
                }
            }
        }

        LongIterator chunkIt = chunkRenderQueue.iterator();

        while (chunkIt.hasNext()) {

            long coordinate = chunkIt.nextLong();

            for (int i = 0; i < gridCount; i++) {
                GridSlotHandle slot = ((GridInstance) gridElements[i]).getGridSlotForChunk(coordinate);
                if (slot != null) {
                    renderChunk(coordinate, slot);
                    break;
                }
            }
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

        if (!worldStreamManager.hasGrids())
            return;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int g = 0; g < size; g++) {

            GridInstance grid = (GridInstance) elements[g];

            for (int i = 0; i < grid.getTotalSlots(); i++) {

                long gridCoordinate = grid.getGridCoordinate(i);
                GridSlotHandle slot = grid.getGridSlot(gridCoordinate);

                queueChunk(slot);

                if (slot.getDetailLevel().renderMode == RenderType.BATCHED)
                    queueMega(slot);
            }
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

        if (coveredSlots.size() != batchedChunks)
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

        if (!hasGridSlotForChunk(coordinate))
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

        if (!hasGridSlotForChunk(coordinate))
            return false;

        if (megaModels.containsKey(coordinate))
            removeMegaInstance(coordinate);

        ObjectArrayList<ModelInstance> modelList = buildModelList(worldRenderInstance);

        if (modelList == null)
            return false;

        megaModels.put(coordinate, modelList);
        return true;
    }

    private boolean hasGridSlotForChunk(long coordinate) {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {
            if (((GridInstance) elements[i]).getGridSlotForChunk(coordinate) != null)
                return true;
        }

        return false;
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