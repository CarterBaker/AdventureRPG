package application.bootstrap.oceanpipeline.turbulencemanager;

import java.util.Arrays;

import application.bootstrap.oceanpipeline.tidemanager.TideManager;
import application.bootstrap.oceanpipeline.turbulence.TurbulenceInstance;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import application.bootstrap.weatherpipeline.weather.WeatherWindowStruct;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternManager;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.NoiseUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TurbulenceManager extends ManagerPackage {

    /*
     * Owns ocean turbulence — how rough the sea is at any point of the world
     * — and the wave set that turbulence drives. Strength comes from weather:
     * each weather's blended wind speed, wind turbulence, and precipitation,
     * swung over time by per-cell gust noise so no two storms breathe in
     * step. Every frame each grid's TurbulenceInstance is rebuilt from its own
     * local weather as a baseline plus one cell per nearby weather-map cell
     * whose weather differs from it, nearest first, so a storm drifting in
     * roughens the sea ahead of it while the water underfoot stays calm. The
     * wave set is a few directional components travelling with the
     * prevailing wind, each wave vector snapped to the world's wrap period so
     * the sea tiles across the world seam, and each grid's phase for them is
     * folded from its reference chunk and the elapsed time in double
     * precision. The sampling methods here are the CPU side of exactly what
     * WaterShader evaluates, so gameplay can ask how high the sea stands at
     * any position.
     */

    // Internal
    private WeatherPatternManager weatherPatternManager;
    private WindManager windManager;
    private WorldManager worldManager;
    private WorldStreamManager worldStreamManager;
    private TideManager tideManager;

    // Time
    private double elapsedSeconds;

    // Waves
    private WorldHandle waveWorldHandle;
    private double[] waveVectorX;
    private double[] waveVectorZ;
    private double[] waveAngularSpeed;
    private float[] waveAmplitudeShare;

    // Scratch
    private final WeatherWindowStruct windowScratch = new WeatherWindowStruct();
    private long[] sortScratch;
    private float[] cellStrengthScratch;

    // Base \\

    @Override
    protected void create() {

        // Time
        this.elapsedSeconds = 0.0;

        // Waves
        this.waveVectorX = new double[EngineSetting.OCEAN_WAVE_COUNT];
        this.waveVectorZ = new double[EngineSetting.OCEAN_WAVE_COUNT];
        this.waveAngularSpeed = new double[EngineSetting.OCEAN_WAVE_COUNT];
        this.waveAmplitudeShare = new float[EngineSetting.OCEAN_WAVE_COUNT];

        // Scratch
        int mapCellCount = EngineSetting.WEATHER_MAP_RESOLUTION * EngineSetting.WEATHER_MAP_RESOLUTION;
        this.sortScratch = new long[mapCellCount];
        this.cellStrengthScratch = new float[mapCellCount];

        create(TurbulenceBufferSystem.class);
    }

    @Override
    protected void get() {

        // Internal
        this.weatherPatternManager = get(WeatherPatternManager.class);
        this.windManager = get(WindManager.class);
        this.worldManager = get(WorldManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.tideManager = get(TideManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        elapsedSeconds += internal.getDeltaTime();

        WorldHandle activeWorld = worldManager.getActiveWorld();

        if (activeWorld != waveWorldHandle)
            resolveWaveSet(activeWorld);

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
        resolveWavePhases(turbulence, grid.getActiveChunkCoordinate());
    }

    // Cells \\

    /*
     * Every weather cell near the grid whose weather stirs the sea differently
     * from the grid's own ambient weather becomes one turbulence cell, nearest
     * first, its influence faded out toward the edge of
     * OCEAN_TURBULENCE_WEATHER_RANGE_CELLS so a cell sliding out of reach
     * never drops out abruptly.
     */
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

    // Waves \\

    private void resolveWaveSet(WorldHandle activeWorld) {

        Vector3 windDirection = windManager.getWindHandle().getGlobalWindDirection();

        double baseAngle = Math.atan2(windDirection.z, windDirection.x);
        double worldWidthBlocks = activeWorld.getWorldScale().x;
        double worldHeightBlocks = activeWorld.getWorldScale().y;
        float shareSum = 0f;

        for (int i = 0; i < EngineSetting.OCEAN_WAVE_COUNT; i++) {

            double angle = baseAngle + Math.toRadians(EngineSetting.OCEAN_WAVE_ANGLES_DEGREES[i]);
            double wavelength = EngineSetting.OCEAN_WAVE_BASE_WAVELENGTH_BLOCKS
                    * EngineSetting.OCEAN_WAVE_WAVELENGTH_RATIOS[i];
            double waveNumber = Math.PI * 2.0 / wavelength;

            waveVectorX[i] = snapToWrapPeriod(Math.cos(angle) * waveNumber, worldWidthBlocks);
            waveVectorZ[i] = snapToWrapPeriod(Math.sin(angle) * waveNumber, worldHeightBlocks);

            double snappedWaveNumber = Math.sqrt(waveVectorX[i] * waveVectorX[i] + waveVectorZ[i] * waveVectorZ[i]);

            waveAngularSpeed[i] = Math.sqrt(EngineSetting.GRAVITY_FORCE * snappedWaveNumber)
                    * EngineSetting.OCEAN_WAVE_SPEED_SCALE;
            waveAmplitudeShare[i] = EngineSetting.OCEAN_WAVE_AMPLITUDE_RATIOS[i];
            shareSum += waveAmplitudeShare[i];
        }

        for (int i = 0; i < EngineSetting.OCEAN_WAVE_COUNT; i++)
            waveAmplitudeShare[i] /= shareSum;

        waveWorldHandle = activeWorld;
    }

    private double snapToWrapPeriod(double waveVectorComponent, double periodBlocks) {

        double cyclesPerPeriod = Math.PI * 2.0 / periodBlocks;

        return Math.round(waveVectorComponent / cyclesPerPeriod) * cyclesPerPeriod;
    }

    private void resolveWavePhases(TurbulenceInstance turbulence, long referenceCoordinate) {

        double originX = (double) Coordinate2Long.unpackX(referenceCoordinate) * EngineSetting.CHUNK_SIZE;
        double originZ = (double) Coordinate2Long.unpackY(referenceCoordinate) * EngineSetting.CHUNK_SIZE;

        for (int i = 0; i < EngineSetting.OCEAN_WAVE_COUNT; i++) {

            double phase = waveVectorX[i] * originX + waveVectorZ[i] * originZ
                    - waveAngularSpeed[i] * elapsedSeconds;

            turbulence.setWavePhase(i, (float) (phase - Math.floor(phase / (Math.PI * 2.0)) * (Math.PI * 2.0)));
        }
    }

    // On-Demand \\

    public float sampleTurbulence(GridInstance grid, double worldBlockX, double worldBlockZ) {
        return grid.getTurbulenceInstance().sampleStrength(
                toRelativeX(grid, worldBlockX),
                toRelativeZ(grid, worldBlockZ));
    }

    public float sampleSurfaceHeightBlocks(GridInstance grid, double worldBlockX, double worldBlockZ) {

        TurbulenceInstance turbulence = grid.getTurbulenceInstance();

        float relativeX = toRelativeX(grid, worldBlockX);
        float relativeZ = toRelativeZ(grid, worldBlockZ);
        float amplitude = turbulence.sampleWaveAmplitudeBlocks(relativeX, relativeZ);
        float swell = 0f;

        for (int i = 0; i < EngineSetting.OCEAN_WAVE_COUNT; i++)
            swell += waveAmplitudeShare[i] * (float) Math.sin(
                    waveVectorX[i] * relativeX + waveVectorZ[i] * relativeZ + turbulence.getWavePhase(i));

        return tideManager.getSurfaceHeightBlocks() + amplitude * swell;
    }

    private float toRelativeX(GridInstance grid, double worldBlockX) {

        double originX = (double) Coordinate2Long.unpackX(grid.getActiveChunkCoordinate()) * EngineSetting.CHUNK_SIZE;

        return (float) WorldWrapUtility.wrappedDelta(worldBlockX, originX, grid.getWorldHandle().getWorldScale().x);
    }

    private float toRelativeZ(GridInstance grid, double worldBlockZ) {

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

    public float getWaveVectorX(int waveIndex) {
        return (float) waveVectorX[waveIndex];
    }

    public float getWaveVectorZ(int waveIndex) {
        return (float) waveVectorZ[waveIndex];
    }

    public float getWaveAmplitudeShare(int waveIndex) {
        return waveAmplitudeShare[waveIndex];
    }
}
