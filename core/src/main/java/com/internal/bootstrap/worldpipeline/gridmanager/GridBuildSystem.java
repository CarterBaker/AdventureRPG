package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.extras.Coordinate2Long;
import com.internal.core.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

class GridBuildSystem extends SystemPackage {

    /*
     * Constructs a GridInstance and all GridSlotHandles for a given focal entity.
     * Derives load order by sorting coordinates by distance from center.
     * Allocates one UBOInstance per slot for GPU grid position data.
     */

    // Internal
    private UBOManager uboManager;

    // Config
    private int chunkSize;
    private int megaChunkSize;
    private int chunkPoolMaxOverflow;

    // Internal \\

    @Override
    protected void create() {
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.megaChunkSize = EngineSetting.MEGA_CHUNK_SIZE;
        this.chunkPoolMaxOverflow = EngineSetting.CHUNK_POOL_MAX_OVERFLOW;
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    // Build \\

    GridInstance buildGrid(EntityInstance focalEntity) {

        float radius = calculateRadius();
        float radiusSquared = radius * radius;

        long[] loadOrder = assignLoadOrder(radius);

        LongOpenHashSet gridCoordinates = new LongOpenHashSet();
        for (long coord : loadOrder)
            gridCoordinates.add(coord);

        int totalSlots = loadOrder.length;
        int maxChunks = totalSlots + chunkPoolMaxOverflow;

        GridInstance gridInstance = create(GridInstance.class);

        Long2ObjectOpenHashMap<GridSlotHandle> gridSlots = createGridSlotHandles(
                gridCoordinates,
                gridInstance);

        populateCoveredSlots(gridSlots);

        gridInstance.constructor(
                focalEntity,
                totalSlots,
                loadOrder,
                gridCoordinates,
                gridSlots,
                radiusSquared,
                maxChunks);

        return gridInstance;
    }

    // Radius \\

    private float calculateRadius() {
        return settings.maxRenderDistance / 2f;
    }

    // Load Order \\

    private long[] assignLoadOrder(float radius) {

        int maxRenderDistance = settings.maxRenderDistance;
        float radiusSquared = radius * radius;

        LongArrayList coordinates = new LongArrayList();
        FloatArrayList distances = new FloatArrayList();

        for (int x = -(maxRenderDistance / 2); x < maxRenderDistance / 2; x++) {
            for (int y = -(maxRenderDistance / 2); y < maxRenderDistance / 2; y++) {

                float d = (x * x) + (y * y);

                if (d <= radiusSquared) {
                    coordinates.add(Coordinate2Long.pack(x, y));
                    distances.add(d);
                }
            }
        }

        Integer[] indices = new Integer[coordinates.size()];
        for (int i = 0; i < indices.length; i++)
            indices[i] = i;

        java.util.Arrays.sort(indices,
                (a, b) -> Float.compare(distances.getFloat(a), distances.getFloat(b)));

        long[] sorted = new long[indices.length];
        for (int i = 0; i < indices.length; i++)
            sorted[i] = coordinates.getLong(indices[i]);

        return sorted;
    }

    // Grid Slot Handles \\

    private Long2ObjectOpenHashMap<GridSlotHandle> createGridSlotHandles(
            LongOpenHashSet gridCoordinates,
            GridInstance gridInstance) {

        Long2ObjectOpenHashMap<GridSlotHandle> gridSlots = new Long2ObjectOpenHashMap<>();

        UBOHandle baseUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.GRID_COORDINATE_UBO);

        LongIterator it = gridCoordinates.iterator();

        while (it.hasNext()) {

            long gridCoordinate = it.nextLong();

            UBOInstance slotUBO = uboManager.createUBOInstance(baseUBO);

            int gridX = Coordinate2Long.unpackX(gridCoordinate) * chunkSize;
            int gridY = Coordinate2Long.unpackY(gridCoordinate) * chunkSize;

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

            float halfMega = megaChunkSize / 2f;
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

            for (int x = 0; x < megaChunkSize; x++) {
                for (int y = 0; y < megaChunkSize; y++) {

                    long coveredCoordinate = Coordinate2Long.pack(originX + x, originY + y);
                    GridSlotHandle coveredSlot = gridSlots.get(coveredCoordinate);

                    if (coveredSlot != null)
                        gridSlotHandle.getCoveredSlots().add(coveredSlot);
                }
            }
        }
    }
}