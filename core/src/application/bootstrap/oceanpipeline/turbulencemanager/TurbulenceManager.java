package application.bootstrap.oceanpipeline.turbulencemanager;

import java.util.Arrays;

import application.bootstrap.oceanpipeline.tidemanager.TideManager;
import application.bootstrap.oceanpipeline.turbulence.TurbulenceInstance;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
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
     * swung over time by per-pattern gust noise so no two storms breathe in
     * step. Every frame each grid's TurbulenceInstance is rebuilt from its own
     * local weather as a baseline plus one cell per weather pattern in range,
     * nearest first, so a storm on the horizon raises distant waves while the
     * water underfoot stays calm. The wave set is a few directional
     * components travelling with the prevailing wind, each wave vector
     * snapped to the world's wrap period so the sea tiles across the world
     * seam, and each grid's phase for them is folded from its reference chunk
     * and the elapsed time in double precision. The sampling methods here are
     * the CPU side of exactly what WaterShader evaluates, so gameplay can ask
     * how high the sea stands at any position.
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
    private long[] sortScratch;

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
        this.sortScratch = new long[EngineSetting.WEATHER_PATTERN_MAX_ACTIVE_COUNT];

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
            resolveGrid((GridInstance) elements[i], activeWorld);
    }

    private void resolveGrid(GridInstance grid, WorldHandle activeWorld) {

        TurbulenceInstance turbulence = grid.getTurbulenceInstance();
        WeatherInstance localWeather = grid.getWeatherInstance();

        float ambientStrength = localWeather.isConfigured()
                ? computeStrength(localWeather, EngineSetting.WEATHER_PATTERN_LOCAL_KEY_SEED)
                : 0f;

        turbulence.beginField(ambientStrength);

        long referenceCoordinate = grid.getActiveChunkCoordinate();

        resolveCells(turbulence, referenceCoordinate, activeWorld);
        resolveWavePhases(turbulence, referenceCoordinate);
    }

    // Cells \\

    private void resolveCells(TurbulenceInstance turbulence, long referenceCoordinate, WorldHandle activeWorld) {

        int refChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int refChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        float rangeChunks = weatherPatternManager.getRangeChunks();
        WeatherInstance[] pool = weatherPatternManager.getPatternPool();
        int patternCount = 0;

        for (int slot = 0; slot < pool.length; slot++) {

            if (!weatherPatternManager.isPatternActive(slot))
                continue;

            WeatherInstance pattern = pool[slot];

            double dx = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkX(), refChunkX, worldWidthChunks);
            double dz = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkZ(), refChunkZ, worldHeightChunks);
            float distanceChunks = (float) Math.sqrt(dx * dx + dz * dz);

            if (distanceChunks - pattern.getFootprintRadiusChunks() > rangeChunks)
                continue;

            sortScratch[patternCount++] = ((long) Float.floatToRawIntBits(distanceChunks) << 32)
                    | (slot & 0xFFFFFFFFL);
        }

        Arrays.sort(sortScratch, 0, patternCount);

        for (int i = 0; i < patternCount; i++) {

            WeatherInstance pattern = pool[(int) (sortScratch[i] & 0xFFFFFFFFL)];

            double dx = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkX(), refChunkX, worldWidthChunks);
            double dz = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkZ(), refChunkZ, worldHeightChunks);

            boolean added = turbulence.addCell(
                    (float) (dx * EngineSetting.CHUNK_SIZE),
                    (float) (dz * EngineSetting.CHUNK_SIZE),
                    pattern.getFootprintRadiusChunks() * EngineSetting.CHUNK_SIZE,
                    pattern.getFadeAlpha() * EngineSetting.OCEAN_TURBULENCE_CELL_WEIGHT,
                    computeStrength(pattern, pattern.getPatternKey()));

            if (!added)
                return;
        }
    }

    private float computeStrength(WeatherInstance weather, long gustKey) {

        float windStrength = weather.getBlendedWindSpeedScale() * weather.getBlendedWindTurbulenceScale()
                * EngineSetting.OCEAN_TURBULENCE_WIND_WEIGHT;
        float rainStrength = weather.getBlendedPrecipitationIntensity()
                * EngineSetting.OCEAN_TURBULENCE_PRECIPITATION_WEIGHT;

        float gust = NoiseUtility.noise2(
                EngineSetting.OCEAN_TURBULENCE_SEED ^ gustKey,
                elapsedSeconds * EngineSetting.OCEAN_TURBULENCE_GUST_FREQUENCY,
                0.0);

        float strength = (windStrength + rainStrength) * (1f + gust * EngineSetting.OCEAN_TURBULENCE_GUST_VARIANCE);

        return Math.max(0f, Math.min(EngineSetting.OCEAN_TURBULENCE_MAX_STRENGTH, strength));
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
