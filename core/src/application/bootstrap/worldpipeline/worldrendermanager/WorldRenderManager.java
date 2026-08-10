package application.bootstrap.worldpipeline.worldrendermanager;

import application.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketState;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
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
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldRenderManager extends ManagerPackage {

    /*
     * Owns the GPU-resident render representation of every rendered chunk and
     * mega chunk. A geometry rebuild (block edit, liquid flow, streaming) no
     * longer tears down and recreates GL buffers — updateEntries() reconciles
     * the new packet against the previous one bucket-by-bucket, reuploading
     * data into the SAME VBO/IBO handles wherever a bucket already exists and
     * only allocating or freeing GL objects when the bucket count for a
     * material actually grows or shrinks. This keeps per-window VAO clones
     * (see VAOManager) valid across updates, since they reference these same
     * handles, and eliminates the GL object churn that made frequent updates
     * — liquid ticks especially — extremely expensive.
     */

    private MaterialManager materialManager;
    private ModelManager modelManager;
    private MeshManager meshManager;
    private RenderManager renderManager;
    private WorldStreamManager worldStreamManager;
    private FrustumCullingSystem frustumCullingSystem;

    private Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> chunkEntries;
    private Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> megaEntries;

    private int batchedChunks;

    @Override
    protected void create() {
        this.frustumCullingSystem = create(FrustumCullingSystem.class);
        this.chunkEntries = new Long2ObjectOpenHashMap<>();
        this.megaEntries = new Long2ObjectOpenHashMap<>();
        this.batchedChunks = EngineSetting.MEGA_CHUNK_SIZE * EngineSetting.MEGA_CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.meshManager = get(MeshManager.class);
        this.renderManager = get(RenderManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void lateUpdate() {
        renderWorld();
    }

    private void renderWorld() {

        if (!worldStreamManager.hasGrids())
            return;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] gridElements = grids.elements();
        int gridCount = grids.size();

        for (int g = 0; g < gridCount; g++) {

            GridInstance grid = (GridInstance) gridElements[g];

            WindowInstance window = grid.getWindowInstance();
            FboInstance worldFbo = grid.getRenderTargetFbo();

            if (window == null || worldFbo == null)
                continue;

            frustumCullingSystem.refresh(grid);

            renderGridMegas(grid, window, worldFbo);
            renderGridChunks(grid, window, worldFbo);
        }
    }

    private void renderGridMegas(GridInstance grid, WindowInstance window, FboInstance worldFbo) {

        Long2ObjectLinkedOpenHashMap<GridSlotHandle> megaQueue = grid.getMegaRenderQueue();
        LongIterator it = megaQueue.keySet().iterator();

        while (it.hasNext()) {

            long coordinate = it.nextLong();
            GridSlotHandle slot = megaQueue.get(coordinate);

            if (!frustumCullingSystem.isMegaVisible(slot))
                continue;

            Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialEntries = megaEntries.get(coordinate);

            if (materialEntries == null)
                continue;

            pushEntries(materialEntries, slot.getSlotUBO(), worldFbo, window);
        }
    }

    private void renderGridChunks(GridInstance grid, WindowInstance window, FboInstance worldFbo) {

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

            Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialEntries = chunkEntries.get(coordinate);

            if (materialEntries == null)
                continue;

            pushEntries(materialEntries, slot.getSlotUBO(), worldFbo, window);
        }
    }

    private void pushEntries(
            Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialEntries,
            UBOInstance slotUBO,
            FboInstance worldFbo,
            WindowInstance window) {

        for (ObjectArrayList<RenderEntry> bucketList : materialEntries.values()) {
            for (int i = 0; i < bucketList.size(); i++) {
                ModelInstance model = bucketList.get(i).modelInstance;
                model.getMaterial().setUBO(slotUBO);
                renderManager.pushRenderCall(model, worldFbo, 0, window);
            }
        }
    }

    // Update \\

    public boolean addChunkInstance(WorldRenderInstance worldRenderInstance) {
        return updateEntries(worldRenderInstance, chunkEntries);
    }

    public boolean addMegaInstance(WorldRenderInstance worldRenderInstance) {
        return updateEntries(worldRenderInstance, megaEntries);
    }

    private boolean updateEntries(
            WorldRenderInstance worldRenderInstance,
            Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> entries) {

        long coordinate = worldRenderInstance.getCoordinate();

        if (!hasGridSlotForChunk(coordinate)) {
            removeEntries(coordinate, entries);
            return false;
        }

        DynamicPacketInstance dynamicPacket = worldRenderInstance.getDynamicPacketInstance();

        if (dynamicPacket.getState() != DynamicPacketState.READY)
            return false;

        Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialEntries = entries.get(coordinate);

        if (materialEntries == null) {
            materialEntries = new Int2ObjectOpenHashMap<>();
            entries.put(coordinate, materialEntries);
        }

        IntOpenHashSet seenMaterials = new IntOpenHashSet();

        for (Int2ObjectMap.Entry<ObjectArrayList<DynamicModelHandle>> entry : dynamicPacket
                .getMaterialID2ModelCollection().int2ObjectEntrySet()) {

            int materialID = entry.getIntKey();
            ObjectArrayList<DynamicModelHandle> sourceBuckets = entry.getValue();
            ObjectArrayList<RenderEntry> renderBuckets = materialEntries.get(materialID);

            if (renderBuckets == null) {
                renderBuckets = new ObjectArrayList<>();
                materialEntries.put(materialID, renderBuckets);
            }

            int liveCount = 0;

            for (int i = 0; i < sourceBuckets.size(); i++) {

                DynamicModelHandle bucket = sourceBuckets.get(i);

                if (bucket.isEmpty())
                    continue;

                if (liveCount < renderBuckets.size()) {
                    RenderEntry existing = renderBuckets.get(liveCount);
                    meshManager.updateMesh(existing.meshInstance, bucket.getVertices(), bucket.getIndices());
                    existing.modelInstance.updateMeshData(existing.meshInstance.getMeshData());
                } else {
                    MeshInstance meshInstance = meshManager.createMesh(
                            bucket.getVAOHandle(), bucket.getVertices(), bucket.getIndices());
                    MaterialInstance clonedMaterial = materialManager.cloneMaterial(materialID);
                    ModelInstance modelInstance = modelManager.createModel(meshInstance, clonedMaterial);
                    renderBuckets.add(new RenderEntry(meshInstance, modelInstance));
                }

                liveCount++;
            }

            while (renderBuckets.size() > liveCount)
                disposeEntry(renderBuckets.remove(renderBuckets.size() - 1));

            if (renderBuckets.isEmpty())
                materialEntries.remove(materialID);
            else
                seenMaterials.add(materialID);
        }

        var iterator = materialEntries.int2ObjectEntrySet().iterator();

        while (iterator.hasNext()) {
            var e = iterator.next();
            if (!seenMaterials.contains(e.getIntKey())) {
                disposeEntries(e.getValue());
                iterator.remove();
            }
        }

        if (materialEntries.isEmpty()) {
            entries.remove(coordinate);
            return false;
        }

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

    // Removal \\

    public void removeChunkInstance(long coordinate) {
        removeEntries(coordinate, chunkEntries);
    }

    public void removeMegaInstance(long coordinate) {
        removeEntries(coordinate, megaEntries);
    }

    private void removeEntries(
            long coordinate,
            Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> entries) {

        Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialEntries = entries.remove(coordinate);

        if (materialEntries == null)
            return;

        for (ObjectArrayList<RenderEntry> list : materialEntries.values())
            disposeEntries(list);
    }

    private void disposeEntry(RenderEntry entry) {
        modelManager.removeMesh(entry.modelInstance);
    }

    private void disposeEntries(ObjectArrayList<RenderEntry> list) {
        for (int i = 0; i < list.size(); i++)
            disposeEntry(list.get(i));
        list.clear();
    }

    // Render Entry \\

    private static final class RenderEntry {

        final MeshInstance meshInstance;
        final ModelInstance modelInstance;

        RenderEntry(MeshInstance meshInstance, ModelInstance modelInstance) {
            this.meshInstance = meshInstance;
            this.modelInstance = modelInstance;
        }
    }
}