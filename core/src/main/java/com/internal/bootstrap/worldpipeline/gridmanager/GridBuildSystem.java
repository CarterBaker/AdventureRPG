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

        // First calculate the radius
        float radius = calculateRadius();

        // Next calculate grid coordinates
        Long2ObjectOpenHashMap<GridSlotData> gridSlotdata = createGridSlotData(radius);

        // Now we know the total amount of chunks the current grid will hold
        int totalSlots = gridSlotdata.size();

        // Initialize data lists
        long[] loadOrder = assignLoadOrder(gridSlotdata);

        LongOpenHashSet gridCoordinates = new LongOpenHashSet(gridSlotdata.keySet());

        Long2ObjectOpenHashMap<GridSlotHandle> gridSlots = createGridSlotHandles(gridCoordinates);

        GridInstance gridInstance = create(GridInstance.class);
        gridInstance.constructor(
                totalSlots,
                loadOrder,
                gridCoordinates,
                gridSlots);

        return gridInstance;
    }

    // Radius formula
    private float calculateRadius() {
        return settings.maxRenderDistance / 2f;
    }

    // Collect all coordinates inside a circle
    private Long2ObjectOpenHashMap<GridSlotData> createGridSlotData(float radius) {

        Long2ObjectOpenHashMap<GridSlotData> gridSlotdata = new Long2ObjectOpenHashMap<>();

        int maxRenderDistance = settings.maxRenderDistance;
        float radiusSquared = radius * radius;

        for (int x = -(maxRenderDistance / 2); x < maxRenderDistance / 2; x++)
            for (int y = -(maxRenderDistance / 2); y < maxRenderDistance / 2; y++) {

                float distanceFromCenter = (x * x) + (y * y);

                if (distanceFromCenter <= radiusSquared) {
                    long gridCoordinate = Coordinate2Long.pack(x, y);
                    gridSlotdata.put(gridCoordinate, createGridSlotData(
                            gridCoordinate,
                            distanceFromCenter));
                }
            }

        return gridSlotdata;
    }

    private GridSlotData createGridSlotData(
            long gridCoordinate,
            float distanceFromCenter) {

        GridSlotData gridSlotData = create(GridSlotData.class);
        gridSlotData.constructor(
                gridCoordinate,
                distanceFromCenter);

        return gridSlotData;
    }

    // Sort by distance and assign load order
    private long[] assignLoadOrder(
            Long2ObjectOpenHashMap<GridSlotData> gridSlotdata) {

        ObjectArrayList<GridSlotData> slots = new ObjectArrayList<>(gridSlotdata.values());

        slots.sort(Comparator.comparingDouble(GridSlotData::getDistanceFromCenter));

        long[] coordinates = new long[slots.size()];
        for (int i = 0; i < slots.size(); i++)
            coordinates[i] = slots.get(i).getGridCoordinate();

        return coordinates;
    }

    private Long2ObjectOpenHashMap<GridSlotHandle> createGridSlotHandles(
            LongOpenHashSet gridCoordinates) {

        Long2ObjectOpenHashMap<GridSlotHandle> gridSlots = new Long2ObjectOpenHashMap<>();

        // Get the UBOManager to access the base UBO and clone it
        UBOHandle baseUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.GRID_COORDINATE_UBO);

        for (Long gridCoordinate : gridCoordinates) {

            // Clone the UBO for this grid slot
            UBOHandle slotUBO = uboManager.cloneUBO(baseUBO);

            // Unpack grid coordinates
            int gridX = Coordinate2Long.unpackX(gridCoordinate) * CHUNK_SIZE;
            int gridY = Coordinate2Long.unpackY(gridCoordinate) * CHUNK_SIZE;
            Vector2 gridPosition = new Vector2(gridX, gridY);

            // Set the static grid position once
            slotUBO.updateUniform("u_gridPosition", gridPosition);
            slotUBO.push();

            // Create the grid slot handle with its unique UBO
            gridSlots.putIfAbsent(
                    gridCoordinate,
                    createGridSlotHandle(gridCoordinate, slotUBO));
        }

        return gridSlots;
    }

    private GridSlotHandle createGridSlotHandle(long gridCoordinate, UBOHandle slotUBO) {

        GridSlotHandle gridSlotHandle = create(GridSlotHandle.class);
        gridSlotHandle.constructor(
                gridCoordinate,
                slotUBO);

        return gridSlotHandle;
    }
}