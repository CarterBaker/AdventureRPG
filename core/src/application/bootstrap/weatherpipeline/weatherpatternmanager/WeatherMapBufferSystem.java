package application.bootstrap.weatherpipeline.weatherpatternmanager;

import java.util.Arrays;

import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.util.WeatherScaleUtility;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import application.bootstrap.weatherpipeline.weather.WeatherWindowStruct;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;
import engine.util.mathematics.vectors.Vector4Int;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WeatherMapBufferSystem extends SystemPackage {

    /*
     * Packs each grid's weather window into its WeatherMapData UBO in
     * LATE_UPDATE. Cloud archetypes in the window become altitude-ordered
     * layers with their settings in world blocks, and every cell packs
     * per-layer coverage and density into one ivec4.
     */

    // Internal
    private WeatherPatternManager weatherPatternManager;
    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;
    private WorldManager worldManager;

    // Cells
    private Vector4Int[] cells;

    // Layers
    private int layerCount;
    private CloudHandle[] layerHandles;
    private int[] cloudTypeIndex2LayerSlot;
    private Vector4[] layerColor;
    private Vector4[] layerShape;
    private Vector4[] layerNoise;
    private Vector4[] layerSurface;

    // Scratch
    private final WeatherWindowStruct windowScratch = new WeatherWindowStruct();
    private final Vector2 shapeOriginScratch = new Vector2();
    private final Vector4 mapOrigin = new Vector4();
    private final Vector2 planet = new Vector2();
    private float[] cellCoverage;
    private float[] cellDensityWeighted;
    private float[] cellDensityWeight;

    // Base \\

    @Override
    protected void create() {

        int maxLayers = EngineSetting.WEATHER_MAP_MAX_LAYERS;

        // Cells
        this.cells = new Vector4Int[EngineSetting.WEATHER_MAP_RESOLUTION * EngineSetting.WEATHER_MAP_RESOLUTION];

        for (int i = 0; i < cells.length; i++)
            cells[i] = new Vector4Int();

        // Layers
        this.layerHandles = new CloudHandle[EngineSetting.MAX_CLOUD_TYPES];
        this.cloudTypeIndex2LayerSlot = new int[EngineSetting.MAX_CLOUD_TYPES];
        this.layerColor = allocate(maxLayers);
        this.layerShape = allocate(maxLayers);
        this.layerNoise = allocate(maxLayers);
        this.layerSurface = allocate(maxLayers);

        // Scratch
        this.cellCoverage = new float[maxLayers];
        this.cellDensityWeighted = new float[maxLayers];
        this.cellDensityWeight = new float[maxLayers];
    }

    @Override
    protected void get() {
        this.weatherPatternManager = get(WeatherPatternManager.class);
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.worldManager = get(WorldManager.class);
    }

    // Update \\

    @Override
    protected void lateUpdate() {

        if (!weatherPatternManager.hasActiveMap())
            return;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            writeGrid((GridInstance) elements[i]);
    }

    private void writeGrid(GridInstance grid) {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        weatherPatternManager.resolveWindow(grid, windowScratch);

        resolveLayers();
        writeLayerTable(grid, activeWorld);
        writeCells();

        mapOrigin.set(
                windowScratch.getMapOriginXBlocks(),
                windowScratch.getMapOriginZBlocks(),
                weatherPatternManager.getCellSizeBlocks(),
                weatherPatternManager.getShapePeriodBlocks());

        planet.set(WeatherScaleUtility.resolvePlanetRadiusBlocks(activeWorld), EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS);

        UBOInstance weatherMapUBO = grid.getWeatherMapUBO();

        weatherMapUBO.updateUniform(EngineSetting.UNIFORM_WEATHER_CELLS, cells);
        weatherMapUBO.updateUniform(EngineSetting.UNIFORM_WEATHER_LAYER_COLOR, layerColor);
        weatherMapUBO.updateUniform(EngineSetting.UNIFORM_WEATHER_LAYER_SHAPE, layerShape);
        weatherMapUBO.updateUniform(EngineSetting.UNIFORM_WEATHER_LAYER_NOISE, layerNoise);
        weatherMapUBO.updateUniform(EngineSetting.UNIFORM_WEATHER_LAYER_SURFACE, layerSurface);
        weatherMapUBO.updateUniform(EngineSetting.UNIFORM_WEATHER_MAP_ORIGIN, mapOrigin);
        weatherMapUBO.updateUniform(EngineSetting.UNIFORM_WEATHER_PLANET, planet);
        weatherMapUBO.updateUniform(EngineSetting.UNIFORM_WEATHER_LAYER_COUNT, layerCount);

        uboManager.push(weatherMapUBO);
    }

    // Layers \\

    private void resolveLayers() {

        Arrays.fill(layerHandles, null);
        Arrays.fill(cloudTypeIndex2LayerSlot, -1);

        int resolution = weatherPatternManager.getMapResolution();

        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {

                WeatherInstance cell = weatherPatternManager.getCell(windowScratch, x, z);

                if (cell == null)
                    continue;

                collectCloudTypes(cell.getPreviousWeatherHandle());
                collectCloudTypes(cell.getWeatherHandle());
            }
        }

        layerCount = 0;

        for (int i = 0; i < layerHandles.length; i++)
            if (layerHandles[i] != null)
                layerHandles[layerCount++] = layerHandles[i];

        Arrays.fill(layerHandles, layerCount, layerHandles.length, null);
        sortLayersByAltitude();

        layerCount = Math.min(layerCount, EngineSetting.WEATHER_MAP_MAX_LAYERS);

        for (int slot = 0; slot < layerCount; slot++)
            cloudTypeIndex2LayerSlot[layerHandles[slot].getCloudTypeIndex()] = slot;
    }

    private void collectCloudTypes(WeatherHandle weatherHandle) {

        int cloudCount = weatherHandle.getCloudCount();

        for (int i = 0; i < cloudCount; i++) {
            CloudHandle cloudHandle = weatherHandle.getCloudHandle(i);
            layerHandles[cloudHandle.getCloudTypeIndex()] = cloudHandle;
        }
    }

    private void sortLayersByAltitude() {

        for (int i = 1; i < layerCount; i++) {

            CloudHandle handle = layerHandles[i];
            int j = i - 1;

            while (j >= 0 && layerHandles[j].getBaseAltitudeKm() > handle.getBaseAltitudeKm()) {
                layerHandles[j + 1] = layerHandles[j];
                j--;
            }

            layerHandles[j + 1] = handle;
        }
    }

    private void writeLayerTable(GridInstance grid, WorldHandle activeWorld) {

        float shapePeriodBlocks = weatherPatternManager.getShapePeriodBlocks();

        for (int slot = 0; slot < layerCount; slot++) {

            CloudHandle cloud = layerHandles[slot];
            float scaleBlocks = WeatherScaleUtility.kilometersToBlocks(activeWorld, cloud.getScaleKm());

            weatherPatternManager.resolveShapeOrigin(grid, cloud.getDriftSpeedScale(), shapeOriginScratch);

            layerColor[slot].set(
                    cloud.getCloudColor().x,
                    cloud.getCloudColor().y,
                    cloud.getCloudColor().z,
                    cloud.getSaturation());

            layerShape[slot].set(
                    EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS
                            + WeatherScaleUtility.kilometersToBlocks(activeWorld, cloud.getBaseAltitudeKm()),
                    WeatherScaleUtility.kilometersToBlocks(activeWorld, cloud.getVerticalThicknessKm()),
                    cloud.getDensity(),
                    cloud.getFullness());

            layerNoise[slot].set(
                    resolveLatticeCount(shapePeriodBlocks, scaleBlocks * cloud.getElongation()),
                    resolveLatticeCount(shapePeriodBlocks, scaleBlocks),
                    Math.max(1f, Math.round(EngineSetting.CLOUD_DETAIL_FREQUENCY_RATIO * cloud.getDensityNoiseScale())),
                    cloud.getNoiseWarpStrength());

            layerSurface[slot].set(
                    shapeOriginScratch.x,
                    shapeOriginScratch.y,
                    cloud.getCoverageBias(),
                    cloud.getSilhouetteSoftness());
        }
    }

    // Whole lattice cells across the shape period, so the shape noise tiles
    // exactly at the period and never seams where the world wraps.
    private float resolveLatticeCount(float shapePeriodBlocks, float featureSizeBlocks) {
        return Math.max(1f, Math.round(shapePeriodBlocks / Math.max(featureSizeBlocks, 1f)));
    }

    // Cells \\

    private void writeCells() {

        int resolution = weatherPatternManager.getMapResolution();

        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {

                WeatherInstance cell = weatherPatternManager.getCell(windowScratch, x, z);
                Vector4Int packed = cells[z * resolution + x];

                if (cell == null || layerCount == 0) {
                    packed.set(0, 0, 0, 0);
                    continue;
                }

                resolveCellLayers(cell);

                packed.set(
                        packCoverage(0),
                        packCoverage(EngineSetting.WEATHER_MAP_LAYERS_PER_COMPONENT),
                        packDensity(0),
                        packDensity(EngineSetting.WEATHER_MAP_LAYERS_PER_COMPONENT));
            }
        }
    }

    private void resolveCellLayers(WeatherInstance cell) {

        Arrays.fill(cellCoverage, 0f);
        Arrays.fill(cellDensityWeighted, 0f);
        Arrays.fill(cellDensityWeight, 0f);

        WeatherHandle previousWeather = cell.getPreviousWeatherHandle();
        WeatherHandle currentWeather = cell.getWeatherHandle();
        float transitionT = cell.getEasedTransitionT();

        if (previousWeather == currentWeather || transitionT >= 1f) {
            accumulateWeather(currentWeather, 1f);
            return;
        }

        accumulateWeather(previousWeather, 1f - transitionT);
        accumulateWeather(currentWeather, transitionT);
    }

    private void accumulateWeather(WeatherHandle weatherHandle, float weight) {

        int cloudCount = weatherHandle.getCloudCount();

        for (int i = 0; i < cloudCount; i++) {

            int slot = cloudTypeIndex2LayerSlot[weatherHandle.getCloudHandle(i).getCloudTypeIndex()];

            if (slot < 0)
                continue;

            cellCoverage[slot] += weatherHandle.getCloudCoverage(i) * weight;
            cellDensityWeighted[slot] += weatherHandle.getCloudDensityScale(i) * weight;
            cellDensityWeight[slot] += weight;
        }
    }

    // Packing \\

    private int packCoverage(int firstSlot) {

        int packed = 0;

        for (int i = 0; i < EngineSetting.WEATHER_MAP_LAYERS_PER_COMPONENT; i++)
            packed |= toByte(cellCoverage[firstSlot + i]) << (i * Byte.SIZE);

        return packed;
    }

    private int packDensity(int firstSlot) {

        int packed = 0;

        for (int i = 0; i < EngineSetting.WEATHER_MAP_LAYERS_PER_COMPONENT; i++) {

            int slot = firstSlot + i;
            float density = cellDensityWeight[slot] > 0f
                    ? cellDensityWeighted[slot] / cellDensityWeight[slot]
                    : 0f;

            packed |= toByte(density / EngineSetting.WEATHER_MAP_DENSITY_SCALE_MAX) << (i * Byte.SIZE);
        }

        return packed;
    }

    private int toByte(float unit) {
        return Math.round(Math.max(0f, Math.min(1f, unit)) * EngineSetting.WEATHER_MAP_CHANNEL_MAX);
    }

    // Utility \\

    private static Vector4[] allocate(int size) {

        Vector4[] array = new Vector4[size];

        for (int i = 0; i < size; i++)
            array[i] = new Vector4();

        return array;
    }
}
