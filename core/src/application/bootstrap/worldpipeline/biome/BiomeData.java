package application.bootstrap.worldpipeline.biome;

import application.bootstrap.calendarpipeline.clock.Season;
import engine.graphics.color.Color;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeData extends DataPackage {

    /*
     * Persistent biome record. Holds identity, biome color, and the seasonal
     * weather name pools WeatherManager resolves into live handles on demand.
     * Color is immutable — set at bootstrap from JSON definition.
     * Owned by BiomeHandle for the full engine session.
     */

    // Identity
    private final String biomeName;
    private final short biomeID;

    // Color
    private final Color biomeColor;

    // Weather
    private final Object2ObjectOpenHashMap<Season, ObjectArrayList<String>> seasonWeatherNames;

    // Constructor \\

    public BiomeData(
            String biomeName,
            short biomeID,
            Color biomeColor,
            Object2ObjectOpenHashMap<Season, ObjectArrayList<String>> seasonWeatherNames) {

        // Identity
        this.biomeName = biomeName;
        this.biomeID = biomeID;

        // Color
        this.biomeColor = biomeColor;

        // Weather
        this.seasonWeatherNames = seasonWeatherNames;
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

    public Object2ObjectOpenHashMap<Season, ObjectArrayList<String>> getSeasonWeatherNames() {
        return seasonWeatherNames;
    }
}