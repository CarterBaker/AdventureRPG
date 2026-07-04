package application.bootstrap.worldpipeline.biome;

import engine.graphics.color.Color;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeData extends DataPackage {

    /*
     * Persistent biome record. Holds identity, biome color, and the named-
     * season weather chance pools WeatherManager resolves into live handles
     * on demand. Seasons are keyed by name rather than a fixed enum — the
     * active calendar decides what those names are, so a biome's "weathers"
     * block can use whatever season names its world's calendar defines.
     * Each pool entry pairs a weather name with its relative chance weight
     * — see WeatherChanceStruct. Color is immutable — set at bootstrap from
     * JSON definition. Owned by BiomeHandle for the full engine session.
     */

    // Identity
    private final String biomeName;
    private final short biomeID;

    // Color
    private final Color biomeColor;

    // Weather
    private final Object2ObjectOpenHashMap<String, ObjectArrayList<WeatherChanceStruct>> seasonWeatherEntries;

    // Constructor \\

    public BiomeData(
            String biomeName,
            short biomeID,
            Color biomeColor,
            Object2ObjectOpenHashMap<String, ObjectArrayList<WeatherChanceStruct>> seasonWeatherEntries) {

        // Identity
        this.biomeName = biomeName;
        this.biomeID = biomeID;

        // Color
        this.biomeColor = biomeColor;

        // Weather
        this.seasonWeatherEntries = seasonWeatherEntries;
    }

    // Accessible \\

    public String getBiomeName() {
        return biomeName;
    }

    public short getBiomeID() {
        return biomeID;
    }

    public Color getBiomeColor() {
        return biomeColor;
    }

    public Object2ObjectOpenHashMap<String, ObjectArrayList<WeatherChanceStruct>> getSeasonWeatherEntries() {
        return seasonWeatherEntries;
    }
}