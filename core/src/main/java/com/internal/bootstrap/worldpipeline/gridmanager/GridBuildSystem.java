package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.vectors.Vector2;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Comparator;

class GridBuildSystem extends SystemPackage {

    private UBOManager uboManager;
    private int CHUNK_SIZE;
    private int MEGA_CHUNK_SIZE;

    @Override
    protected void create() {
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.MEGA_CHUNK_SIZE = EngineSetting.MEGA_CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    GridInstance buildGrid() {

        float radius = calculateRadius();
        float radiusSquared = radius * radius;

        Long2ObjectOpenHashMap<GridSlotData> gridSlotData = createGridSlotData(radius);

        int totalSlots = gridSlotData.size();

        long[] loadOrder = assignLoadOrder(gridSlotData);

        LongOpenHashSet gridCoordinates = new LongOpenHashSet(gridSlotData.keySet());

        Long2ObjectOpenHashMap<GridSlotHandle> gridSlots = createGridSlotHandles(
                gridCoordinates,
                gridSlotData);

        populateCoveredSlots(gridSlots);

        GridInstance gridInstance = create(GridInstance.class);
        gridInstance.constructor(
                totalSlots,
                loadOrder,
                gridCoordinates,
                gridSlots,
                radiusSquared);

        return gridInstance;
    }

    private float calculateRadius() {
        return settings.maxRenderDistance / 2f;
    }

    private Long2ObjectOpenHashMap<GridSlotData> createGridSlotData(float radius) {

        Long2ObjectOpenHashMap<GridSlotData> gridSlotData = new Long2ObjectOpenHashMap<>();

        int maxRenderDistance = settings.maxRenderDistance;
        float radiusSquared = radius * radius;

        for (int x = -(maxRenderDistance / 2); x < maxRenderDistance / 2; x++) {
            for (int y = -(maxRenderDistance / 2); y < maxRenderDistance / 2; y++) {

                float distanceFromCenter = (x * x) + (y * y);

                if (distanceFromCenter <= radiusSquared) {

                    float angleRadians = (float) Math.atan2(y, x);
                    long gridCoordinate = Coordinate2Long.pack(x, y);

                    gridSlotData.put(gridCoordinate, createGridSlotData(
                            gridCoordinate,
                            distanceFromCenter,
                            angleRadians));
                }
            }
        }

        return gridSlotData;
    }

    private GridSlotData createGridSlotData(
            long gridCoordinate,
            float distanceFromCenter,
            float angleRadians) {

        GridSlotData gridSlotData = create(GridSlotData.class);
        gridSlotData.constructor(
                gridCoordinate,
                distanceFromCenter,
                angleRadians);

        return gridSlotData;
    }

    private long[] assignLoadOrder(Long2ObjectOpenHashMap<GridSlotData> gridSlotData) {

        ObjectArrayList<GridSlotData> slots = new ObjectArrayList<>(gridSlotData.values());
        slots.sort(Comparator.comparingDouble(GridSlotData::getDistanceFromCenter));

        long[] coordinates = new long[slots.size()];
        for (int i = 0; i < slots.size(); i++)
            coordinates[i] = slots.get(i).getGridCoordinate();

        return coordinates;
    }

    private Long2ObjectOpenHashMap<GridSlotHandle> createGridSlotHandles(
            LongOpenHashSet gridCoordinates,
            Long2ObjectOpenHashMap<GridSlotData> gridSlotData) {

        Long2ObjectOpenHashMap<GridSlotHandle> gridSlots = new Long2ObjectOpenHashMap<>();

        UBOHandle baseUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.GRID_COORDINATE_UBO);

        for (Long gridCoordinate : gridCoordinates) {

            UBOHandle slotUBO = uboManager.cloneUBO(baseUBO);

            int gridX = Coordinate2Long.unpackX(gridCoordinate) * CHUNK_SIZE;
            int gridY = Coordinate2Long.unpackY(gridCoordinate) * CHUNK_SIZE;
            Vector2 gridPosition = new Vector2(gridX, gridY);

            slotUBO.updateUniform("u_gridPosition", gridPosition);
            slotUBO.push();

            GridSlotData slotData = gridSlotData.get(gridCoordinate.longValue());
            float distanceFromCenter = slotData.getDistanceFromCenter();
            float angleFromCenter = slotData.getAngleRadians();

            int chunkX = Coordinate2Long.unpackX(gridCoordinate);
            int chunkY = Coordinate2Long.unpackY(gridCoordinate);
            float absoluteChunkDistance = (float) Math.sqrt(chunkX * chunkX + chunkY * chunkY);

            GridSlotDetailLevel detailLevel = GridSlotDetailLevel.getDetailLevelForDistance(absoluteChunkDistance);

            gridSlots.putIfAbsent(
                    gridCoordinate,
                    createGridSlotHandle(
                            gridCoordinate,
                            slotUBO,
                            distanceFromCenter,
                            angleFromCenter,
                            detailLevel));
        }

        return gridSlots;
    }

    private GridSlotHandle createGridSlotHandle(
            long gridCoordinate,
            UBOHandle slotUBO,
            float distanceFromCenter,
            float angleFromCenter,
            GridSlotDetailLevel detailLevel) {

        GridSlotHandle gridSlotHandle = create(GridSlotHandle.class);
        gridSlotHandle.constructor(
                gridCoordinate,
                slotUBO,
                distanceFromCenter,
                angleFromCenter,
                detailLevel);

        return gridSlotHandle;
    }

    private void populateCoveredSlots(Long2ObjectOpenHashMap<GridSlotHandle> gridSlots) {

        for (Long2ObjectOpenHashMap.Entry<GridSlotHandle> entry : gridSlots.long2ObjectEntrySet()) {

            long gridCoordinate = entry.getLongKey();
            GridSlotHandle gridSlotHandle = entry.getValue();

            int originX = Coordinate2Long.unpackX(gridCoordinate);
            int originY = Coordinate2Long.unpackY(gridCoordinate);

            for (int x = 0; x < MEGA_CHUNK_SIZE; x++) {
                for (int y = 0; y < MEGA_CHUNK_SIZE; y++) {

                    long coveredCoordinate = Coordinate2Long.pack(originX + x, originY + y);
                    GridSlotHandle coveredSlot = gridSlots.get(coveredCoordinate);

                    if (coveredSlot != null)
                        gridSlotHandle.getCoveredSlots().add(coveredSlot);
                }
            }
        }
    }
}