package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class WeatherManager extends ManagerPackage {

    /*
     * Owns the weather definition palette and answers what the weather is at
     * any point of the world. WeatherFlowSystem scrolls the static weather
     * image across the world; RegionSampleSystem reads a pixel of it as a
     * percentile and walks the governing biome's pool for the active season,
     * which is built once per season, ordered calmest to most severe, and
     * cached by biome ID. The answer is a pure function of position and
     * shared world time, so every player sees the same weather.
     */

    // Internal
    private ClockManager clockManager;
    private BiomeManager biomeManager;
    private SeasonManager seasonManager;
    private WorldManager worldManager;
    private WeatherFlowSystem weatherFlowSystem;
    private RegionSampleSystem regionSampleSystem;

    // Palette
    private Object2ShortOpenHashMap<String> weatherName2WeatherID;
    private Short2ObjectOpenHashMap<WeatherHandle> weatherID2WeatherHandle;

    // Season
    private String activeSeason;

    // Biome Pools
    private Short2ObjectOpenHashMap<ObjectArrayList<WeatherHandle>> biomeID2WeatherHandles;
    private Short2ObjectOpenHashMap<FloatArrayList> biomeID2WeatherChances;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.weatherName2WeatherID = new Object2ShortOpenHashMap<>();
        this.weatherName2WeatherID.defaultReturnValue((short) -1);
        this.weatherID2WeatherHandle = new Short2ObjectOpenHashMap<>();

        // Biome Pools
        this.biomeID2WeatherHandles = new Short2ObjectOpenHashMap<>();
        this.biomeID2WeatherChances = new Short2ObjectOpenHashMap<>();

        this.weatherFlowSystem = create(WeatherFlowSystem.class);
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

        if (currentSeason == null || currentSeason.equals(activeSeason))
            return;

        biomeID2WeatherHandles.clear();
        biomeID2WeatherChances.clear();
        this.activeSeason = currentSeason;
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

    // Biome Pool \\

    private BiomeHandle resolveBiome(long worldChunkCoordinate) {
        return biomeManager.getBiome(
                worldManager.getActiveWorld(),
                WorldWrapUtility.wrapAroundWorld(worldManager.getActiveWorld(), worldChunkCoordinate));
    }

    private void buildBiomePool(BiomeHandle biomeHandle) {

        String resolvedSeason = activeSeason;
        ObjectArrayList<String> names = biomeHandle.getWeatherNamesForSeason(activeSeason);

        if (names.isEmpty()) {
            resolvedSeason = resolveFallbackSeasonName(biomeHandle, activeSeason);
            names = biomeHandle.getWeatherNamesForSeason(resolvedSeason);
        }

        FloatArrayList chances = biomeHandle.getWeatherChancesForSeason(resolvedSeason);
        float precipitationBias = seasonManager.getSeasonHandleFromSeasonName(resolvedSeason)
                .getPrecipitationChanceScale();
        int size = names.size();

        ObjectArrayList<WeatherHandle> handles = new ObjectArrayList<>(size);
        FloatArrayList weightedChances = new FloatArrayList(size);

        for (int i = 0; i < size; i++) {

            WeatherHandle handle = getWeatherHandleFromWeatherName(names.get(i));
            float chance = chances.getFloat(i);

            if (handle.getPrecipitationIntensity() > 0f)
                chance *= precipitationBias;

            insertBySeverity(handles, weightedChances, handle, chance);
        }

        biomeID2WeatherHandles.put(biomeHandle.getBiomeID(), handles);
        biomeID2WeatherChances.put(biomeHandle.getBiomeID(), weightedChances);
    }

    private void insertBySeverity(
            ObjectArrayList<WeatherHandle> handles,
            FloatArrayList chances,
            WeatherHandle handle,
            float chance) {

        int index = handles.size();

        while (index > 0 && handles.get(index - 1).getSeverity() > handle.getSeverity())
            index--;

        handles.add(index, handle);
        chances.add(index, chance);
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

    // Accessible \\

    public boolean hasWeather(String weatherName) {
        return weatherName2WeatherID.containsKey(weatherName);
    }

    public short getWeatherIDFromWeatherName(String weatherName) {

        if (!weatherName2WeatherID.containsKey(weatherName))
            request(weatherName);

        if (!weatherName2WeatherID.containsKey(weatherName))
            throwException("Weather \"" + weatherName + "\" was not registered after its on-demand load completed — "
                    + "the loaded file must declare a different weather name than the one requested. "
                    + "Check for a resource-name/path mismatch between the weather directory and its declared name.");

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

    public double getFlowOffsetXBlocks() {
        return weatherFlowSystem.getOffsetXBlocks();
    }

    public double getFlowOffsetZBlocks() {
        return weatherFlowSystem.getOffsetZBlocks();
    }

    public float sampleNoisePercentile(double noiseChunkX, double noiseChunkZ) {
        return regionSampleSystem.samplePercentile(noiseChunkX, noiseChunkZ);
    }

    /*
     * Resolves the single weather a pixel of the weather image produces at a
     * world chunk: that chunk's biome pool for the active season, read at
     * the pixel's percentile.
     */
    public WeatherHandle resolveWeather(long worldChunkCoordinate, float noisePercentile) {

        if (!hasActiveWeatherPool())
            throwException("Cannot resolve weather before any season has been resolved. "
                    + "Callers should check hasActiveWeatherPool() first.");

        BiomeHandle biomeHandle = resolveBiome(worldChunkCoordinate);
        short biomeID = biomeHandle.getBiomeID();

        if (!biomeID2WeatherHandles.containsKey(biomeID))
            buildBiomePool(biomeHandle);

        return regionSampleSystem.pickFromPool(
                biomeID2WeatherHandles.get(biomeID),
                biomeID2WeatherChances.get(biomeID),
                noisePercentile);
    }
}
