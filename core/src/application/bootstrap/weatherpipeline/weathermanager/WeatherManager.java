package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class WeatherManager extends ManagerPackage {

    /*
     * Owns the weather definition palette and resolves whichever biome
     * governs a given world location, through BiomeManager, into that
     * biome's own chance-weighted pool of candidate weathers for the
     * active season, then hands that pool to RegionSampleSystem to pick
     * exactly one weather — never a blend of two. Each biome's pool is
     * built once per season and cached by biome ID, so repeated queries
     * against the same biome never re-walk its weather list or reallocate.
     */

    private ClockManager clockManager;
    private BiomeManager biomeManager;
    private SeasonManager seasonManager;
    private WorldManager worldManager;

    private GlobalNoiseSystem globalNoiseSystem;
    private RegionSampleSystem regionSampleSystem;

    private Object2ShortOpenHashMap<String> weatherName2WeatherID;
    private Short2ObjectOpenHashMap<WeatherHandle> weatherID2WeatherHandle;

    private String activeSeason;

    // Per-Biome Pool Cache — rebuilt whenever the active season changes
    private final Short2ObjectOpenHashMap<ObjectArrayList<WeatherHandle>> biomeWeatherHandles = new Short2ObjectOpenHashMap<>();
    private final Short2ObjectOpenHashMap<FloatArrayList> biomeWeatherChances = new Short2ObjectOpenHashMap<>();

    // Biased Pool — recycled scratch for "next weather" biasing
    private final ObjectArrayList<WeatherHandle> biasedWeatherHandles = new ObjectArrayList<>();
    private final FloatArrayList biasedWeatherChances = new FloatArrayList();

    // Resolved Pool — points at either a cached biome pool or the biased
    // scratch pool above, reassigned fresh ahead of every weather resolution
    private ObjectArrayList<WeatherHandle> resolvedPoolHandles;
    private FloatArrayList resolvedPoolChances;

    @Override
    protected void create() {

        this.weatherName2WeatherID = new Object2ShortOpenHashMap<>();
        this.weatherID2WeatherHandle = new Short2ObjectOpenHashMap<>();

        this.globalNoiseSystem = create(GlobalNoiseSystem.class);
        this.regionSampleSystem = create(RegionSampleSystem.class);

        create(WeatherLoader.class);
    }

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.seasonManager = get(SeasonManager.class);
        this.worldManager = get(WorldManager.class);
    }

    @Override
    protected void update() {

        String currentSeason = clockManager.getClockHandle().getCurrentSeason();

        if (currentSeason != null && !currentSeason.equals(activeSeason)) {
            biomeWeatherHandles.clear();
            biomeWeatherChances.clear();
            this.activeSeason = currentSeason;
        }
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

    // Biome-Resolved Pool \\

    private void resolvePoolForChunkCoordinate(long chunkCoordinate) {

        BiomeHandle biomeHandle = biomeManager.getBiomeHandleFromChunkCoordinate(
                worldManager.getActiveWorld(), chunkCoordinate);

        short biomeID = biomeHandle.getBiomeID();

        if (biomeWeatherHandles.containsKey(biomeID)) {
            resolvedPoolHandles = biomeWeatherHandles.get(biomeID);
            resolvedPoolChances = biomeWeatherChances.get(biomeID);
            return;
        }

        buildBiomePool(biomeHandle);
    }

    private void buildBiomePool(BiomeHandle biomeHandle) {

        String resolvedSeason = activeSeason;
        ObjectArrayList<String> names = biomeHandle.getWeatherNamesForSeason(activeSeason);

        if (names.isEmpty()) {
            resolvedSeason = resolveFallbackSeasonName(biomeHandle, activeSeason);
            names = biomeHandle.getWeatherNamesForSeason(resolvedSeason);
        }

        FloatArrayList chances = biomeHandle.getWeatherChancesForSeason(resolvedSeason);
        float precipitationBias = resolvePrecipitationBias(resolvedSeason);
        int size = names.size();

        ObjectArrayList<WeatherHandle> handles = new ObjectArrayList<>(size);
        FloatArrayList weightedChances = new FloatArrayList(size);

        for (int i = 0; i < size; i++) {

            WeatherHandle handle = getWeatherHandleFromWeatherName(names.get(i));
            float chance = chances.getFloat(i);

            if (handle.getPrecipitationIntensity() > 0f)
                chance *= precipitationBias;

            handles.add(handle);
            weightedChances.add(chance);
        }

        biomeWeatherHandles.put(biomeHandle.getBiomeID(), handles);
        biomeWeatherChances.put(biomeHandle.getBiomeID(), weightedChances);

        resolvedPoolHandles = handles;
        resolvedPoolChances = weightedChances;
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

    private void applyNextWeatherBias(WeatherHandle currentWeather) {

        if (currentWeather == null || !currentWeather.hasNextWeatherSuggestions())
            return;

        int size = resolvedPoolHandles.size();

        biasedWeatherHandles.clear();
        biasedWeatherChances.clear();

        for (int i = 0; i < size; i++) {

            WeatherHandle handle = resolvedPoolHandles.get(i);
            float suggestionChance = currentWeather.getNextWeatherChanceFor(handle);
            float biasedChance = resolvedPoolChances.getFloat(i)
                    + suggestionChance * EngineSetting.WEATHER_NEXT_SUGGESTION_INFLUENCE;

            biasedWeatherHandles.add(handle);
            biasedWeatherChances.add(biasedChance);
        }

        resolvedPoolHandles = biasedWeatherHandles;
        resolvedPoolChances = biasedWeatherChances;
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
        return activeSeason != null;
    }

    public float getEffectiveRangeChunks() {
        return regionSampleSystem.getEffectiveRangeChunks();
    }

    public float getWorldDriftChunksPerSecondX() {
        return globalNoiseSystem.getWorldDriftChunksPerSecondX();
    }

    public float getGlobalStormIntensityAt(long chunkCoordinate) {
        return globalNoiseSystem.sampleGlobalIntensity(chunkCoordinate);
    }

    /*
     * Resolves the single active weather at an exact chunk coordinate — no
     * horizon blending, no next-weather bias, just that coordinate's own
     * biome pool read straight against the noise field.
     */
    public WeatherHandle resolveWeather(long chunkCoordinate) {

        if (!hasActiveWeatherPool())
            throwException("Cannot resolve weather before any season has been resolved. "
                    + "Callers should check hasActiveWeatherPool() first.");

        resolvePoolForChunkCoordinate(chunkCoordinate);

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        return regionSampleSystem.resolveWeather(chunkX, chunkZ, resolvedPoolHandles, resolvedPoolChances);
    }

    public WeatherHandle resolveWeatherTowardHorizon(long homeChunkCoordinate, long referenceChunkCoordinate) {
        return resolveWeatherTowardHorizonInternal(homeChunkCoordinate, referenceChunkCoordinate, null);
    }

    public WeatherHandle resolveWeatherTowardHorizonBiased(
            long homeChunkCoordinate,
            long referenceChunkCoordinate,
            WeatherHandle currentWeather) {
        return resolveWeatherTowardHorizonInternal(homeChunkCoordinate, referenceChunkCoordinate, currentWeather);
    }

    private WeatherHandle resolveWeatherTowardHorizonInternal(
            long homeChunkCoordinate,
            long referenceChunkCoordinate,
            WeatherHandle currentWeather) {

        if (!hasActiveWeatherPool())
            throwException("Cannot resolve weather before any season has been resolved. "
                    + "Callers should check hasActiveWeatherPool() first.");

        resolvePoolForChunkCoordinate(homeChunkCoordinate);
        applyNextWeatherBias(currentWeather);

        int homeChunkX = Coordinate2Long.unpackX(homeChunkCoordinate);
        int homeChunkZ = Coordinate2Long.unpackY(homeChunkCoordinate);

        int referenceChunkX = Coordinate2Long.unpackX(referenceChunkCoordinate);
        int referenceChunkZ = Coordinate2Long.unpackY(referenceChunkCoordinate);

        return regionSampleSystem.resolveWeatherTowardHorizon(
                homeChunkX, homeChunkZ, referenceChunkX, referenceChunkZ,
                resolvedPoolHandles, resolvedPoolChances);
    }
}