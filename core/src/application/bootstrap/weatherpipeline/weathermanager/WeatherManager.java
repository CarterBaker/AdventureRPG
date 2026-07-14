package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.WeatherChanceStruct;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class WeatherManager extends ManagerPackage {

    /*
     * Owns the weather palette and drives the live weather simulation — the
     * fusion of the active biome, the calendar's current named season, and
     * the world's wrapped/rotating noise field. The sky dome and the
     * overhead volumetric cloud layer both resolve their own weather
     * identity through resolveWeatherBandTowardHorizon(), so the two visual
     * layers can never disagree about what's happening at a given bearing.
     * The biased variant folds a weather's own suggested successors in as a
     * soft nudge on top of that same noise-driven pick — never a guarantee.
     */

    private static final float NEXT_WEATHER_SUGGESTION_INFLUENCE = 1.5f;

    private ClockManager clockManager;
    private BiomeManager biomeManager;
    private WindowManager windowManager;
    private PlayerManager playerManager;
    private SeasonManager seasonManager;

    private GlobalNoiseBranch globalNoiseBranch;
    private RegionSampleBranch regionSampleBranch;
    private TemperatureBranch temperatureBranch;

    private Object2ShortOpenHashMap<String> weatherName2WeatherID;
    private Short2ObjectOpenHashMap<WeatherHandle> weatherID2WeatherHandle;

    private String lastSeason;
    private ObjectArrayList<WeatherPoolEntryStruct> activeWeatherPool;

    // Base \\

    @Override
    protected void create() {

        this.weatherName2WeatherID = new Object2ShortOpenHashMap<>();
        this.weatherID2WeatherHandle = new Short2ObjectOpenHashMap<>();

        this.globalNoiseBranch = create(GlobalNoiseBranch.class);
        this.regionSampleBranch = create(RegionSampleBranch.class);
        this.temperatureBranch = create(TemperatureBranch.class);

        create(WeatherLoader.class);
    }

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.windowManager = get(WindowManager.class);
        this.playerManager = get(PlayerManager.class);
        this.seasonManager = get(SeasonManager.class);
    }

    @Override
    protected void update() {

        updateReferenceCoordinate();

        String currentSeason = clockManager.getClockHandle().getCurrentSeason();

        if (currentSeason != null && !currentSeason.equals(lastSeason)) {
            BiomeHandle activeBiome = resolveActiveBiome();
            this.activeWeatherPool = resolveWeatherPool(activeBiome, currentSeason);
            this.lastSeason = currentSeason;
        }

        if (activeWeatherPool == null)
            return;

        regionSampleBranch.sampleRegions(activeWeatherPool);
        temperatureBranch.updateTemperature(regionSampleBranch.getCenterSample());
    }

    // Reference Coordinate \\

    private void updateReferenceCoordinate() {

        WindowInstance mainWindow = windowManager.getMainWindow();

        if (mainWindow == null)
            return;

        int windowID = mainWindow.getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        setReferenceCoordinate(player.getWorldPositionStruct().getChunkCoordinate());
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

        ObjectArrayList<WeatherChanceStruct> entries = biomeHandle.getWeatherEntriesForSeason(season);

        if (entries.isEmpty())
            entries = resolveFallbackEntries(biomeHandle, season);

        float precipitationBias = resolvePrecipitationBias(season);

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

    private ObjectArrayList<WeatherChanceStruct> resolveFallbackEntries(BiomeHandle biomeHandle, String season) {

        if (!biomeHandle.hasAnyWeathers())
            throwException("Biome \"" + biomeHandle.getBiomeName() +
                    "\" has no \"weathers\" block defined at all — cannot resolve any season, including \""
                    + season + "\"");

        String fallbackSeason = biomeHandle.getDefinedSeasonNames().get(0);

        errorLog("[WeatherManager] Biome \"" + biomeHandle.getBiomeName() +
                "\" has no weathers defined for season \"" + season + "\" — falling back to \"" +
                fallbackSeason + "\". Defined seasons: " + biomeHandle.getDefinedSeasonNames());

        return biomeHandle.getWeatherEntriesForSeason(fallbackSeason);
    }

    // Next Weather Bias \\

    private ObjectArrayList<WeatherPoolEntryStruct> buildBiasedPool(WeatherHandle currentWeather) {

        if (!currentWeather.hasNextWeatherSuggestions())
            return activeWeatherPool;

        ObjectArrayList<WeatherPoolEntryStruct> biased = new ObjectArrayList<>(activeWeatherPool.size());

        for (int i = 0; i < activeWeatherPool.size(); i++) {

            WeatherPoolEntryStruct entry = activeWeatherPool.get(i);
            float suggestionChance = currentWeather.getNextWeatherChanceFor(entry.getWeatherHandle());
            float biasedChance = entry.getChance() + suggestionChance * NEXT_WEATHER_SUGGESTION_INFLUENCE;

            biased.add(new WeatherPoolEntryStruct(entry.getWeatherHandle(), biasedChance));
        }

        return biased;
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

    public void setReferenceCoordinate(long chunkCoordinate) {
        regionSampleBranch.setReferenceCoordinate(chunkCoordinate);
    }

    public long getReferenceCoordinate() {
        return regionSampleBranch.getReferenceCoordinate();
    }

    public boolean hasActiveWeatherPool() {
        return activeWeatherPool != null;
    }

    public float getEffectiveNearRangeChunks() {
        return regionSampleBranch.getEffectiveNearRangeChunks();
    }

    /*
     * Chunk-space drift rate along the world's longitudinal axis, driven
     * purely by the planet's own rotation speed. WeatherPatternManager reads
     * this to migrate pattern positions, and RegionSampleBranch's own noise
     * field scrolls at the identical rate — so both are always in lockstep
     * by construction rather than by tuning two constants to match.
     */
    public float getWorldDriftChunksPerSecondX() {
        return globalNoiseBranch.getWorldDriftChunksPerSecondX();
    }

    public float getWindSpeedScale() {

        if (!hasActiveWeatherPool())
            return EngineSetting.DEFAULT_WEATHER_WIND_SPEED_SCALE;

        return regionSampleBranch.getCenterSample().getWindSpeedScale();
    }

    public float getWindTurbulenceScale() {

        if (!hasActiveWeatherPool())
            return EngineSetting.DEFAULT_WEATHER_WIND_TURBULENCE_SCALE;

        return regionSampleBranch.getCenterSample().getWindTurbulenceScale();
    }

    public float getHumidity() {

        if (!hasActiveWeatherPool())
            return EngineSetting.DEFAULT_WEATHER_HUMIDITY;

        return regionSampleBranch.getCenterSample().getHumidity();
    }

    public float getVisibility() {

        if (!hasActiveWeatherPool())
            return EngineSetting.DEFAULT_WEATHER_VISIBILITY;

        return regionSampleBranch.getCenterSample().getVisibility();
    }

    public float getFogDensityScale() {

        if (!hasActiveWeatherPool())
            return EngineSetting.DEFAULT_WEATHER_FOG_DENSITY_SCALE;

        return regionSampleBranch.getCenterSample().getFogDensityScale();
    }

    public float getCurrentTemperature() {
        return temperatureBranch.getCurrentTemperature();
    }

    public float getGlobalStormIntensityAt(long chunkCoordinate) {
        return globalNoiseBranch.sampleGlobalIntensity(chunkCoordinate);
    }

    public void resolveWeatherBand(WeatherBandStruct out, long chunkCoordinate) {

        if (activeWeatherPool == null)
            throwException("Cannot resolve a weather band before any season has been resolved. "
                    + "Callers should check hasActiveWeatherPool() first.");

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkY = Coordinate2Long.unpackY(chunkCoordinate);

        regionSampleBranch.resolveBand(out, chunkX, chunkY, activeWeatherPool);
    }

    public void resolveWeatherBandTowardHorizon(WeatherBandStruct out, long homeChunkCoordinate) {
        resolveWeatherBandTowardHorizonInternal(out, homeChunkCoordinate, null);
    }

    public void resolveWeatherBandTowardHorizonBiased(
            WeatherBandStruct out,
            long homeChunkCoordinate,
            WeatherHandle currentWeather) {
        resolveWeatherBandTowardHorizonInternal(out, homeChunkCoordinate, currentWeather);
    }

    private void resolveWeatherBandTowardHorizonInternal(
            WeatherBandStruct out,
            long homeChunkCoordinate,
            WeatherHandle currentWeather) {

        if (activeWeatherPool == null)
            throwException("Cannot resolve a weather band before any season has been resolved. "
                    + "Callers should check hasActiveWeatherPool() first.");

        int homeChunkX = Coordinate2Long.unpackX(homeChunkCoordinate);
        int homeChunkZ = Coordinate2Long.unpackY(homeChunkCoordinate);

        long referenceCoordinate = getReferenceCoordinate();
        int referenceChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int referenceChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        ObjectArrayList<WeatherPoolEntryStruct> pool = currentWeather == null
                ? activeWeatherPool
                : buildBiasedPool(currentWeather);

        regionSampleBranch.resolveBandTowardHorizon(
                out, homeChunkX, homeChunkZ, referenceChunkX, referenceChunkZ, pool);
    }
}