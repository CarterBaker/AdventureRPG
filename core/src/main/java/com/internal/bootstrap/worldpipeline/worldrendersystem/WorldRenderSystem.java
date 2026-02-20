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
    private static final float TWO_PI = (float) (Math.PI * 2f);
    private static final float PI = (float) Math.PI;

    // Minimum angular bleed so far-away chunks at the cone edge never get zero
    // tolerance.
    // Raise this if you still see gaps; lower it if the cone feels too fat at
    // distance.
    private static final float MIN_BLEED = 0.05f;

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

        float cameraAngle = getCameraAngle(camera);
        float halfFov = getHalfFov(camera);
        float absPitch = getAbsPitch(camera);
        float effectiveAngle = getEffectiveHalfAngle(halfFov, absPitch);
        boolean fullCircle = effectiveAngle >= PI;

        for (long coordinate : megaRenderQueue.keySet())
            renderMega(coordinate, cameraAngle, effectiveAngle, fullCircle);

        for (long coordinate : chunkRenderQueue.keySet())
            renderChunk(coordinate, cameraAngle, effectiveAngle, fullCircle);
    }

    private void renderMega(long megaCoordinate, float cameraAngle, float effectiveAngle, boolean fullCircle) {

        GridSlotHandle slot = megaRenderQueue.get(megaCoordinate);

        if (!isMegaVisible(slot, cameraAngle, effectiveAngle, fullCircle))
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

    private void renderChunk(long chunkCoordinate, float cameraAngle, float effectiveAngle, boolean fullCircle) {

        GridSlotHandle slot = chunkRenderQueue.get(chunkCoordinate);

        if (megaRenderQueue.containsKey(slot.getMegaCoordinate()))
            return;

        if (!isChunkVisible(slot, cameraAngle, effectiveAngle, fullCircle))
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

    // Culling \\

    private boolean isChunkVisible(GridSlotHandle slot, float cameraAngle, float effectiveAngle, boolean fullCircle) {

        float distanceSq = slot.getChunkDistanceFromCenter();

        if (distanceSq <= 4f || fullCircle)
            return true;

        float distance = (float) Math.sqrt(distanceSq);

        // 0.75/distance gives wide tolerance up close, shrinking at range.
        // MIN_BLEED floors it so edge chunks at max distance still get enough tolerance
        // to never leave gaps between the last visible chunk and the cone boundary.
        float bleed = Math.max(MIN_BLEED, 0.75f / distance);

        return isWithinAngle(slot.getChunkAngleFromCenter(), cameraAngle, effectiveAngle + bleed);
    }

    private boolean isMegaVisible(GridSlotHandle slot, float cameraAngle, float effectiveAngle, boolean fullCircle) {

        float distanceSq = slot.getMegaDistanceFromCenter();

        if (distanceSq <= 4f || fullCircle)
            return true;

        float distance = (float) Math.sqrt(distanceSq);

        // atan naturally shrinks at range just like the chunk bleed — floor it the same
        // way
        float megaBleed = Math.max(MIN_BLEED, (float) Math.atan(megaAngularBleedBase / distance));

        return isWithinAngle(slot.getMegaAngleFromCenter(), cameraAngle, effectiveAngle + megaBleed);
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

    private float getCameraAngle(CameraInstance camera) {
        return (float) Math.atan2(camera.getDirection().z, camera.getDirection().x);
    }

    /**
     * LibGDX PerspectiveCamera.fieldOfView is vertical FOV.
     * Convert to horizontal half-FOV using the viewport aspect ratio so the cone
     * correctly covers the full screen width regardless of resolution or window
     * shape.
     *
     * tan(hHalfFov) = tan(vHalfFov) * (width / height)
     */
    private float getHalfFov(CameraInstance camera) {
        float verticalHalfFov = (float) Math.toRadians(camera.getFOV() / 2f);
        float aspectRatio = camera.getViewport().x / camera.getViewport().y;
        return (float) Math.atan(Math.tan(verticalHalfFov) * aspectRatio);
    }

    private float getAbsPitch(CameraInstance camera) {
        float sinPitch = Math.max(-1f, Math.min(1f, -camera.getDirection().y));
        return Math.abs((float) Math.asin(sinPitch));
    }

    /**
     * Expand the horizontal cone as pitch increases.
     * pitch = 0 → halfFov (normal tight forward cone)
     * pitch = 90 → PI (full circle, looking straight down/up)
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