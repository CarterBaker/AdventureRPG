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
     * Owns the weather palette and drives the live weather simulation.
     * Weather is the fusion of the active biome, the calendar's current
     * named season, and the world's wrapped/rotating noise field
     * (GlobalNoiseBranch blended with RegionSampleBranch's wind-drifted
     * local noise). The sky dome's pattern arcs and the overhead volumetric
     * cloud objects both resolve their own weather identity directly
     * through resolveWeatherBandTowardHorizon() rather than reading a
     * shared directional sample, so the two visual layers can never
     * disagree about what's happening at a given bearing.
     *
     * activeWeatherPool stays null until the calendar resolves its first
     * named season. hasActiveWeatherPool() must be checked by any
     * cross-package caller of
     * resolveWeatherBand()/resolveWeatherBandTowardHorizon()
     * before calling either — both throw otherwise. getWindSpeedScale(),
     * getWindTurbulenceScale(), getHumidity(), getVisibility(), and
     * getFogDensityScale() are the exceptions — each falls back to a
     * neutral default before that point instead of throwing.
     */

    // Internal
    private ClockManager clockManager;
    private BiomeManager biomeManager;
    private WindowManager windowManager;
    private PlayerManager playerManager;
    private SeasonManager seasonManager;

    // Branches
    private GlobalNoiseBranch globalNoiseBranch;
    private RegionSampleBranch regionSampleBranch;
    private TemperatureBranch temperatureBranch;

    // Palette
    private Object2ShortOpenHashMap<String> weatherName2WeatherID;
    private Short2ObjectOpenHashMap<WeatherHandle> weatherID2WeatherHandle;

    // Season Tracking
    private String lastSeason;
    private ObjectArrayList<WeatherPoolEntryStruct> activeWeatherPool;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.weatherName2WeatherID = new Object2ShortOpenHashMap<>();
        this.weatherID2WeatherHandle = new Short2ObjectOpenHashMap<>();

        // Branches
        this.globalNoiseBranch = create(GlobalNoiseBranch.class);
        this.regionSampleBranch = create(RegionSampleBranch.class);
        this.temperatureBranch = create(TemperatureBranch.class);

        create(WeatherLoader.class);
    }

    @Override
    protected void get() {

        // Internal
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

    /*
     * Mirrors the main window's player onto the exact chunk-granular
     * reference GridInstance already treats as authoritative for terrain
     * recentering, so weather/cloud systems and terrain can never disagree
     * about which chunk is "here". No-op before a player has spawned.
     */
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

    /*
     * Single hardcoded biome for now — the one integration point a future
     * per-location biome lookup (sampling a biome map at the reference
     * coordinate) needs to replace.
     */
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

    /*
     * Falls back to the first season the biome actually defined weathers
     * for when the calendar's exact current season name isn't one of them
     * — a mismatched/renamed season, not necessarily a malformed biome.
     */
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

    /*
     * Blended fogDensityScale at the reference coordinate — not yet wired
     * into fog rendering (AtmosphericFog.glsl's curve is currently distance
     * -only), exposed so a future weather-aware fog pass has this ready.
     */
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

    /*
     * Resolves the true, un-blended weather at an arbitrary world-space
     * chunk coordinate. Throws before the first season ever resolves —
     * callers should check hasActiveWeatherPool() first.
     */
    public void resolveWeatherBand(WeatherBandStruct out, long chunkCoordinate) {

        if (activeWeatherPool == null)
            throwException("Cannot resolve a weather band before any season has been resolved. "
                    + "Callers should check hasActiveWeatherPool() first.");

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkY = Coordinate2Long.unpackY(chunkCoordinate);

        regionSampleBranch.resolveBand(out, chunkX, chunkY, activeWeatherPool);
    }

    /*
     * Resolves the weather identity for a home coordinate anywhere between
     * the player and the streaming edge, blended toward the sky dome's own
     * far-range sample along the identical bearing — see
     * RegionSampleBranch.resolveBandTowardHorizon(). Every weather pattern
     * (sky arc and overhead volume alike) resolves through this single
     * entry point, which is what keeps the two visual layers in sync.
     */
    public void resolveWeatherBandTowardHorizon(WeatherBandStruct out, long homeChunkCoordinate) {

        if (activeWeatherPool == null)
            throwException("Cannot resolve a weather band before any season has been resolved. "
                    + "Callers should check hasActiveWeatherPool() first.");

        int homeChunkX = Coordinate2Long.unpackX(homeChunkCoordinate);
        int homeChunkZ = Coordinate2Long.unpackY(homeChunkCoordinate);

        long referenceCoordinate = getReferenceCoordinate();
        int referenceChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int referenceChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        regionSampleBranch.resolveBandTowardHorizon(
                out, homeChunkX, homeChunkZ, referenceChunkX, referenceChunkZ, activeWeatherPool);
    }
}