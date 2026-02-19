package com.internal.bootstrap.worldpipeline.worldrendersystem;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicModelHandle;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketState;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.cameramanager.CameraManager;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldRenderSystem extends SystemPackage {

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private RenderSystem renderSystem;
    private CameraManager cameraManager;
    private PlayerManager playerManager;
    private GridManager gridManager;

    // GPU data
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelHandle>> chunkModels;
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelHandle>> megaModels;

    // Render queues
    private Long2ObjectLinkedOpenHashMap<GridSlotHandle> chunkRenderQueue;
    private Long2ObjectLinkedOpenHashMap<GridSlotHandle> megaRenderQueue;

    // Culling constants
    private static final float HALF_PI = (float) (Math.PI / 2f);
    private static final float TWO_PI = (float) (Math.PI * 2f);
    private static final float PI = (float) Math.PI;
    private float megaAngularBleedBase;

    // Internal \\

    @Override
    protected void create() {
        this.chunkModels = new Long2ObjectOpenHashMap<>();
        this.megaModels = new Long2ObjectOpenHashMap<>();
        this.chunkRenderQueue = new Long2ObjectLinkedOpenHashMap<>();
        this.megaRenderQueue = new Long2ObjectLinkedOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.renderSystem = get(RenderSystem.class);
        this.gridManager = get(GridManager.class);
        this.cameraManager = get(CameraManager.class);
        this.playerManager = get(PlayerManager.class);
    }

    @Override
    protected void awake() {
        float megaSize = EngineSetting.MEGA_CHUNK_SIZE;
        this.megaAngularBleedBase = (float) Math.sqrt(megaSize * megaSize + megaSize * megaSize) / 2f;
    }

    @Override
    protected void update() {
        renderWorld();
    }

    // Render \\

    private void renderWorld() {

        CameraInstance camera = cameraManager.getMainCamera();

        // Horizontal compass direction the camera faces
        float cameraAngle = getCameraAngle(camera);
        float halfFov = getHalfFov(camera);

        // How far we've tilted from the horizon (0 = looking straight, PI/2 = straight
        // down/up)
        float absPitch = getAbsPitch(camera);

        // The horizontal arc of the visible cone.
        // At pitch=0: equals halfFov (tight forward cone).
        // As pitch approaches PI/2: expands toward PI (full 360 circle),
        // because every horizontal direction is "ahead" when looking straight down.
        float effectiveAngle = getEffectiveHalfAngle(halfFov, absPitch);
        boolean fullCircle = effectiveAngle >= PI;

        for (long coordinate : megaRenderQueue.keySet())
            renderMega(coordinate, cameraAngle, effectiveAngle, fullCircle);

        for (long coordinate : chunkRenderQueue.keySet())
            renderChunk(coordinate, cameraAngle, effectiveAngle, fullCircle);
    }

    private void renderMega(long megaCoordinate, float cameraAngle, float effectiveAngle, boolean fullCircle) {

        GridSlotHandle gridSlotHandle = megaRenderQueue.get(megaCoordinate);

        if (!isMegaVisible(gridSlotHandle, cameraAngle, effectiveAngle, fullCircle))
            return;

        ObjectArrayList<ModelHandle> models = megaModels.get(megaCoordinate);
        if (models == null)
            return;

        UBOHandle uboHandle = gridSlotHandle.getSlotUBO();
        for (ModelHandle model : models) {
            model.getMaterial().setUBO(EngineSetting.GRID_COORDINATE_UBO, uboHandle);
            renderSystem.pushRenderCall(model, 0);
        }
    }

    private void renderChunk(long chunkCoordinate, float cameraAngle, float effectiveAngle, boolean fullCircle) {

        GridSlotHandle gridSlotHandle = chunkRenderQueue.get(chunkCoordinate);

        if (megaRenderQueue.containsKey(gridSlotHandle.getMegaCoordinate()))
            return;

        if (!isChunkVisible(gridSlotHandle, cameraAngle, effectiveAngle, fullCircle))
            return;

        ObjectArrayList<ModelHandle> models = chunkModels.get(chunkCoordinate);
        if (models == null)
            return;

        UBOHandle uboHandle = gridSlotHandle.getSlotUBO();
        for (ModelHandle model : models) {
            model.getMaterial().setUBO(EngineSetting.GRID_COORDINATE_UBO, uboHandle);
            renderSystem.pushRenderCall(model, 0);
        }
    }

    // Culling \\
    //
    // The grid already only holds chunks within maxRenderDistance — the grid
    // boundary
    // IS the distance limit. No separate distance check is needed here. We only
    // need
    // to know whether each chunk falls inside the horizontal view cone.

    private boolean isChunkVisible(GridSlotHandle slot, float cameraAngle, float effectiveAngle, boolean fullCircle) {

        // Chunks right underfoot are always visible
        if (slot.getDistanceFromCenter() <= 4f)
            return true;

        // Looking straight down/up — all horizontal directions visible
        if (fullCircle)
            return true;

        // Nearby chunks subtend a wider angle so they need more bleed to avoid edge
        // popping
        float bleed = 0.75f / (float) Math.sqrt(slot.getDistanceFromCenter());

        return isWithinAngle(slot.getAngleFromCenter(), cameraAngle, effectiveAngle + bleed);
    }

    private boolean isMegaVisible(GridSlotHandle slot, float cameraAngle, float effectiveAngle, boolean fullCircle) {

        if (slot.getDistanceFromCenter() <= 4f)
            return true;

        if (fullCircle)
            return true;

        float distance = (float) Math.sqrt(slot.getDistanceFromCenter());
        float megaBleed = (float) Math.atan(megaAngularBleedBase / distance);

        return isWithinAngle(slot.getAngleFromCenter(), cameraAngle, effectiveAngle + megaBleed);
    }

    private boolean isWithinAngle(float slotAngle, float cameraAngle, float tolerance) {

        float diff = slotAngle - cameraAngle;

        if (diff > PI)
            diff -= TWO_PI;
        if (diff < -PI)
            diff += TWO_PI;

        return Math.abs(diff) <= tolerance;
    }

    // Camera Math \\

    /**
     * Horizontal compass bearing the camera faces, in radians.
     * Uses atan2(-z, x) to match the grid's atan2(gridY, gridX) convention,
     * assuming world +X = grid +X and world -Z = grid +Y.
     */
    private float getCameraAngle(CameraInstance camera) {
        return (float) Math.atan2(-camera.getDirection().z, camera.getDirection().x);
    }

    private float getHalfFov(CameraInstance camera) {
        return (float) Math.toRadians(camera.getFOV() / 2f);
    }

    /**
     * Recover absolute pitch from the direction vector.
     * CameraInstance sets direction.y = -sin(pitch), so we negate to recover pitch.
     * Returns [0, PI/2]: 0 = horizon, PI/2 = straight up or down.
     */
    private float getAbsPitch(CameraInstance camera) {
        float sinPitch = Math.max(-1f, Math.min(1f, -camera.getDirection().y));
        return Math.abs((float) Math.asin(sinPitch));
    }

    /**
     * Compute the effective horizontal half-angle of the visible cone.
     *
     * Derivation: a view cone with vertical half-angle α, tilted at pitch θ below
     * the horizon, intersects the XZ plane in a horizontal arc of:
     * atan(tan(α) / cos(θ))
     *
     * pitch = 0 → atan(tan(α) / 1) = α (normal FOV, tight cone forward)
     * pitch = 90° → cos(θ) → 0, result → PI (full 360°, looking straight down)
     *
     * Returns PI as sentinel when cos(pitch) < 0.01 (essentially vertical).
     * Caller uses the fullCircle flag to skip the angle test entirely in that case.
     */
    private float getEffectiveHalfAngle(float halfFov, float absPitch) {
        float cosP = (float) Math.cos(absPitch);
        if (cosP < 0.01f)
            return PI;
        return (float) Math.atan(Math.tan(halfFov) / cosP);
    }

    // Render Queue \\

    public void rebuildRenderQueue() {

        clearRenderQueue();

        GridInstance gridInstance = gridManager.getGrid();

        for (int i = 0; i < gridInstance.getTotalSlots(); i++) {

            long gridCoordinate = gridInstance.getGridCoordinate(i);
            GridSlotHandle gridSlotHandle = gridInstance.getGridSlot(gridCoordinate);

            queueChunk(gridSlotHandle);

            if (gridSlotHandle.getDetailLevel().renderMode == RenderType.BATCHED)
                queueMega(gridSlotHandle);
        }
    }

    private void queueChunk(GridSlotHandle gridSlotHandle) {

        if (megaRenderQueue.containsKey(gridSlotHandle.getMegaCoordinate()))
            return;

        chunkRenderQueue.put(gridSlotHandle.getChunkCoordinate(), gridSlotHandle);
    }

    private void queueMega(GridSlotHandle gridSlotHandle) {

        if (gridSlotHandle.getChunkCoordinate() != gridSlotHandle.getMegaCoordinate())
            return;

        megaRenderQueue.put(gridSlotHandle.getMegaCoordinate(), gridSlotHandle);

        for (GridSlotHandle coveredSlot : gridSlotHandle.getCoveredSlots())
            chunkRenderQueue.remove(coveredSlot.getChunkCoordinate());
    }

    private void clearRenderQueue() {
        chunkRenderQueue.clear();
        megaRenderQueue.clear();
    }

    // World Render System \\

    public boolean addChunkInstance(WorldRenderInstance worldRenderInstance) {

        if (worldRenderInstance.getGridSlotHandle() == null)
            return false;

        long coordinate = worldRenderInstance.getCoordinate();

        if (chunkModels.containsKey(coordinate))
            removeChunkInstance(coordinate);

        ObjectArrayList<ModelHandle> modelList = buildModelList(worldRenderInstance);
        if (modelList == null)
            return false;

        chunkModels.put(coordinate, modelList);
        return true;
    }

    public boolean addMegaInstance(WorldRenderInstance worldRenderInstance) {

        if (worldRenderInstance.getGridSlotHandle() == null)
            return false;

        long coordinate = worldRenderInstance.getCoordinate();

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