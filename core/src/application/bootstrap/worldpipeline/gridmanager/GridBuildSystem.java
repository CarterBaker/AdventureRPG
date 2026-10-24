package application.bootstrap.worldpipeline.gridmanager;

import java.util.Arrays;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.oceanpipeline.exposuremanager.ExposureManager;
import application.bootstrap.oceanpipeline.turbulencemanager.TurbulenceManager;
import application.bootstrap.oceanpipeline.wavemanager.WaveManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.precipitationmanager.PrecipitationManager;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternManager;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.grid.GridRangeStruct;
import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

class GridBuildSystem extends SystemPackage {

    /*
     * Constructs a GridInstance and all GridSlotHandles for a given focal
     * entity, window and range, including each window's own cloned clock,
     * weather, wind, precipitation, ocean turbulence, lighting and render
     * settings UBO instances. Rebuilding re-lays an existing grid's slots for
     * the range it follows in place and releases the slot UBOs it replaces,
     * leaving the grid's per-window state untouched. The slots form a disc of
     * the range's render distance, so a range of zero lays none.
     */

    // Internal
    private UBOManager uboManager;
    private ClockManager clockManager;
    private WeatherPatternManager weatherPatternManager;
    private WindManager windManager;
    private PrecipitationManager precipitationManager;
    private TurbulenceManager turbulenceManager;
    private ExposureManager exposureManager;
    private WaveManager waveManager;

    // Config
    private int chunkSize;
    private int megaChunkSize;
    private int chunkPoolMaxOverflow;

