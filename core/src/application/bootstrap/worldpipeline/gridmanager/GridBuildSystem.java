package application.bootstrap.worldpipeline.gridmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

class GridBuildSystem extends SystemPackage {

    /*
     * Constructs a GridInstance and all GridSlotHandles for a given focal
     * entity and window. Each grid gets its own ClockInstance, its own
     * WeatherPatternInstance for local wind/temperature, and its own
     * Time/Sun/Moon/Sky/Weather-Map UBO instances cloned from the shared
     * base handles, so every window tracks its own location independently.
     */

    // Internal
    private UBOManager uboManager;
    private ClockManager clockManager;
    private WeatherManager weatherManager;
    private WeatherPatternManager weatherPatternManager;

    // Config
    private int chunkSize;
    private int megaChunkSize;
    private int chunkPoolMaxOverflow;

    // Base UBO Handles
    private UBOHandle timeDataBase;
    private UBOHandle sunLightBase;
    private UBOHandle moonLightBase;
    private UBOHandle skyColorBase;
    private UBOHandle weatherMapBase;

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
        this.clockManager = get(ClockManager.class);
        this.weatherManager = get(WeatherManager.class);
        this.weatherPatternManager = get(WeatherPatternManager.class);
    }

    @Override
    protected void awake() {
        this.timeDataBase = uboManager.getUBOHandleFromUBOName(EngineSetting.UBO_TIME_DATA_NAME);
        this.sunLightBase = uboManager.getUBOHandleFromUBOName(EngineSetting.SUN_LIGHT_UBO);
        this.moonLightBase = uboManager.getUBOHandleFromUBOName(EngineSetting.MOON_LIGHT_UBO);
        this.skyColorBase = uboManager.getUBOHandleFromUBOName(EngineSetting.SKY_COLOR_UBO);
        this.weatherMapBase = uboManager.getUBOHandleFromUBOName(EngineSetting.WEATHER_MAP_UBO);
    }

    // Build \\

    GridInstance buildGrid(EntityInstance focalEntity, WindowInstance windowInstance, FboInstance renderTargetFbo) {

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
                windowInstance,
                renderTargetFbo,
                totalSlots,
                loadOrder,
                gridCoordinates,
                gridSlots,
                radiusSquared,
                maxChunks,
                clockManager.createClockInstance(),
                uboManager.createUBOInstance(timeDataBase),
                uboManager.createUBOInstance(sunLightBase),
                uboManager.createUBOInstance(moonLightBase),
                uboManager.createUBOInstance(skyColorBase),
                buildWeatherMapUBO(),
                weatherPatternManager.createLocalPatternInstance());

        return gridInstance;
    }

    // Weather Map \\

    private UBOInstance buildWeatherMapUBO() {

        UBOInstance weatherMapUBO = uboManager.createUBOInstance(weatherMapBase);

        weatherMapUBO.updateUniform("u_weatherOuterRangeChunks", weatherManager.getEffectiveOuterRangeChunks());
        weatherMapUBO.updateUniform("u_weatherNearRangeChunks", weatherManager.getEffectiveNearRangeChunks());

        uboManager.push(weatherMapUBO);

        return weatherMapUBO;
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

            int chunkX = Coordinate2Long.unpackX(gridCoordinate);
            int chunkY = Coordinate2Long.unpackY(gridCoordinate);

            int gridX = chunkX * chunkSize;
            int gridY = chunkY * chunkSize;

            float absoluteChunkDistance = (float) Math.sqrt(chunkX * chunkX + chunkY * chunkY);
            GridSlotDetailLevel detailLevel = GridSlotDetailLevel.getDetailLevelForDistance(absoluteChunkDistance);

            float chunkDistanceFromCenter = chunkX * chunkX + chunkY * chunkY;
            float chunkAngleFromCenter = (float) Math.atan2(chunkY, chunkX);

            float halfMega = megaChunkSize / 2f;
            float mcx = chunkX + halfMega;
            float mcy = chunkY + halfMega;
            float megaDistanceFromCenter = mcx * mcx + mcy * mcy;
            float megaAngleFromCenter = (float) Math.atan2(mcy, mcx);

            slotUBO.updateUniform("u_gridPosition", new Vector2(gridX, gridY));
            slotUBO.updateUniform("u_distanceFromCenter", chunkDistanceFromCenter);
            uboManager.push(slotUBO);

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