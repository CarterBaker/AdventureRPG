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
    private static final float HALF_PI = (float) (Math.PI / 2f);

    // Minimum angular bleed so far chunks at the cone edge never gap.
    private static final float MIN_BLEED = 0.05f;

    // Minimum render radius (squared) at extreme 90 degree pitch.
    // Radius of 5 chunks = 25 squared. Always less than fullCenterRadiusSq on any
    // real grid.
    private static final float PITCH_MIN_DIST_SQ = 25f;

    // Angle widens fast so there are no holes even at moderate downward angles.
    // 2 = already halfway to full circle at 45 degrees down.
    private static final float PITCH_POWER_ANGLE = 1f;

    // Distance stays large much longer for performance, only pulls back sharply
    // near vertical.
    // 6 = barely shrinks until ~70 degrees, then drops hard near 90.
    private static final float PITCH_POWER_DISTANCE = 6f;

    // Max render radius in center-offset chunk units, derived from settings in
    // awake.
    private float fullCenterRadiusSq;
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

        // Max render radius from settings, padded by halfMega to cover furthest mega
        // center in the same center-offset unit system that slot distances use.
        float halfMega = megaSize / 2f;
        float centerRadius = (settings.maxRenderDistance / 2f) + halfMega;
        this.fullCenterRadiusSq = centerRadius * centerRadius;
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

        // Angle and distance use separate power curves so they can be tuned
        // independently.
        // Angle widens early to prevent holes at moderate pitch.
        // Distance stays full longer for performance, only shrinks near vertical.
        float tAngle = getPitchT(absPitch, PITCH_POWER_ANGLE);
        float tDistance = getPitchT(absPitch, PITCH_POWER_DISTANCE);
        float effectiveAngle = getEffectiveHalfAngle(halfFov, tAngle);
        float maxDistanceSq = getPitchMaxDistanceSq(tDistance);

        for (long coordinate : megaRenderQueue.keySet())
            renderMega(coordinate, cameraAngle, effectiveAngle, maxDistanceSq);

        for (long coordinate : chunkRenderQueue.keySet())
            renderChunk(coordinate, cameraAngle, effectiveAngle, maxDistanceSq);
    }

    private void renderMega(long megaCoordinate, float cameraAngle, float effectiveAngle, float maxDistanceSq) {

        GridSlotHandle slot = megaRenderQueue.get(megaCoordinate);

        if (!isMegaVisible(slot, cameraAngle, effectiveAngle, maxDistanceSq))
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

    private void renderChunk(long chunkCoordinate, float cameraAngle, float effectiveAngle, float maxDistanceSq) {

        GridSlotHandle slot = chunkRenderQueue.get(chunkCoordinate);

        if (megaRenderQueue.containsKey(slot.getMegaCoordinate()))
            return;

        if (!isChunkVisible(slot, cameraAngle, effectiveAngle, maxDistanceSq))
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

    private boolean isChunkVisible(GridSlotHandle slot, float cameraAngle, float effectiveAngle, float maxDistanceSq) {

        float distanceSq = slot.getChunkDistanceFromCenter();

        if (distanceSq > maxDistanceSq)
            return false;

        float distance = (float) Math.sqrt(distanceSq);
        float bleed = Math.max(MIN_BLEED, 0.75f / Math.max(distance, 0.001f));

        return isWithinAngle(slot.getChunkAngleFromCenter(), cameraAngle, effectiveAngle + bleed);
    }

    private boolean isMegaVisible(GridSlotHandle slot, float cameraAngle, float effectiveAngle, float maxDistanceSq) {

        float distanceSq = slot.getMegaDistanceFromCenter();

        if (distanceSq > maxDistanceSq)
            return false;

        float distance = (float) Math.sqrt(distanceSq);
        float megaBleed = Math.max(MIN_BLEED, (float) Math.atan(megaAngularBleedBase / Math.max(distance, 0.001f)));

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
     * Pitch curve driver. Returns t in [0, 1].
     * t = 0 at horizon, t = 1 at straight up/down.
     * power controls how quickly t rises — low = early onset, high = late sharp
     * drop.
     */
    private float getPitchT(float absPitch, float power) {
        return (float) Math.pow(absPitch / HALF_PI, power);
    }

    /**
     * Horizontal half-angle of the visible cone.
     * t = 0 → halfFov (tight forward cone at horizon)
     * t = 1 → PI (full 360 circle at vertical)
     */
    private float getEffectiveHalfAngle(float halfFov, float t) {
        return halfFov + t * (PI - halfFov);
    }

    /**
     * Max render distance (squared).
     * t = 0 → fullCenterRadiusSq (full distance at horizon)
     * t = 1 → PITCH_MIN_DIST_SQ (5 chunk radius at vertical)
     *
     * PITCH_MIN_DIST_SQ = 25 is always less than fullCenterRadiusSq on any
     * real grid so this always shrinks, never inverts.
     */
    private float getPitchMaxDistanceSq(float t) {
        return fullCenterRadiusSq + t * (PITCH_MIN_DIST_SQ - fullCenterRadiusSq);
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