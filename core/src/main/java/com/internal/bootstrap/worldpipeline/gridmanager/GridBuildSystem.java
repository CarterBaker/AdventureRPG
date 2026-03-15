package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.extras.Coordinate2Long;
import com.internal.core.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;

class GridBuildSystem extends SystemPackage {

    // Internal
    private UBOManager uboManager;

    // Config
    private int CHUNK_SIZE;
    private int MEGA_CHUNK_SIZE;

    // Base \\

    @Override
    protected void create() {
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.MEGA_CHUNK_SIZE = EngineSetting.MEGA_CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    // Build \\

    GridInstance buildGrid() {

        float radius = calculateRadius();
        float radiusSquared = radius * radius;

        Long2ObjectOpenHashMap<GridSlotData> gridSlotData = createGridSlotData(radius);

        int totalSlots = gridSlotData.size();
        long[] loadOrder = assignLoadOrder(gridSlotData);
        LongOpenHashSet gridCoordinates = new LongOpenHashSet(gridSlotData.keySet());

        // Create instance first so handles can hold a back-reference to it
        GridInstance gridInstance = create(GridInstance.class);

        Long2ObjectOpenHashMap<GridSlotHandle> gridSlots = createGridSlotHandles(
                gridCoordinates,
                gridSlotData,
                gridInstance);

        populateCoveredSlots(gridSlots);

        gridInstance.constructor(
                totalSlots,
                loadOrder,
                gridCoordinates,
                gridSlots,
                radiusSquared);

        return gridInstance;
    }

    // Radius \\

    private float calculateRadius() {
        return settings.maxRenderDistance / 2f;
    }

    // Grid Slot Data \\

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

        GridSlotData data = create(GridSlotData.class);
        data.constructor(gridCoordinate, distanceFromCenter, angleRadians);
        return data;
    }

    // Load Order \\

    private long[] assignLoadOrder(Long2ObjectOpenHashMap<GridSlotData> gridSlotData) {

        ObjectArrayList<GridSlotData> slots = new ObjectArrayList<>(gridSlotData.values());
        slots.sort(Comparator.comparingDouble(GridSlotData::getDistanceFromCenter));

        long[] coordinates = new long[slots.size()];
        for (int i = 0; i < slots.size(); i++)
            coordinates[i] = slots.get(i).getGridCoordinate();

        return coordinates;
    }

    // Grid Slot Handles \\

    private Long2ObjectOpenHashMap<GridSlotHandle> createGridSlotHandles(
            LongOpenHashSet gridCoordinates,
            Long2ObjectOpenHashMap<GridSlotData> gridSlotData,
            GridInstance gridInstance) {

        Long2ObjectOpenHashMap<GridSlotHandle> gridSlots = new Long2ObjectOpenHashMap<>();

        UBOHandle baseUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.GRID_COORDINATE_UBO);

        LongIterator it = gridCoordinates.iterator();

        while (it.hasNext()) {

            long gridCoordinate = it.nextLong();

            UBOInstance slotUBO = uboManager.createUBOInstance(baseUBO);

            int gridX = Coordinate2Long.unpackX(gridCoordinate) * CHUNK_SIZE;
            int gridY = Coordinate2Long.unpackY(gridCoordinate) * CHUNK_SIZE;

            slotUBO.updateUniform("u_gridPosition", new Vector2(gridX, gridY));
            uboManager.push(slotUBO);

            int chunkX = Coordinate2Long.unpackX(gridCoordinate);
            int chunkY = Coordinate2Long.unpackY(gridCoordinate);

            float absoluteChunkDistance = (float) Math.sqrt(chunkX * chunkX + chunkY * chunkY);
            GridSlotDetailLevel detailLevel = GridSlotDetailLevel.getDetailLevelForDistance(absoluteChunkDistance);

            float ccx = chunkX + 0.5f;
            float ccy = chunkY + 0.5f;
            float chunkDistanceFromCenter = ccx * ccx + ccy * ccy;
            float chunkAngleFromCenter = (float) Math.atan2(ccy, ccx);

            float halfMega = MEGA_CHUNK_SIZE / 2f;
            float mcx = chunkX + halfMega;
            float mcy = chunkY + halfMega;
            float megaDistanceFromCenter = mcx * mcx + mcy * mcy;
            float megaAngleFromCenter = (float) Math.atan2(mcy, mcx);

            gridSlots.putIfAbsent(
                    gridCoordinate,
                    createGridSlotHandle(
                            gridCoordinate,
                            slotUBO,
                            chunkDistanceFromCenter,
                            chunkAngleFromCenter,
                            megaDistanceFromCenter,
                            megaAngleFromCenter,
                            detailLevel,
                            gridInstance));
        }

        return gridSlots;
    }

    private GridSlotHandle createGridSlotHandle(
            long gridCoordinate,
            UBOInstance slotUBO,
            float chunkDistanceFromCenter,
            float chunkAngleFromCenter,
            float megaDistanceFromCenter,
            float megaAngleFromCenter,
            GridSlotDetailLevel detailLevel,
            GridInstance gridInstance) {

        GridSlotHandle handle = create(GridSlotHandle.class);
        handle.constructor(
                gridCoordinate,
                slotUBO,
                chunkDistanceFromCenter,
                chunkAngleFromCenter,
                megaDistanceFromCenter,
                megaAngleFromCenter,
                detailLevel,
                gridInstance);

        return handle;
    }

    // Covered Slots \\

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