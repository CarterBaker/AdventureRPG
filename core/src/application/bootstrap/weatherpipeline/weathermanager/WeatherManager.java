package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weather.WeatherPoolEntryInstance;
import application.bootstrap.weatherpipeline.weatherband.WeatherBandInstance;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.WeatherChanceStruct;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class WeatherManager extends ManagerPackage {

    /*
     * Owns the weather definition palette and resolves the active biome/season
     * into a chance-weighted pool of candidate weathers. Band resolution is
     * reference-agnostic — callers supply whichever chunk coordinate they want
     * the "toward horizon" blend measured from. Pool entries are recycled
     * WeatherPoolEntryInstances, grown once and never reallocated per-entry.
     */

    private static final float NEXT_WEATHER_SUGGESTION_INFLUENCE = 1.5f;

    private ClockManager clockManager;
    private BiomeManager biomeManager;
    private SeasonManager seasonManager;

    private GlobalNoiseSystem globalNoiseSystem;
    private RegionSampleSystem regionSampleSystem;

    private Object2ShortOpenHashMap<String> weatherName2WeatherID;
    private Short2ObjectOpenHashMap<WeatherHandle> weatherID2WeatherHandle;

    private String lastSeason;
    private boolean weatherPoolResolved;

    // Active Pool — recycled across season changes
    private WeatherPoolEntryInstance[] activeEntryPool;
    private final ObjectArrayList<WeatherPoolEntryInstance> activeWeatherPool = new ObjectArrayList<>();

    // Biased Pool — recycled scratch for "next weather" biasing
    private WeatherPoolEntryInstance[] biasedEntryPool;
    private final ObjectArrayList<WeatherPoolEntryInstance> biasedPoolScratch = new ObjectArrayList<>();

    @Override
    protected void create() {

        this.weatherName2WeatherID = new Object2ShortOpenHashMap<>();
        this.weatherID2WeatherHandle = new Short2ObjectOpenHashMap<>();

        this.globalNoiseSystem = create(GlobalNoiseSystem.class);
        this.regionSampleSystem = create(RegionSampleSystem.class);

        this.activeEntryPool = new WeatherPoolEntryInstance[0];
        this.biasedEntryPool = new WeatherPoolEntryInstance[0];

        create(WeatherLoader.class);
    }

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.seasonManager = get(SeasonManager.class);
    }

    @Override
    protected void update() {

        String currentSeason = clockManager.getClockHandle().getCurrentSeason();

        if (currentSeason != null && !currentSeason.equals(lastSeason)) {
            BiomeHandle activeBiome = resolveActiveBiome();
            resolveWeatherPool(activeBiome, currentSeason);
            this.lastSeason = currentSeason;
        }
    }

    // Biome Selection \\

    private BiomeHandle resolveActiveBiome() {
        return biomeManager.getBiomeHandleFromBiomeName(EngineSetting.DEFAULT_BIOME_NAME);
    }

    // Management \\

    void addWeatherHandle(WeatherHandle weatherHandle) {

        if (weatherID2WeatherHandle.containsKey(weatherHandle.getWeatherID())) {
            WeatherHandle existing = weatherID2WeatherHandle.get(weatherHandle.getWeatherID());
            if (RegistryUtility.isCollision(weatherHandle.getWeatherName(), existing.getWeatherName(),
                    weatherHandle.getWeatherID()))
                throwException("Weather ID collision: '"
                        + weatherHandle.getWeatherName() + "' collides with '"
                        + existing.getWeatherName() + "' (ID " + weatherHandle.getWeatherID()
                        + ") — rename one weather to resolve");
        }

        weatherName2WeatherID.put(weatherHandle.getWeatherName(), weatherHandle.getWeatherID());
        weatherID2WeatherHandle.put(weatherHandle.getWeatherID(), weatherHandle);
    }

    // On-Demand \\

    public void request(String weatherName) {
        ((WeatherLoader) internalLoader).request(weatherName);
    }

    // Biome Resolution \\

    private void resolveWeatherPool(BiomeHandle biomeHandle, String season) {

        String resolvedSeason = season;
        ObjectArrayList<WeatherChanceStruct> entries = biomeHandle.getWeatherEntriesForSeason(season);

        if (entries.isEmpty()) {
            resolvedSeason = resolveFallbackSeasonName(biomeHandle, season);
            entries = biomeHandle.getWeatherEntriesForSeason(resolvedSeason);
        }

        float precipitationBias = resolvePrecipitationBias(resolvedSeason);
        int size = entries.size();

        activeEntryPool = growEntryPool(activeEntryPool, size);
        activeWeatherPool.clear();

        for (int i = 0; i < size; i++) {

            WeatherChanceStruct entry = entries.get(i);
            WeatherHandle handle = getWeatherHandleFromWeatherName(entry.getWeatherName());
            float chance = entry.getChance();

            if (handle.getPrecipitationIntensity() > 0f)
                chance *= precipitationBias;

            WeatherPoolEntryInstance scratchEntry = activeEntryPool[i];
            scratchEntry.constructor(handle, chance);
            activeWeatherPool.add(scratchEntry);
        }

        weatherPoolResolved = true;
    }

    private float resolvePrecipitationBias(String season) {
        return seasonManager.getSeasonHandleFromSeasonName(season).getPrecipitationChanceScale();
    }

    private String resolveFallbackSeasonName(BiomeHandle biomeHandle, String season) {

        if (!biomeHandle.hasAnyWeathers())
            throwException("Biome \"" + biomeHandle.getBiomeName() +
                    "\" has no \"weathers\" block defined at all — cannot resolve any season, including \""
                    + season + "\"");

        String fallbackSeason = biomeHandle.getDefinedSeasonNames().get(0);

        errorLog("[WeatherManager] Biome \"" + biomeHandle.getBiomeName() +
                "\" has no weathers defined for season \"" + season + "\" — falling back to \"" +
                fallbackSeason + "\". Defined seasons: " + biomeHandle.getDefinedSeasonNames());

        return fallbackSeason;
    }

    // Next Weather Bias \\

    private ObjectArrayList<WeatherPoolEntryInstance> buildBiasedPool(WeatherHandle currentWeather) {

        if (!currentWeather.hasNextWeatherSuggestions())
            return activeWeatherPool;

        int size = activeWeatherPool.size();
        biasedEntryPool = growEntryPool(biasedEntryPool, size);

        biasedPoolScratch.clear();

        for (int i = 0; i < size; i++) {

            WeatherPoolEntryInstance entry = activeWeatherPool.get(i);
            float suggestionChance = currentWeather.getNextWeatherChanceFor(entry.getWeatherHandle());
            float biasedChance = entry.getChance() + suggestionChance * NEXT_WEATHER_SUGGESTION_INFLUENCE;

            WeatherPoolEntryInstance scratchEntry = biasedEntryPool[i];
            scratchEntry.constructor(entry.getWeatherHandle(), biasedChance);
            biasedPoolScratch.add(scratchEntry);
        }

        return biasedPoolScratch;
    }

    private WeatherPoolEntryInstance[] growEntryPool(WeatherPoolEntryInstance[] pool, int size) {

        if (pool.length >= size)
            return pool;

        WeatherPoolEntryInstance[] grown = new WeatherPoolEntryInstance[size];
        System.arraycopy(pool, 0, grown, 0, pool.length);

        for (int i = pool.length; i < grown.length; i++) {
            WeatherPoolEntryInstance entry = create(WeatherPoolEntryInstance.class);
            entry.constructor(null, 0f);
            grown[i] = entry;
        }

        return grown;
    }

    // Accessible \\

    public boolean hasWeather(String weatherName) {
        return weatherName2WeatherID.containsKey(weatherName);
    }

    public short getWeatherIDFromWeatherName(String weatherName) {

        if (!weatherName2WeatherID.containsKey(weatherName))
            request(weatherName);

        return weatherName2WeatherID.getShort(weatherName);
    }

    public WeatherHandle getWeatherHandleFromWeatherID(short weatherID) {

        WeatherHandle handle = weatherID2WeatherHandle.get(weatherID);

        if (handle == null)
            throwException("No handle registered for weather ID: " + weatherID);

        return handle;
    }

    public WeatherHandle getWeatherHandleFromWeatherName(String weatherName) {
        return getWeatherHandleFromWeatherID(getWeatherIDFromWeatherName(weatherName));
    }

    public boolean hasActiveWeatherPool() {
        return weatherPoolResolved;
    }

    public float getEffectiveOuterRangeChunks() {
        return regionSampleSystem.getEffectiveOuterRangeChunks();
    }

    public float getEffectiveNearRangeChunks() {
        return regionSampleSystem.getEffectiveNearRangeChunks();
    }

    public float getWorldDriftChunksPerSecondX() {
        return globalNoiseSystem.getWorldDriftChunksPerSecondX();
    }

    public float getGlobalStormIntensityAt(long chunkCoordinate) {
        return globalNoiseSystem.sampleGlobalIntensity(chunkCoordinate);
    }

    public void resolveWeatherBand(WeatherBandInstance out, long chunkCoordinate) {

        if (!weatherPoolResolved)
            throwException("Cannot resolve a weather band before any season has been resolved. "
                    + "Callers should check hasActiveWeatherPool() first.");

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        regionSampleSystem.resolveBand(out, chunkX, chunkZ, activeWeatherPool);
    }

    public void resolveWeatherBandTowardHorizon(
            WeatherBandInstance out,
            long homeChunkCoordinate,
            long referenceChunkCoordinate) {
        resolveWeatherBandTowardHorizonInternal(out, homeChunkCoordinate, referenceChunkCoordinate, null);
    }

    public void resolveWeatherBandTowardHorizonBiased(
            WeatherBandInstance out,
            long homeChunkCoordinate,
            long referenceChunkCoordinate,
            WeatherHandle currentWeather) {
        resolveWeatherBandTowardHorizonInternal(out, homeChunkCoordinate, referenceChunkCoordinate, currentWeather);
    }

    private void resolveWeatherBandTowardHorizonInternal(
            WeatherBandInstance out,
            long homeChunkCoordinate,
            long referenceChunkCoordinate,
            WeatherHandle currentWeather) {

        if (!weatherPoolResolved)
            throwException("Cannot resolve a weather band before any season has been resolved. "
                    + "Callers should check hasActiveWeatherPool() first.");

        int homeChunkX = Coordinate2Long.unpackX(homeChunkCoordinate);
        int homeChunkZ = Coordinate2Long.unpackY(homeChunkCoordinate);

        int referenceChunkX = Coordinate2Long.unpackX(referenceChunkCoordinate);
        int referenceChunkZ = Coordinate2Long.unpackY(referenceChunkCoordinate);

        ObjectArrayList<WeatherPoolEntryInstance> pool = currentWeather == null
                ? activeWeatherPool
                : buildBiasedPool(currentWeather);

        regionSampleSystem.resolveBandTowardHorizon(
                out, homeChunkX, homeChunkZ, referenceChunkX, referenceChunkZ, pool);
    }
}