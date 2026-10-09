package application.bootstrap.worldpipeline.worldrendermanager;

import application.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketState;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.weatherpipeline.cloudmanager.CloudManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.grid.WaterTargetStruct;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.bootstrap.worldpipeline.macrochunk.MacroChunkInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class WorldRenderManager extends ManagerPackage {

    /*
     * Owns the GPU representation of every rendered chunk, mega and macro.
     * updateEntries() reconciles a rebuilt packet bucket by bucket, reuploading
     * into the same buffers so per-window VAO clones stay valid, and released
     * entries are emptied and pooled per material so streaming never churns GPU
     * objects or garbage. A mega that has left its grid's mega queue keeps
     * standing in for its chunks until every one of them is individually on the
     * GPU, so switching representation never opens a hole. Every change to the
     * entries advances the drawn revision, so MacroRenderSystem, which owns
     * distant macro terrain, re-resolves which chunks a grid draws only when
     * that can have changed, and MacroWaterRenderSystem owns the grid's
     * distant sea plane. Water, any material reading OceanData, never enters
     * the G-buffer: it is drawn forward into the grid's water target, after
     * deferred lighting, with the grid's light and sky data and the scene it
     * refracts and reflects bound on each push. Every other material whose
     * shader reads the cloud noise, for the clouds' shadows, has it bound on
     * each push. pushSurfaceModel() and pushWaterModel() are the single call
     * sites for both, shared by chunks, megas and macros. Trees are entries
     * of their own beside the terrain — a chunk's full trees, a mega's
     * stand-ins — drawn whenever the terrain they belong to is, so a tree
     * that changes reuploads only itself. Until a mega's stand-ins land, the
     * trees its chunks still hold stand in for them, so no forest blinks out
     * as the player walks away from it.
     */

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private MeshManager meshManager;
    private RenderManager renderManager;
    private WorldStreamManager worldStreamManager;
    private CloudManager cloudManager;
    private FrustumCullingSystem frustumCullingSystem;
    private MacroRenderSystem macroRenderSystem;
    private MacroWaterRenderSystem macroWaterRenderSystem;

    // Entries
    private Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> chunkEntries;
    private Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> megaEntries;
    private LongOpenHashSet emptyChunks;

    // Tree Entries
    private Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> chunkTreeEntries;
    private Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> megaTreeEntries;
    private LongOpenHashSet drawnMegaTrees;

    // Stand-In — resolved once per grid per frame
    private LongOpenHashSet resolvedStandIns;
    private LongOpenHashSet standInMegas;

    // Water — resolved once per grid per frame
    private FBOInstance waterFbo;
    private int waterSceneColorTexture;
    private int waterSceneDepthTexture;
    private int waterSkyColorTexture;
    private int waterCloudColorTexture;
    private int waterCloudDistanceTexture;

    // Pools
    private Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialID2RenderEntryPool;
    private ObjectArrayList<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> materialEntriesPool;
    private ObjectArrayList<ObjectArrayList<RenderEntry>> bucketListPool;
    private int renderEntryPoolMax;

    // Scratch
    private IntOpenHashSet seenMaterials;
    private FloatArrayList emptyVertices;
    private ShortArrayList emptyIndices;

    // Settings
    private int batchedChunks;

    // Drawn State
    private int drawnRevision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.frustumCullingSystem = create(FrustumCullingSystem.class);
        this.macroRenderSystem = create(MacroRenderSystem.class);
        this.macroWaterRenderSystem = create(MacroWaterRenderSystem.class);

        // Entries
        this.chunkEntries = new Long2ObjectOpenHashMap<>();
        this.megaEntries = new Long2ObjectOpenHashMap<>();
        this.emptyChunks = new LongOpenHashSet();

        // Tree Entries
        this.chunkTreeEntries = new Long2ObjectOpenHashMap<>();
        this.megaTreeEntries = new Long2ObjectOpenHashMap<>();
        this.drawnMegaTrees = new LongOpenHashSet();

        // Stand-In
        this.resolvedStandIns = new LongOpenHashSet();
        this.standInMegas = new LongOpenHashSet();

        // Pools
        this.materialID2RenderEntryPool = new Int2ObjectOpenHashMap<>();
        this.materialEntriesPool = new ObjectArrayList<>();
        this.bucketListPool = new ObjectArrayList<>();
        this.renderEntryPoolMax = EngineSetting.WORLD_RENDER_ENTRY_POOL_MAX_PER_MATERIAL;

        // Scratch
        this.seenMaterials = new IntOpenHashSet();
        this.emptyVertices = new FloatArrayList();
        this.emptyIndices = new ShortArrayList();

        // Settings
        this.batchedChunks = EngineSetting.MEGA_CHUNK_SIZE * EngineSetting.MEGA_CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.meshManager = get(MeshManager.class);
        this.renderManager = get(RenderManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.cloudManager = get(CloudManager.class);
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
            FBOInstance worldFbo = grid.getRenderTargetFbo();

            if (window == null || worldFbo == null)
                continue;

            frustumCullingSystem.refresh(grid);
            resolveWaterTarget(grid);

            macroRenderSystem.renderGridMacros(grid, window, worldFbo);

            if (waterFbo != null)
                macroWaterRenderSystem.renderGridWater(grid, window);

            renderGridMegas(grid, window, worldFbo);
            renderGridChunks(grid, window, worldFbo);
        }
    }

    private void renderGridMegas(GridInstance grid, WindowInstance window, FBOInstance worldFbo) {

        Long2ObjectLinkedOpenHashMap<GridSlotHandle> megaQueue = grid.getMegaRenderQueue();
        ObjectIterator<Long2ObjectMap.Entry<GridSlotHandle>> iterator = megaQueue.long2ObjectEntrySet().fastIterator();

        while (iterator.hasNext()) {

            Long2ObjectMap.Entry<GridSlotHandle> queued = iterator.next();
            long coordinate = queued.getLongKey();
            GridSlotHandle slot = queued.getValue();

            if (!frustumCullingSystem.isMegaVisible(slot))
                continue;

            Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialEntries = megaEntries.get(coordinate);

            if (materialEntries == null) {
                renderCoveredChunksIndividually(slot, grid, window, worldFbo);
                continue;
            }

            pushEntries(materialEntries, slot.getSlotUBO(), grid, worldFbo, window);

            if (drawnMegaTrees.contains(coordinate))
                pushTrees(megaTreeEntries, coordinate, slot.getSlotUBO(), grid, worldFbo, window);
            else
                pushCoveredChunkTrees(slot, grid, window, worldFbo);
        }
    }

    // A mega whose stand-ins are not on the GPU yet shows whatever trees its chunks still hold
    private void pushCoveredChunkTrees(
            GridSlotHandle megaSlot,
            GridInstance grid,
            WindowInstance window,
            FBOInstance worldFbo) {

        ObjectArrayList<GridSlotHandle> coveredSlots = megaSlot.getCoveredSlots();

        for (int i = 0; i < coveredSlots.size(); i++) {

            GridSlotHandle coveredSlot = coveredSlots.get(i);

            if (frustumCullingSystem.isChunkVisible(coveredSlot))
                pushTrees(chunkTreeEntries, coveredSlot.getChunkCoordinate(), coveredSlot.getSlotUBO(), grid,
                        worldFbo, window);
        }
    }

    private void renderCoveredChunksIndividually(
            GridSlotHandle megaSlot,
            GridInstance grid,
            WindowInstance window,
            FBOInstance worldFbo) {

        ObjectArrayList<GridSlotHandle> coveredSlots = megaSlot.getCoveredSlots();

        for (int i = 0; i < coveredSlots.size(); i++) {

            GridSlotHandle coveredSlot = coveredSlots.get(i);

            if (!frustumCullingSystem.isChunkVisible(coveredSlot))
                continue;

            pushChunk(coveredSlot.getChunkCoordinate(), coveredSlot.getSlotUBO(), grid, worldFbo, window);
        }
    }

    private void renderGridChunks(GridInstance grid, WindowInstance window, FBOInstance worldFbo) {

        Long2ObjectLinkedOpenHashMap<GridSlotHandle> chunkQueue = grid.getChunkRenderQueue();
        Long2ObjectLinkedOpenHashMap<GridSlotHandle> megaQueue = grid.getMegaRenderQueue();
        ObjectIterator<Long2ObjectMap.Entry<GridSlotHandle>> iterator = chunkQueue.long2ObjectEntrySet().fastIterator();

        resolvedStandIns.clear();
        standInMegas.clear();

        while (iterator.hasNext()) {

            Long2ObjectMap.Entry<GridSlotHandle> queued = iterator.next();
            long coordinate = queued.getLongKey();
            GridSlotHandle slot = queued.getValue();
            long megaCoordinate = Coordinate2Long.toMegaChunkCoordinate(coordinate);

            if (megaQueue.containsKey(megaCoordinate))
                continue;

            if (renderStandInMega(grid, megaCoordinate, window, worldFbo))
                continue;

            if (!frustumCullingSystem.isChunkVisible(slot))
                continue;

            pushChunk(coordinate, slot.getSlotUBO(), grid, worldFbo, window);
        }
    }

    // A chunk drawn on its own, its trees with it once its ground is on the GPU — ground with no geometry included
    private void pushChunk(
            long coordinate,
            UBOInstance slotUBO,
            GridInstance grid,
            FBOInstance worldFbo,
            WindowInstance window) {

        Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialEntries = chunkEntries.get(coordinate);

        if (materialEntries == null && !emptyChunks.contains(coordinate))
            return;

        if (materialEntries != null)
            pushEntries(materialEntries, slotUBO, grid, worldFbo, window);

        pushTrees(chunkTreeEntries, coordinate, slotUBO, grid, worldFbo, window);
    }

    // A standing-in mega is drawn once, the first time one of its chunks comes up, and covers all of them
    private boolean renderStandInMega(
            GridInstance grid,
            long megaCoordinate,
            WindowInstance window,
            FBOInstance worldFbo) {

        if (!resolvedStandIns.add(megaCoordinate))
            return standInMegas.contains(megaCoordinate);

        if (!isMegaStandingIn(grid, megaCoordinate))
            return false;

        standInMegas.add(megaCoordinate);

        GridSlotHandle megaSlot = grid.getGridSlotForChunk(megaCoordinate);

        if (frustumCullingSystem.isMegaVisible(megaSlot)) {
            pushEntries(megaEntries.get(megaCoordinate), megaSlot.getSlotUBO(), grid, worldFbo, window);
            pushTrees(megaTreeEntries, megaCoordinate, megaSlot.getSlotUBO(), grid, worldFbo, window);
        }

        return true;
    }

    // The trees standing on a chunk or mega, when it has any on the GPU
    private void pushTrees(
            Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> treeEntries,
            long coordinate,
            UBOInstance slotUBO,
            GridInstance grid,
            FBOInstance worldFbo,
            WindowInstance window) {

        Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialEntries = treeEntries.get(coordinate);

        if (materialEntries != null)
            pushEntries(materialEntries, slotUBO, grid, worldFbo, window);
    }

    private void pushEntries(
            Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialEntries,
            UBOInstance slotUBO,
            GridInstance grid,
            FBOInstance worldFbo,
            WindowInstance window) {

        ObjectIterator<Int2ObjectMap.Entry<ObjectArrayList<RenderEntry>>> iterator = materialEntries
                .int2ObjectEntrySet()
                .fastIterator();

        while (iterator.hasNext()) {

            ObjectArrayList<RenderEntry> bucketList = iterator.next().getValue();

            for (int i = 0; i < bucketList.size(); i++) {

                RenderEntry entry = bucketList.get(i);
                entry.modelInstance.getMaterial().setUBO(slotUBO);

                if (entry.usesOceanData)
                    pushWaterModel(entry.modelInstance, grid, window);
                else
                    pushSurfaceModel(entry.modelInstance, worldFbo, window);
            }
        }
    }

    // Surface \\

    void pushSurfaceModel(ModelInstance modelInstance, FBOInstance worldFbo, WindowInstance window) {

        MaterialInstance material = modelInstance.getMaterial();

        if (material.getUniform(EngineSetting.UNIFORM_CLOUD_NOISE) != null)
            material.setUniform(EngineSetting.UNIFORM_CLOUD_NOISE, cloudManager.getCloudNoiseTexture());

        renderManager.pushRenderCall(modelInstance, worldFbo, EngineSetting.DEFAULT_RENDER_DEPTH, window);
    }

    // Water \\

    private void resolveWaterTarget(GridInstance grid) {

        WaterTargetStruct waterTarget = grid.getWaterTarget();

        if (waterTarget == null) {
            waterFbo = null;
            return;
        }

        waterFbo = waterTarget.getWaterFbo();
        waterSceneColorTexture = waterTarget.getSceneColorTexture();
        waterSceneDepthTexture = waterTarget.getSceneDepthTexture();
        waterSkyColorTexture = waterTarget.getSkyColorTexture();
        waterCloudColorTexture = waterTarget.getCloudColorTexture();
        waterCloudDistanceTexture = waterTarget.getCloudDistanceTexture();
    }

    void pushWaterModel(ModelInstance modelInstance, GridInstance grid, WindowInstance window) {

        if (waterFbo == null)
            return;

        MaterialInstance material = modelInstance.getMaterial();

        material.setUBO(grid.getOceanDataUBO());
        material.setUBO(grid.getSunLightUBO());
        material.setUBO(grid.getMoonLightUBO());
        material.setUBO(grid.getSkyColorUBO());

        material.setUniform(EngineSetting.UNIFORM_WATER_SCENE_COLOR, waterSceneColorTexture);
        material.setUniform(EngineSetting.UNIFORM_WATER_SCENE_DEPTH, waterSceneDepthTexture);
        material.setUniform(EngineSetting.UNIFORM_WATER_SKY_COLOR, waterSkyColorTexture);
        material.setUniform(EngineSetting.UNIFORM_WATER_CLOUD_COLOR, waterCloudColorTexture);
        material.setUniform(EngineSetting.UNIFORM_WATER_CLOUD_DISTANCE, waterCloudDistanceTexture);

        renderManager.pushRenderCall(modelInstance, waterFbo, EngineSetting.DEFAULT_RENDER_DEPTH, window);
    }

    // Update \\

    // A merged chunk with no geometry is rendered as nothing, so it never stalls a handoff or retries its upload
    public boolean addChunkInstance(WorldRenderInstance worldRenderInstance) {

        long coordinate = worldRenderInstance.getCoordinate();

        if (worldRenderInstance.getDynamicPacketInstance().getState() != DynamicPacketState.EMPTY) {

            if (emptyChunks.remove(coordinate))
                drawnRevision++;

            return updateEntries(coordinate, worldRenderInstance.getDynamicPacketInstance(), chunkEntries);
        }

        if (!hasGridSlotForChunk(coordinate))
            return false;

        removeEntries(coordinate, chunkEntries);

        if (emptyChunks.add(coordinate))
            drawnRevision++;

        return true;
    }

    public boolean addMegaInstance(WorldRenderInstance worldRenderInstance) {
        return updateEntries(
                worldRenderInstance.getCoordinate(), worldRenderInstance.getDynamicPacketInstance(), megaEntries);
    }

    // Trees \\

    // A chunk's tree packet on the GPU, or its old trees taken down when it holds none — false only when no grid
    // draws the chunk any more
    public boolean addChunkTrees(long coordinate, DynamicPacketInstance treePacket) {
        return updateTrees(coordinate, treePacket, chunkTreeEntries);
    }

    public void removeChunkTrees(long coordinate) {
        removeEntries(coordinate, chunkTreeEntries);
    }

    public boolean addMegaTrees(long coordinate, DynamicPacketInstance treePacket) {

        if (!updateTrees(coordinate, treePacket, megaTreeEntries))
            return false;

        drawnMegaTrees.add(coordinate);

        return true;
    }

    // True once a mega's stand-ins are on the GPU, none at all included
    public boolean isMegaTreesDrawn(long megaCoordinate) {
        return drawnMegaTrees.contains(megaCoordinate);
    }

    private boolean updateTrees(
            long coordinate,
            DynamicPacketInstance treePacket,
            Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> treeEntries) {

        if (!hasGridSlotForChunk(coordinate)) {
            removeEntries(coordinate, treeEntries);
            return false;
        }

        if (treePacket.getState() != DynamicPacketState.READY) {
            removeEntries(coordinate, treeEntries);
            return true;
        }

        updateEntries(coordinate, treePacket, treeEntries);

        return true;
    }

    // Macro \\

    public void placeMacroInstance(MacroChunkInstance macro, GridInstance grid) {
        macroRenderSystem.placeMacro(macro, grid);
    }

    public void addMacroInstance(MacroChunkInstance macro, GridInstance grid) {
        macroRenderSystem.uploadMacro(macro);
        macroWaterRenderSystem.writeMask(macro, grid);
    }

    public void removeMacroInstance(MacroChunkInstance macro, GridInstance grid) {
        macroRenderSystem.hideMacro(macro);
        macroWaterRenderSystem.clearMask(macro, grid);
    }

    public void disposeMacroInstance(MacroChunkInstance macro) {
        macroRenderSystem.disposeMacro(macro);
    }

    public void disposeMacroWater(GridInstance grid) {
        macroWaterRenderSystem.disposeWater(grid);
    }

    // Mega Readiness \\

    public boolean isMegaRendered(long megaCoordinate) {
        return megaEntries.containsKey(megaCoordinate);
    }

    // Mega Handoff \\

    // A mega outside the queue still on the GPU covers its whole footprint until each queued chunk is uploaded
    public boolean isMegaStandingIn(GridInstance grid, long megaCoordinate) {

        if (grid.getMegaRenderQueue().containsKey(megaCoordinate) || !megaEntries.containsKey(megaCoordinate))
            return false;

        GridSlotHandle megaSlot = grid.getGridSlotForChunk(megaCoordinate);

        if (megaSlot == null)
            return false;

        ObjectArrayList<GridSlotHandle> coveredSlots = megaSlot.getCoveredSlots();

        if (coveredSlots.size() != batchedChunks)
            return false;

        Long2ObjectLinkedOpenHashMap<GridSlotHandle> chunkQueue = grid.getChunkRenderQueue();

        for (int i = 0; i < coveredSlots.size(); i++) {

            long chunkCoordinate = coveredSlots.get(i).getChunkCoordinate();

            if (chunkQueue.containsKey(chunkCoordinate) && !isChunkRendered(chunkCoordinate))
                return true;
        }

        return false;
    }

    private boolean isChunkRendered(long chunkCoordinate) {
        return chunkEntries.containsKey(chunkCoordinate) || emptyChunks.contains(chunkCoordinate);
    }

    // Drawn State \\

    int getDrawnRevision() {
        return drawnRevision;
    }

    boolean isChunkDrawn(GridInstance grid, long chunkCoordinate) {

        if (grid.getGridSlotForChunk(chunkCoordinate) == null)
            return false;

        long megaCoordinate = Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate);

        if (grid.getMegaRenderQueue().containsKey(megaCoordinate) && megaEntries.containsKey(megaCoordinate))
            return true;

        if (isMegaStandingIn(grid, megaCoordinate))
            return true;

        return isChunkRendered(chunkCoordinate);
    }

    // Entries \\

    private boolean updateEntries(
            long coordinate,
            DynamicPacketInstance dynamicPacket,
            Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> entries) {

        if (!hasGridSlotForChunk(coordinate)) {
            removeEntries(coordinate, entries);
            return false;
        }

        if (dynamicPacket.getState() != DynamicPacketState.READY)
            return false;

        drawnRevision++;

        Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialEntries = entries.get(coordinate);

        if (materialEntries == null) {
            materialEntries = acquireMaterialEntries();
            entries.put(coordinate, materialEntries);
        }

        seenMaterials.clear();

        ObjectIterator<Int2ObjectMap.Entry<ObjectArrayList<DynamicModelHandle>>> sourceIterator = dynamicPacket
                .getMaterialID2ModelCollection()
                .int2ObjectEntrySet()
                .fastIterator();

        while (sourceIterator.hasNext()) {

            Int2ObjectMap.Entry<ObjectArrayList<DynamicModelHandle>> entry = sourceIterator.next();
            int materialID = entry.getIntKey();
            ObjectArrayList<DynamicModelHandle> sourceBuckets = entry.getValue();
            ObjectArrayList<RenderEntry> renderBuckets = materialEntries.get(materialID);

            if (renderBuckets == null) {
                renderBuckets = acquireBucketList();
                materialEntries.put(materialID, renderBuckets);
            }

            int liveCount = 0;

            for (int i = 0; i < sourceBuckets.size(); i++) {

                DynamicModelHandle bucket = sourceBuckets.get(i);

                if (bucket.isEmpty())
                    continue;

                if (liveCount < renderBuckets.size())
                    uploadEntry(renderBuckets.get(liveCount), bucket);
                else
                    renderBuckets.add(acquireEntry(materialID, bucket));

                liveCount++;
            }

            while (renderBuckets.size() > liveCount)
                releaseEntry(renderBuckets.remove(renderBuckets.size() - 1));

            if (renderBuckets.isEmpty()) {
                materialEntries.remove(materialID);
                bucketListPool.push(renderBuckets);
            } else
                seenMaterials.add(materialID);
        }

        ObjectIterator<Int2ObjectMap.Entry<ObjectArrayList<RenderEntry>>> iterator = materialEntries
                .int2ObjectEntrySet()
                .fastIterator();

        while (iterator.hasNext()) {

            Int2ObjectMap.Entry<ObjectArrayList<RenderEntry>> entry = iterator.next();

            if (seenMaterials.contains(entry.getIntKey()))
                continue;

            releaseBucketList(entry.getValue());
            iterator.remove();
        }

        if (materialEntries.isEmpty()) {
            entries.remove(coordinate);
            materialEntriesPool.push(materialEntries);
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

        if (emptyChunks.remove(coordinate))
            drawnRevision++;

        removeEntries(coordinate, chunkEntries);
    }

    // A mega's stand-in trees leave with it
    public void removeMegaInstance(long coordinate) {
        removeEntries(coordinate, megaEntries);
        removeEntries(coordinate, megaTreeEntries);
        drawnMegaTrees.remove(coordinate);
    }

    private void removeEntries(
            long coordinate,
            Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>>> entries) {

        Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialEntries = entries.remove(coordinate);

        if (materialEntries == null)
            return;

        drawnRevision++;
        releaseMaterialEntries(materialEntries);
    }

    // Pools \\

    private Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> acquireMaterialEntries() {
        return materialEntriesPool.isEmpty() ? new Int2ObjectOpenHashMap<>() : materialEntriesPool.pop();
    }

    private void releaseMaterialEntries(Int2ObjectOpenHashMap<ObjectArrayList<RenderEntry>> materialEntries) {

        ObjectIterator<Int2ObjectMap.Entry<ObjectArrayList<RenderEntry>>> iterator = materialEntries
                .int2ObjectEntrySet()
                .fastIterator();

        while (iterator.hasNext())
            releaseBucketList(iterator.next().getValue());

        materialEntries.clear();
        materialEntriesPool.push(materialEntries);
    }

    private ObjectArrayList<RenderEntry> acquireBucketList() {
        return bucketListPool.isEmpty() ? new ObjectArrayList<>() : bucketListPool.pop();
    }

    private void releaseBucketList(ObjectArrayList<RenderEntry> bucketList) {

        for (int i = 0; i < bucketList.size(); i++)
            releaseEntry(bucketList.get(i));

        bucketList.clear();
        bucketListPool.push(bucketList);
    }

    private RenderEntry acquireEntry(int materialID, DynamicModelHandle bucket) {

        ObjectArrayList<RenderEntry> pool = materialID2RenderEntryPool.get(materialID);

        while (pool != null && !pool.isEmpty()) {

            RenderEntry pooled = pool.pop();

            if (pooled.vaoHandle == bucket.getVAOHandle()) {
                uploadEntry(pooled, bucket);
                return pooled;
            }

            disposeEntry(pooled);
        }

        MeshInstance meshInstance = meshManager.createMesh(
                bucket.getVAOHandle(), bucket.getVertices(), bucket.getIndices());
        MaterialInstance clonedMaterial = materialManager.cloneMaterial(materialID);
        ModelInstance modelInstance = modelManager.createModel(meshInstance, clonedMaterial);

        return new RenderEntry(bucket.getVAOHandle(), meshInstance, modelInstance);
    }

    // A pooled entry keeps its buffer handles but gives up its storage until its next tenant uploads
    private void releaseEntry(RenderEntry entry) {

        int materialID = entry.modelInstance.getMaterial().getMaterialID();
        ObjectArrayList<RenderEntry> pool = materialID2RenderEntryPool.get(materialID);

        if (pool == null) {
            pool = new ObjectArrayList<>();
            materialID2RenderEntryPool.put(materialID, pool);
        }

        if (pool.size() >= renderEntryPoolMax) {
            disposeEntry(entry);
            return;
        }

        meshManager.updateMesh(entry.meshInstance, emptyVertices, emptyIndices);
        entry.modelInstance.updateMeshData(entry.meshInstance.getMeshData());
        pool.push(entry);
    }

    private void uploadEntry(RenderEntry entry, DynamicModelHandle bucket) {
        meshManager.updateMesh(entry.meshInstance, bucket.getVertices(), bucket.getIndices());
        entry.modelInstance.updateMeshData(entry.meshInstance.getMeshData());
    }

    private void disposeEntry(RenderEntry entry) {
        modelManager.removeMesh(entry.modelInstance);
    }

    // Render Entry \\

    private static final class RenderEntry {

        final VAOHandle vaoHandle;
        final MeshInstance meshInstance;
        final ModelInstance modelInstance;
        final boolean usesOceanData;

        RenderEntry(VAOHandle vaoHandle, MeshInstance meshInstance, ModelInstance modelInstance) {
            this.vaoHandle = vaoHandle;
            this.meshInstance = meshInstance;
            this.modelInstance = modelInstance;
            this.usesOceanData = modelInstance.getMaterial().getShaderHandle()
                    .getCompiledUBOBlockNames().contains(EngineSetting.OCEAN_DATA_UBO);
        }
    }
}