    // Base UBO Handles
    private UBOHandle timeDataBase;
    private UBOHandle sunLightBase;
    private UBOHandle moonLightBase;
    private UBOHandle skyColorBase;
    private UBOHandle celestialDataBase;
    private UBOHandle weatherMapBase;
    private UBOHandle windDataBase;
    private UBOHandle precipitationDataBase;
    private UBOHandle oceanDataBase;
    private UBOHandle settingsDataBase;

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
        this.weatherPatternManager = get(WeatherPatternManager.class);
        this.windManager = get(WindManager.class);
        this.precipitationManager = get(PrecipitationManager.class);
        this.turbulenceManager = get(TurbulenceManager.class);
        this.exposureManager = get(ExposureManager.class);
        this.waveManager = get(WaveManager.class);
    }

    @Override
    protected void awake() {
        this.timeDataBase = uboManager.getUBOHandleFromUBOName(EngineSetting.UBO_TIME_DATA_NAME);
        this.sunLightBase = uboManager.getUBOHandleFromUBOName(EngineSetting.SUN_LIGHT_UBO);
        this.moonLightBase = uboManager.getUBOHandleFromUBOName(EngineSetting.MOON_LIGHT_UBO);
        this.skyColorBase = uboManager.getUBOHandleFromUBOName(EngineSetting.SKY_COLOR_UBO);
        this.celestialDataBase = uboManager.getUBOHandleFromUBOName(EngineSetting.CELESTIAL_DATA_UBO);
        this.weatherMapBase = uboManager.getUBOHandleFromUBOName(EngineSetting.WEATHER_MAP_UBO);
        this.windDataBase = uboManager.getUBOHandleFromUBOName(EngineSetting.WIND_DATA_UBO);
        this.precipitationDataBase = uboManager.getUBOHandleFromUBOName(EngineSetting.PRECIPITATION_DATA_UBO);
        this.oceanDataBase = uboManager.getUBOHandleFromUBOName(EngineSetting.OCEAN_DATA_UBO);
        this.settingsDataBase = uboManager.getUBOHandleFromUBOName(EngineSetting.SETTINGS_UBO);
    }

    // Build \\

    GridInstance buildGrid(
            EntityInstance focalEntity,
            WindowInstance windowInstance,
            FBOInstance renderTargetFbo,
            GridRangeStruct range) {

        int renderDistance = range.getRenderDistance();
        float radius = calculateRadius(renderDistance);
        float radiusSquared = radius * radius;

        long[] loadOrder = assignLoadOrder(renderDistance);
        LongOpenHashSet gridCoordinates = collectGridCoordinates(loadOrder);

        int totalSlots = loadOrder.length;
        int maxChunks = totalSlots + chunkPoolMaxOverflow;

        GridInstance gridInstance = create(GridInstance.class);

        Long2ObjectOpenHashMap<GridSlotHandle> gridSlots = createGridSlotHandles(
                gridCoordinates,
                gridInstance);

        populateCoveredSlots(gridSlots);

        int immediateSlotCount = countImmediateSlots(loadOrder, gridSlots);
        int prioritySlotCount = countPrioritySlots(loadOrder, gridSlots);

        gridInstance.constructor(
                focalEntity,
                windowInstance,
                renderTargetFbo,
                totalSlots,
                loadOrder,
                immediateSlotCount,
                prioritySlotCount,
                gridCoordinates,
                gridSlots,
                radiusSquared,
                range,
                uboManager.createUBOInstance(settingsDataBase),
                maxChunks,
                clockManager.createClockInstance(),
                uboManager.createUBOInstance(timeDataBase),
                uboManager.createUBOInstance(sunLightBase),
                uboManager.createUBOInstance(moonLightBase),
                uboManager.createUBOInstance(skyColorBase),
                uboManager.createUBOInstance(celestialDataBase),
                uboManager.createUBOInstance(weatherMapBase),
                weatherPatternManager.createLocalWeatherInstance(),
                weatherPatternManager.createTemperatureInstance(),
                windManager.createWindInstance(),
                uboManager.createUBOInstance(windDataBase),
                precipitationManager.createPrecipitationInstance(),
                uboManager.createUBOInstance(precipitationDataBase),
                turbulenceManager.createTurbulenceInstance(),
                exposureManager.createExposureInstance(),
                waveManager.createWaveInstance(),
                uboManager.createUBOInstance(oceanDataBase));

        pushRenderSettings(gridInstance);

        return gridInstance;
    }

    // Rebuild \\

    void rebuildGrid(GridInstance gridInstance) {

        releaseSlotUBOs(gridInstance);

        int renderDistance = gridInstance.getRange().getRenderDistance();
        float radius = calculateRadius(renderDistance);
        long[] loadOrder = assignLoadOrder(renderDistance);
        LongOpenHashSet gridCoordinates = collectGridCoordinates(loadOrder);

        Long2ObjectOpenHashMap<GridSlotHandle> gridSlots = createGridSlotHandles(
                gridCoordinates,
                gridInstance);

        populateCoveredSlots(gridSlots);

        gridInstance.rebuildSlots(
                loadOrder.length,
                loadOrder,
                countImmediateSlots(loadOrder, gridSlots),
                countPrioritySlots(loadOrder, gridSlots),
                gridCoordinates,
                gridSlots,
                radius * radius);

        pushRenderSettings(gridInstance);
    }

    private void releaseSlotUBOs(GridInstance gridInstance) {
        for (GridSlotHandle gridSlotHandle : gridInstance.getGridSlots().values())
            uboManager.destroyInstance(gridSlotHandle.getSlotUBO());
    }

    // Render Settings \\

    // The grid's own reach for its shaders, so the fog's seam lands on the rim this grid actually draws
    void pushRenderSettings(GridInstance gridInstance) {

        UBOInstance settingsData = gridInstance.getSettingsDataUBO();

        settingsData.updateUniform(EngineSetting.UNIFORM_RENDER_DISTANCE, (float) gridInstance.getRenderDistance());
        settingsData.updateUniform(EngineSetting.UNIFORM_CHUNK_SIZE, (float) chunkSize);
        settingsData.updateUniform(EngineSetting.UNIFORM_DETAIL_RADIUS, (float) settings.detailRadius);
        uboManager.push(settingsData);
    }

    // Radius \\

    private float calculateRadius(int renderDistance) {
        return renderDistance / 2f;
    }

    // Load Order \\

    private long[] assignLoadOrder(int renderDistance) {

        int half = renderDistance / 2;
        float radius = calculateRadius(renderDistance);
        float radiusSquared = radius * radius;

        LongArrayList coordinates = new LongArrayList();
        long[] sortScratch = new long[renderDistance * renderDistance];
        int count = 0;

        for (int x = -half; x < half; x++) {
            for (int y = -half; y < half; y++) {

                int distanceSq = x * x + y * y;

                if (distanceSq > radiusSquared)
                    continue;

                coordinates.add(Coordinate2Long.pack(x, y));
                sortScratch[count] = ((long) distanceSq << 32) | (count & 0xFFFFFFFFL);
                count++;
            }
        }

        Arrays.sort(sortScratch, 0, count);

        long[] sorted = new long[count];

        for (int i = 0; i < count; i++) {
            int coordIndex = (int) (sortScratch[i] & 0xFFFFFFFFL);
            sorted[i] = coordinates.getLong(coordIndex);
        }

        return sorted;
    }

    private LongOpenHashSet collectGridCoordinates(long[] loadOrder) {

        LongOpenHashSet gridCoordinates = new LongOpenHashSet();

        for (long coord : loadOrder)
            gridCoordinates.add(coord);

        return gridCoordinates;
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

            slotUBO.updateUniform(EngineSetting.UNIFORM_GRID_POSITION, new Vector2(gridX, gridY));
            slotUBO.updateUniform(EngineSetting.UNIFORM_DISTANCE_FROM_CENTER, chunkDistanceFromCenter);
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

    // Immediate Range \\

    private int countImmediateSlots(long[] loadOrder, Long2ObjectOpenHashMap<GridSlotHandle> gridSlots) {

        int count = 0;

        while (count < loadOrder.length
                && gridSlots.get(loadOrder[count]).getDetailLevel() == GridSlotDetailLevel.IMMEDIATE)
            count++;

        return count;
    }

    // Priority Range \\

    private int countPrioritySlots(long[] loadOrder, Long2ObjectOpenHashMap<GridSlotHandle> gridSlots) {

        int count = 0;

        while (count < loadOrder.length && gridSlots.get(loadOrder[count]).getDetailLevel().prioritized)
            count++;

        return count;
    }
}