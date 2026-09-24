package application.bootstrap.weatherpipeline.weatherpatternmanager;

import java.util.Arrays;

import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import application.bootstrap.weatherpipeline.weather.WeatherWindowStruct;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;
import engine.util.mathematics.vectors.Vector4Int;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WeatherMapBufferSystem extends SystemPackage {

    /*
     * Packs each grid's weather window into its own WeatherMapData UBO every
     * frame. The cloud archetypes present anywhere in the window become the
     * frame's layers, ordered by altitude, and each layer's archetype
     * settings are written once into a small table. Every cell is then a
     * single ivec4: one byte of coverage and one byte of density scale per
     * layer, cross-faded across the cell's own weather transition, so the
     * shader reads the whole sky with one fetch per cell and interpolates
     * between cells itself. Runs in LATE_UPDATE so the window is placed
     * against the reference chunk physics settled on this frame.
     */

    // Internal
    private WeatherPatternManager weatherPatternManager;
    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;

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

        weatherPatternManager.resolveWindow(grid, windowScratch);

        resolveLayers();
        writeLayerTable(grid);
        writeCells();

        mapOrigin.set(
                windowScratch.getMapOriginXBlocks(),
                windowScratch.getMapOriginZBlocks(),
                weatherPatternManager.getCellSizeBlocks(),
                weatherPatternManager.getDomeRangeBlocks());

        UBOInstance weatherMapUBO = grid.getWeatherMapUBO();

        weatherMapUBO.updateUniform("u_weatherCells", cells);
        weatherMapUBO.updateUniform("u_weatherLayerColor", layerColor);
        weatherMapUBO.updateUniform("u_weatherLayerShape", layerShape);
        weatherMapUBO.updateUniform("u_weatherLayerNoise", layerNoise);
        weatherMapUBO.updateUniform("u_weatherLayerSurface", layerSurface);
        weatherMapUBO.updateUniform("u_weatherMapOrigin", mapOrigin);
        weatherMapUBO.updateUniform("u_weatherShapePeriod", weatherPatternManager.getShapePeriodBlocks());
        weatherMapUBO.updateUniform("u_weatherLayerCount", layerCount);

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

            while (j >= 0 && layerHandles[j].getBaseAltitude() > handle.getBaseAltitude()) {
                layerHandles[j + 1] = layerHandles[j];
                j--;
            }

            layerHandles[j + 1] = handle;
        }
    }

    private void writeLayerTable(GridInstance grid) {

        float shapePeriodBlocks = weatherPatternManager.getShapePeriodBlocks();

        for (int slot = 0; slot < layerCount; slot++) {

            CloudHandle cloud = layerHandles[slot];

            weatherPatternManager.resolveShapeOrigin(grid, cloud.getDriftSpeedScale(), shapeOriginScratch);

            layerColor[slot].set(
                    cloud.getCloudColor().x,
                    cloud.getCloudColor().y,
                    cloud.getCloudColor().z,
                    cloud.getSaturation());

            layerShape[slot].set(
                    cloud.getBaseAltitude(),
                    cloud.getVerticalThickness(),
                    cloud.getDensity(),
                    cloud.getFullness());

            layerNoise[slot].set(
                    resolveLatticeCount(shapePeriodBlocks, cloud.getScale() * cloud.getElongation()),
                    resolveLatticeCount(shapePeriodBlocks, cloud.getScale()),
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
