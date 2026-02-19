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

    // Frustum Culling
    private static final float PITCH_MIN_DISTANCE_SQ = 16f;
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
        float fullRadiusSq = gridManager.getGrid().getRadiusSquared();

        float cameraAngle = getCameraAngle(camera);
        float halfFov = getHalfFov(camera);
        float pitchDistanceSq = getPitchDistanceSq(camera, fullRadiusSq);

        for (long coordinate : megaRenderQueue.keySet())
            renderMega(coordinate, cameraAngle, halfFov, pitchDistanceSq);

        for (long coordinate : chunkRenderQueue.keySet())
            renderChunk(coordinate, cameraAngle, halfFov, pitchDistanceSq);
    }

    private void renderMega(long megaCoordinate, float cameraAngle, float halfFov, float pitchDistanceSq) {

        GridSlotHandle gridSlotHandle = megaRenderQueue.get(megaCoordinate);

        if (!isMegaVisible(gridSlotHandle, cameraAngle, halfFov, pitchDistanceSq))
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

    private void renderChunk(long chunkCoordinate, float cameraAngle, float halfFov, float pitchDistanceSq) {

        GridSlotHandle gridSlotHandle = chunkRenderQueue.get(chunkCoordinate);

        if (megaRenderQueue.containsKey(gridSlotHandle.getMegaCoordinate()))
            return;

        if (!isChunkVisible(gridSlotHandle, cameraAngle, halfFov, pitchDistanceSq))
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

    // Frustum Culling \\

    private boolean isChunkVisible(GridSlotHandle slot, float cameraAngle, float halfFov, float pitchDistanceSq) {

        float distanceSq = slot.getDistanceFromCenter();

        if (distanceSq > pitchDistanceSq)
            return false;

        if (distanceSq <= 4f)
            return true;

        float bleed = 0.75f / (float) Math.sqrt(distanceSq);

        return isWithinAngle(slot.getAngleFromCenter(), cameraAngle, halfFov + bleed);
    }

    private boolean isMegaVisible(GridSlotHandle slot, float cameraAngle, float halfFov, float pitchDistanceSq) {

        float distanceSq = slot.getDistanceFromCenter();

        if (distanceSq > pitchDistanceSq)
            return false;

        if (distanceSq <= 4f)
            return true;

        float distance = (float) Math.sqrt(distanceSq);
        float megaBleed = (float) Math.atan(megaAngularBleedBase / distance);

        return isWithinAngle(slot.getAngleFromCenter(), cameraAngle, halfFov + megaBleed);
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
        return (float) Math.atan2(-camera.getDirection().z, camera.getDirection().x);
    }

    private float getHalfFov(CameraInstance camera) {
        return (float) Math.toRadians(camera.getFOV() / 2f);
    }

    private float getPitchDistanceSq(CameraInstance camera, float fullRadiusSq) {

        // Camera stores y as negative when looking up - negate to correct
        float pitch = (float) Math.asin(Math.max(-1f, Math.min(1f, -camera.getDirection().y)));
        float absPitch = Math.abs(pitch);
        float pitchT = 1f - (absPitch / HALF_PI);
        pitchT = pitchT * pitchT;

        // Higher player Y increases minimum render distance when looking down
        float playerY = playerManager.getPlayer().getWorldPositionStruct().getPosition().y;
        float heightFactor = Math.min(1f, playerY / (EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE));
        float minDistanceSq = PITCH_MIN_DISTANCE_SQ + (heightFactor * (fullRadiusSq * 0.25f));

        return minDistanceSq + (pitchT * (fullRadiusSq - minDistanceSq));
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