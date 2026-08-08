package application.bootstrap.worldpipeline.biome;

import engine.graphics.color.Color;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeData extends DataPackage {

    /*
     * Persistent biome record. Holds identity, biome color, the named-season
     * weather pools WeatherManager resolves into live handles on demand, the
     * map color this biome matches against the world PNG, and the probable
     * biome variants that may replace this biome during generation for
     * hand-authored variety. mapColor is optional — MAP_COLOR_UNDEFINED means
     * this biome is never chosen directly from the PNG and can only appear as
     * another biome's variant. probableBiomeNames/Chances are raw, unresolved
     * references — BiomeManager resolves them to live handles on demand.
     * seasonNames preserves JSON declaration order for the same keys —
     * WeatherManager falls back through this order when the calendar's
     * actual current season isn't one this biome defined.
     */

    public static final int MAP_COLOR_UNDEFINED = -1;

    private final String biomeName;
    private final short biomeID;

    private final Color biomeColor;

    private final Object2ObjectOpenHashMap<String, ObjectArrayList<String>> seasonWeatherNames;
    private final Object2ObjectOpenHashMap<String, FloatArrayList> seasonWeatherChances;
    private final ObjectArrayList<String> seasonNames;

    private final int mapColor;
    private final ObjectArrayList<String> probableBiomeNames;
    private final FloatArrayList probableBiomeChances;

    public BiomeData(
            String biomeName,
            short biomeID,
            Color biomeColor,
            Object2ObjectOpenHashMap<String, ObjectArrayList<String>> seasonWeatherNames,
            Object2ObjectOpenHashMap<String, FloatArrayList> seasonWeatherChances,
            ObjectArrayList<String> seasonNames,
            int mapColor,
            ObjectArrayList<String> probableBiomeNames,
            FloatArrayList probableBiomeChances) {

        this.biomeName = biomeName;
        this.biomeID = biomeID;

        this.biomeColor = biomeColor;

        this.seasonWeatherNames = seasonWeatherNames;
        this.seasonWeatherChances = seasonWeatherChances;
        this.seasonNames = seasonNames;

        this.mapColor = mapColor;
        this.probableBiomeNames = probableBiomeNames;
        this.probableBiomeChances = probableBiomeChances;
    }

    public String getBiomeName() {
        return biomeName;
    }

    public short getBiomeID() {
        return biomeID;
    }

    public Color getBiomeColor() {
        return biomeColor;
    }

    public ObjectArrayList<String> getWeatherNamesForSeason(String seasonName) {
        return seasonWeatherNames.get(seasonName);
    }

    public FloatArrayList getWeatherChancesForSeason(String seasonName) {
        return seasonWeatherChances.get(seasonName);
    }

    public ObjectArrayList<String> getSeasonNames() {
        return seasonNames;
    }

    public int getMapColor() {
        return mapColor;
    }

    public boolean hasMapColor() {
        return mapColor != MAP_COLOR_UNDEFINED;
    }

    public ObjectArrayList<String> getProbableBiomeNames() {
        return probableBiomeNames;
    }

    public FloatArrayList getProbableBiomeChances() {
        return probableBiomeChances;
    }
}