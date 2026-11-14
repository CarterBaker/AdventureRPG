package application.bootstrap.oceanpipeline.turbulencemanager;

import java.util.Arrays;

import application.bootstrap.oceanpipeline.turbulence.TurbulenceInstance;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import application.bootstrap.weatherpipeline.weather.WeatherWindowStruct;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.NoiseUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TurbulenceManager extends ManagerPackage {

    /*
     * Owns ocean turbulence, the weather term of the sea state, and the ocean
     * clock every wave and gust runs on. Each frame rebuilds every grid's
     * TurbulenceInstance from its local weather plus nearby differing weather
     * cells, so an approaching storm roughens the sea ahead of it and a storm
     * to the west raises giant waves only on the western water. Grids resolve
     * in LATE_UPDATE, once FIXED_UPDATE has settled their reference chunk,
     * since every cell position is relative to it.
     */

    // Internal
    private WeatherPatternManager weatherPatternManager;
    private WorldStreamManager worldStreamManager;

    // Time
    private double elapsedSeconds;

    // Scratch
    private final WeatherWindowStruct windowScratch = new WeatherWindowStruct();
    private long[] sortScratch;
    private float[] cellStrengthScratch;

    // Base \\

    @Override
    protected void create() {

        // Time
        this.elapsedSeconds = 0.0;

        // Scratch
        int mapCellCount = EngineSetting.WEATHER_MAP_RESOLUTION * EngineSetting.WEATHER_MAP_RESOLUTION;
        this.sortScratch = new long[mapCellCount];
        this.cellStrengthScratch = new float[mapCellCount];
    }

    @Override
    protected void get() {

        // Internal
        this.weatherPatternManager = get(WeatherPatternManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Update \\

    @Override
    protected void update() {
        elapsedSeconds += internal.getDeltaTime();
    }

    @Override
    protected void lateUpdate() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            resolveGrid((GridInstance) elements[i]);
    }

    private void resolveGrid(GridInstance grid) {

        TurbulenceInstance turbulence = grid.getTurbulenceInstance();
        WeatherInstance localWeather = grid.getWeatherInstance();

        float ambientStrength = localWeather.isConfigured()
                ? computeStrength(localWeather, EngineSetting.WEATHER_LOCAL_KEY_SEED)
                : 0f;
        float ambientWeatherStrength = localWeather.isConfigured()
                ? computeWeatherStrength(localWeather)
                : 0f;

        turbulence.beginField(ambientStrength);

        resolveCells(turbulence, grid, ambientWeatherStrength);
    }

    // Cells \\

    private void resolveCells(TurbulenceInstance turbulence, GridInstance grid, float ambientWeatherStrength) {

        if (!weatherPatternManager.hasActiveMap())
            return;

        weatherPatternManager.resolveWindow(grid, windowScratch);

        int resolution = weatherPatternManager.getMapResolution();
        float cellSizeBlocks = weatherPatternManager.getCellSizeBlocks();
        float rangeBlocks = EngineSetting.OCEAN_TURBULENCE_WEATHER_RANGE_CELLS * cellSizeBlocks;
        int candidateCount = 0;

        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {

                WeatherInstance cell = weatherPatternManager.getCell(windowScratch, x, z);

                if (cell == null)
                    continue;

                float centerX = (x + 0.5f) * cellSizeBlocks - windowScratch.getMapOriginXBlocks();
                float centerZ = (z + 0.5f) * cellSizeBlocks - windowScratch.getMapOriginZBlocks();
                float distanceBlocks = (float) Math.sqrt(centerX * centerX + centerZ * centerZ);

                if (distanceBlocks >= rangeBlocks)
                    continue;

                if (Math.abs(computeWeatherStrength(cell) - ambientWeatherStrength)
                        <= EngineSetting.OCEAN_TURBULENCE_STRENGTH_EPSILON)
                    continue;

                int index = z * resolution + x;
                cellStrengthScratch[index] = computeStrength(cell, cell.getCellKey());
                sortScratch[candidateCount++] = ((long) Float.floatToRawIntBits(distanceBlocks) << 32)
                        | (index & 0xFFFFFFFFL);
            }
        }

        Arrays.sort(sortScratch, 0, candidateCount);

        float radiusBlocks = cellSizeBlocks * EngineSetting.OCEAN_TURBULENCE_WEATHER_CELL_RADIUS_RATIO;

        for (int i = 0; i < candidateCount; i++) {

            int index = (int) (sortScratch[i] & 0xFFFFFFFFL);
            float distanceBlocks = Float.intBitsToFloat((int) (sortScratch[i] >>> 32));
            int x = index % resolution;
            int z = index / resolution;

            boolean added = turbulence.addCell(
                    (x + 0.5f) * cellSizeBlocks - windowScratch.getMapOriginXBlocks(),
                    (z + 0.5f) * cellSizeBlocks - windowScratch.getMapOriginZBlocks(),
                    radiusBlocks,
                    EngineSetting.OCEAN_TURBULENCE_CELL_WEIGHT * resolveRangeFade(distanceBlocks, rangeBlocks),
                    cellStrengthScratch[index]);

            if (!added)
                return;
        }
    }

    private float resolveRangeFade(float distanceBlocks, float rangeBlocks) {

        float fadeStart = rangeBlocks * EngineSetting.OCEAN_TURBULENCE_WEATHER_FADE_START_RATIO;
        float t = Math.max(0f, Math.min(1f, (distanceBlocks - fadeStart) / Math.max(rangeBlocks - fadeStart, 1f)));

        return 1f - t * t * (3f - 2f * t);
    }

    private float computeStrength(WeatherInstance weather, long gustKey) {

        float gust = NoiseUtility.noise2(
                EngineSetting.OCEAN_TURBULENCE_SEED ^ gustKey,
                elapsedSeconds * EngineSetting.OCEAN_TURBULENCE_GUST_FREQUENCY,
                0.0);

        float strength = computeWeatherStrength(weather)
                * (1f + gust * EngineSetting.OCEAN_TURBULENCE_GUST_VARIANCE);

        return Math.max(0f, Math.min(EngineSetting.OCEAN_TURBULENCE_MAX_STRENGTH, strength));
    }

    private float computeWeatherStrength(WeatherInstance weather) {

        float windStrength = weather.getBlendedWindSpeedScale() * weather.getBlendedWindTurbulenceScale()
                * EngineSetting.OCEAN_TURBULENCE_WIND_WEIGHT;
        float rainStrength = weather.getBlendedPrecipitationIntensity()
                * EngineSetting.OCEAN_TURBULENCE_PRECIPITATION_WEIGHT;

        return windStrength + rainStrength;
    }

    // On-Demand \\

    public float sampleTurbulence(GridInstance grid, double worldBlockX, double worldBlockZ) {
        return grid.getTurbulenceInstance().sampleStrength(
                toRelativeX(grid, worldBlockX),
                toRelativeZ(grid, worldBlockZ));
    }

    public float toRelativeX(GridInstance grid, double worldBlockX) {

        double originX = (double) Coordinate2Long.unpackX(grid.getActiveChunkCoordinate()) * EngineSetting.CHUNK_SIZE;

        return (float) WorldWrapUtility.wrappedDelta(worldBlockX, originX, grid.getWorldHandle().getWorldScale().x);
    }

    public float toRelativeZ(GridInstance grid, double worldBlockZ) {

        double originZ = (double) Coordinate2Long.unpackY(grid.getActiveChunkCoordinate()) * EngineSetting.CHUNK_SIZE;

        return (float) WorldWrapUtility.wrappedDelta(worldBlockZ, originZ, grid.getWorldHandle().getWorldScale().y);
    }

    // Grid Factory \\

    public TurbulenceInstance createTurbulenceInstance() {
        TurbulenceInstance instance = create(TurbulenceInstance.class);
        instance.constructor();
        return instance;
    }

    // Accessible \\

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }
}
