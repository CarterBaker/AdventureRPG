package application.bootstrap.worldpipeline.worldrendermanager;

import application.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketState;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldRenderManager extends ManagerPackage {

    /*
     * Drives the world render pipeline each frame. Each grid owns its render
     * queues as Long2ObjectLinkedOpenHashMap<GridSlotHandle> — chunk and mega
     * world coordinates mapped directly to their slot handle, populated at
     * rebuild time. No reverse lookup needed at render time. Frustum culling
     * refreshes once per grid against that grid's window camera. Render calls
     * are pushed to each grid's window explicitly — no active window state.
     */

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private RenderManager renderManager;
    private WorldStreamManager worldStreamManager;
    private FrustumCullingSystem frustumCullingSystem;

    // GPU Data
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelInstance>> chunkModels;
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelInstance>> megaModels;

    // Settings
    private int batchedChunks;

    // Internal \\

    @Override
    protected void create() {

        this.frustumCullingSystem = create(FrustumCullingSystem.class);
        this.chunkModels = new Long2ObjectOpenHashMap<>();
        this.megaModels = new Long2ObjectOpenHashMap<>();
        this.batchedChunks = EngineSetting.MEGA_CHUNK_SIZE * EngineSetting.MEGA_CHUNK_SIZE;
    }

    @Override
    protected void get() {

        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.renderManager = get(RenderManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void lateUpdate() {
        renderWorld();
    }

    // Render \\

    private void renderWorld() {

        if (!worldStreamManager.hasGrids())
            return;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] gridElements = grids.elements();
        int gridCount = grids.size();

        for (int g = 0; g < gridCount; g++) {

            GridInstance grid = (GridInstance) gridElements[g];

            WindowInstance window = grid.getWindowInstance();

            if (window == null)
                continue;

            frustumCullingSystem.refresh(grid);

            renderGridMegas(grid, window);
            renderGridChunks(grid, window);
        }
    }

    private void renderGridMegas(GridInstance grid, WindowInstance window) {

        Long2ObjectLinkedOpenHashMap<GridSlotHandle> megaQueue = grid.getMegaRenderQueue();
        LongIterator it = megaQueue.keySet().iterator();

        while (it.hasNext()) {

            long coordinate = it.nextLong();
            GridSlotHandle slot = megaQueue.get(coordinate);

            if (!frustumCullingSystem.isMegaVisible(slot))
                continue;

            ObjectArrayList<ModelInstance> models = megaModels.get(coordinate);

            if (models == null)
                continue;

            UBOInstance slotUBO = slot.getSlotUBO();

            for (int i = 0; i < models.size(); i++) {
                ModelInstance model = models.get(i);
                model.getMaterial().setUBO(slotUBO);
                renderManager.pushRenderCall(model, "MainScene", window);
            }
        }
    }

    private void renderGridChunks(GridInstance grid, WindowInstance window) {

        Long2ObjectLinkedOpenHashMap<GridSlotHandle> chunkQueue = grid.getChunkRenderQueue();
        Long2ObjectLinkedOpenHashMap<GridSlotHandle> megaQueue = grid.getMegaRenderQueue();
        LongIterator it = chunkQueue.keySet().iterator();

        while (it.hasNext()) {

            long coordinate = it.nextLong();
            GridSlotHandle slot = chunkQueue.get(coordinate);

            if (megaQueue.containsKey(slot.getMegaCoordinate()))
                continue;

            if (!frustumCullingSystem.isChunkVisible(slot))
                continue;

            ObjectArrayList<ModelInstance> models = chunkModels.get(coordinate);

            if (models == null)
                continue;

            UBOInstance slotUBO = slot.getSlotUBO();

            for (int i = 0; i < models.size(); i++) {
                ModelInstance model = models.get(i);
                model.getMaterial().setUBO(slotUBO);
                renderManager.pushRenderCall(model, "MainScene", window);
            }
        }
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