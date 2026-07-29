package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
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
     * Owns the weather definition palette and resolves the active biome/
     * season into a chance-weighted pool of candidate weathers. Band
     * resolution is reference-agnostic — callers supply whichever chunk
     * coordinate they want the "toward horizon" blend measured from, so
     * WeatherPatternManager can resolve bands relative to any number of
     * active grids rather than one pinned reference.
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
    private ObjectArrayList<WeatherPoolEntryStruct> activeWeatherPool;

    // Next Weather Bias Scratch — grown once, never shrunk, mutated in
    // place each reevaluation instead of allocating a fresh pool/entries.
    private WeatherPoolEntryStruct[] biasedEntryPool;
    private final ObjectArrayList<WeatherPoolEntryStruct> biasedPoolScratch = new ObjectArrayList<>();

    @Override
    protected void create() {

        this.weatherName2WeatherID = new Object2ShortOpenHashMap<>();
        this.weatherID2WeatherHandle = new Short2ObjectOpenHashMap<>();

        this.globalNoiseSystem = create(GlobalNoiseSystem.class);
        this.regionSampleSystem = create(RegionSampleSystem.class);

        this.biasedEntryPool = new WeatherPoolEntryStruct[0];

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
            this.activeWeatherPool = resolveWeatherPool(activeBiome, currentSeason);
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

    private ObjectArrayList<WeatherPoolEntryStruct> resolveWeatherPool(BiomeHandle biomeHandle, String season) {

        String resolvedSeason = season;
        ObjectArrayList<WeatherChanceStruct> entries = biomeHandle.getWeatherEntriesForSeason(season);

        if (entries.isEmpty()) {
            resolvedSeason = resolveFallbackSeasonName(biomeHandle, season);
            entries = biomeHandle.getWeatherEntriesForSeason(resolvedSeason);
        }

        float precipitationBias = resolvePrecipitationBias(resolvedSeason);

        ObjectArrayList<WeatherPoolEntryStruct> pool = new ObjectArrayList<>(entries.size());

        for (int i = 0; i < entries.size(); i++) {

            WeatherChanceStruct entry = entries.get(i);
            WeatherHandle handle = getWeatherHandleFromWeatherName(entry.getWeatherName());
            float chance = entry.getChance();

            if (handle.getPrecipitationIntensity() > 0f)
                chance *= precipitationBias;

            pool.add(new WeatherPoolEntryStruct(handle, chance));
        }

        return pool;
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

    /*
     * Biases the active pool toward currentWeather's own suggested next
     * weathers. Reuses a grow-only pool of scratch entries and a single
     * scratch list rather than allocating fresh ones per pattern
     * reevaluation — the result is only ever read synchronously by the
     * caller within the same call and never retained past it, so reuse
     * across successive calls is safe.
     */
    private ObjectArrayList<WeatherPoolEntryStruct> buildBiasedPool(WeatherHandle currentWeather) {

        if (!currentWeather.hasNextWeatherSuggestions())
            return activeWeatherPool;

        int size = activeWeatherPool.size();
        ensureBiasedPoolCapacity(size);

        biasedPoolScratch.clear();

        for (int i = 0; i < size; i++) {

            WeatherPoolEntryStruct entry = activeWeatherPool.get(i);
            float suggestionChance = currentWeather.getNextWeatherChanceFor(entry.getWeatherHandle());
            float biasedChance = entry.getChance() + suggestionChance * NEXT_WEATHER_SUGGESTION_INFLUENCE;

            WeatherPoolEntryStruct scratchEntry = biasedEntryPool[i];
            scratchEntry.set(entry.getWeatherHandle(), biasedChance);
            biasedPoolScratch.add(scratchEntry);
        }

        return biasedPoolScratch;
    }

    private void ensureBiasedPoolCapacity(int size) {

        if (biasedEntryPool.length >= size)
            return;

        WeatherPoolEntryStruct[] grown = new WeatherPoolEntryStruct[size];
        System.arraycopy(biasedEntryPool, 0, grown, 0, biasedEntryPool.length);

        for (int i = biasedEntryPool.length; i < grown.length; i++)
            grown[i] = new WeatherPoolEntryStruct(null, 0f);

        biasedEntryPool = grown;
    }

    // Accessible \\

    public boolean hasWeather(String weatherName) {
        return weatherName2WeatherID.containsKey(weatherName);
    }

    public short getWeatherIDFromWeatherName(String weatherName) {

        if (!weatherName2WeatherID.containsKey(weatherName))
            request(weatherName);

        return RegistryUtility.toShortID(weatherName);
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
        return activeWeatherPool != null;
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

    public void resolveWeatherBand(WeatherBandStruct out, long chunkCoordinate) {

        if (activeWeatherPool == null)
            throwException("Cannot resolve a weather band before any season has been resolved. "
                    + "Callers should check hasActiveWeatherPool() first.");

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        regionSampleSystem.resolveBand(out, chunkX, chunkZ, activeWeatherPool);
    }

    public void resolveWeatherBandTowardHorizon(
            WeatherBandStruct out,
            long homeChunkCoordinate,
            long referenceChunkCoordinate) {
        resolveWeatherBandTowardHorizonInternal(out, homeChunkCoordinate, referenceChunkCoordinate, null);
    }

    public void resolveWeatherBandTowardHorizonBiased(
            WeatherBandStruct out,
            long homeChunkCoordinate,
            long referenceChunkCoordinate,
            WeatherHandle currentWeather) {
        resolveWeatherBandTowardHorizonInternal(out, homeChunkCoordinate, referenceChunkCoordinate, currentWeather);
    }

    private void resolveWeatherBandTowardHorizonInternal(
            WeatherBandStruct out,
            long homeChunkCoordinate,
            long referenceChunkCoordinate,
            WeatherHandle currentWeather) {

        if (activeWeatherPool == null)
            throwException("Cannot resolve a weather band before any season has been resolved. "
                    + "Callers should check hasActiveWeatherPool() first.");

        int homeChunkX = Coordinate2Long.unpackX(homeChunkCoordinate);
        int homeChunkZ = Coordinate2Long.unpackY(homeChunkCoordinate);

        int referenceChunkX = Coordinate2Long.unpackX(referenceChunkCoordinate);
        int referenceChunkZ = Coordinate2Long.unpackY(referenceChunkCoordinate);

        ObjectArrayList<WeatherPoolEntryStruct> pool = currentWeather == null
                ? activeWeatherPool
                : buildBiasedPool(currentWeather);

        regionSampleSystem.resolveBandTowardHorizon(
                out, homeChunkX, homeChunkZ, referenceChunkX, referenceChunkZ, pool);
    }
}