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

    @Override
    protected void create() {
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    GridInstance buildGrid() {

        // Calculate the radius of the grid
        float radius = calculateRadius();

        // Create temporary grid slot data with angle and distance
        Long2ObjectOpenHashMap<GridSlotData> gridSlotdata = createGridSlotData(radius);

        // Calculate total slots in grid
        int totalSlots = gridSlotdata.size();

        // Assign load order sorted by distance
        long[] loadOrder = assignLoadOrder(gridSlotdata);

        // Extract grid coordinates from data
        LongOpenHashSet gridCoordinates = new LongOpenHashSet(gridSlotdata.keySet());

        // Create persistent grid slot handles with management data
        Long2ObjectOpenHashMap<GridSlotHandle> gridSlots = createGridSlotHandles(
                gridCoordinates,
                gridSlotdata,
                radius);

        GridInstance gridInstance = create(GridInstance.class);
        gridInstance.constructor(
                totalSlots,
                loadOrder,
                gridCoordinates,
                gridSlots);

        return gridInstance;
    }

    // Calculate grid radius from max render distance
    private float calculateRadius() {
        return settings.maxRenderDistance / 2f;
    }

    // Create all grid slot coordinates within circular radius
    private Long2ObjectOpenHashMap<GridSlotData> createGridSlotData(float radius) {

        Long2ObjectOpenHashMap<GridSlotData> gridSlotdata = new Long2ObjectOpenHashMap<>();

        int maxRenderDistance = settings.maxRenderDistance;
        float radiusSquared = radius * radius;

        for (int x = -(maxRenderDistance / 2); x < maxRenderDistance / 2; x++)
            for (int y = -(maxRenderDistance / 2); y < maxRenderDistance / 2; y++) {

                float distanceFromCenter = (x * x) + (y * y);

                if (distanceFromCenter <= radiusSquared) {

                    // Calculate angle for visibility calculations
                    float angleRadians = (float) Math.atan2(y, x);

                    long gridCoordinate = Coordinate2Long.pack(x, y);

                    gridSlotdata.put(gridCoordinate, createGridSlotData(
                            gridCoordinate,
                            distanceFromCenter,
                            angleRadians));
                }
            }

        return gridSlotdata;
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

    // Sort slots by distance and return load order
    private long[] assignLoadOrder(
            Long2ObjectOpenHashMap<GridSlotData> gridSlotdata) {

        ObjectArrayList<GridSlotData> slots = new ObjectArrayList<>(gridSlotdata.values());

        slots.sort(Comparator.comparingDouble(GridSlotData::getDistanceFromCenter));

        long[] coordinates = new long[slots.size()];

        for (int i = 0; i < slots.size(); i++)
            coordinates[i] = slots.get(i).getGridCoordinate();

        return coordinates;
    }

    // Create persistent grid slot handles with detail levels
    private Long2ObjectOpenHashMap<GridSlotHandle> createGridSlotHandles(
            LongOpenHashSet gridCoordinates,
            Long2ObjectOpenHashMap<GridSlotData> gridSlotdata,
            float radius) {

        Long2ObjectOpenHashMap<GridSlotHandle> gridSlots = new Long2ObjectOpenHashMap<>();

        // Get base UBO for cloning
        UBOHandle baseUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.GRID_COORDINATE_UBO);

        for (Long gridCoordinate : gridCoordinates) {

            // Clone UBO for this grid slot
            UBOHandle slotUBO = uboManager.cloneUBO(baseUBO);

            // Unpack and calculate grid position
            int gridX = Coordinate2Long.unpackX(gridCoordinate) * CHUNK_SIZE;
            int gridY = Coordinate2Long.unpackY(gridCoordinate) * CHUNK_SIZE;
            Vector2 gridPosition = new Vector2(gridX, gridY);

            // Set static grid position uniform
            slotUBO.updateUniform("u_gridPosition", gridPosition);
            slotUBO.push();

            // Get distance from temporary data
            GridSlotData slotData = gridSlotdata.get(gridCoordinate.longValue());
            float distanceFromCenter = slotData.getDistanceFromCenter();

            // Calculate normalized distance (0.0 = center, 1.0 = edge)
            float normalizedDistance = (float) Math.sqrt(distanceFromCenter) / radius;

            // Determine detail level from distance
            GridSlotDetailLevel detailLevel = GridSlotDetailLevel.getDetailLevelForDistance(normalizedDistance);

            // Create persistent grid slot handle
            gridSlots.putIfAbsent(
                    gridCoordinate,
                    createGridSlotHandle(
                            gridCoordinate,
                            slotUBO,
                            distanceFromCenter,
                            normalizedDistance,
                            detailLevel));
        }

        return gridSlots;
    }

    private GridSlotHandle createGridSlotHandle(
            long gridCoordinate,
            UBOHandle slotUBO,
            float distanceFromCenter,
            float normalizedDistance,
            GridSlotDetailLevel detailLevel) {

        GridSlotHandle gridSlotHandle = create(GridSlotHandle.class);
        gridSlotHandle.constructor(
                gridCoordinate,
                slotUBO,
                distanceFromCenter,
                normalizedDistance,
                detailLevel);

        return gridSlotHandle;
    }
}