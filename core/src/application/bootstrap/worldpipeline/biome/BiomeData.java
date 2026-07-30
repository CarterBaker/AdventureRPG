package application.bootstrap.worldpipeline.biome;

import engine.graphics.color.Color;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeData extends DataPackage {

    /*
     * Persistent biome record. Holds identity, biome color, and the named-
     * season weather pools WeatherManager resolves into live handles on
     * demand. Each season's pool is two parallel fastutil lists — names
     * and their relative chance weights — rather than a wrapper type per
     * entry. seasonNames preserves JSON declaration order for the same
     * keys — WeatherManager falls back through this order when the
     * calendar's actual current season isn't one this biome defined.
     */

    private final String biomeName;
    private final short biomeID;

    private final Color biomeColor;

    private final Object2ObjectOpenHashMap<String, ObjectArrayList<String>> seasonWeatherNames;
    private final Object2ObjectOpenHashMap<String, FloatArrayList> seasonWeatherChances;
    private final ObjectArrayList<String> seasonNames;

    public BiomeData(
            String biomeName,
            short biomeID,
            Color biomeColor,
            Object2ObjectOpenHashMap<String, ObjectArrayList<String>> seasonWeatherNames,
            Object2ObjectOpenHashMap<String, FloatArrayList> seasonWeatherChances,
            ObjectArrayList<String> seasonNames) {

        this.biomeName = biomeName;
        this.biomeID = biomeID;

        this.biomeColor = biomeColor;

        this.seasonWeatherNames = seasonWeatherNames;
        this.seasonWeatherChances = seasonWeatherChances;
        this.seasonNames = seasonNames;
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
}