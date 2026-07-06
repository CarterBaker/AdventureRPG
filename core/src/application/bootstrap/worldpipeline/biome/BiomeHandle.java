package application.bootstrap.worldpipeline.biome;

import engine.graphics.color.Color;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeHandle extends HandlePackage {

    /*
     * Persistent biome record. Wraps BiomeData and delegates all access
     * through it. Registered in BiomeManager from bootstrap to shutdown.
     */

    // Internal
    private BiomeData biomeData;

    // Constructor \\

    public void constructor(BiomeData biomeData) {
        this.biomeData = biomeData;
    }

    // Accessible \\

    public BiomeData getBiomeData() {
        return biomeData;
    }

    public String getBiomeName() {
        return biomeData.getBiomeName();
    }

    public short getBiomeID() {
        return biomeData.getBiomeID();
    }

    public Color getBiomeColor() {
        return biomeData.getBiomeColor();
    }

    public ObjectArrayList<WeatherChanceStruct> getWeatherEntriesForSeason(String seasonName) {

        ObjectArrayList<WeatherChanceStruct> entries = biomeData.getSeasonWeatherEntries().get(seasonName);

        if (entries == null)
            return new ObjectArrayList<>();

        return entries;
    }

    public boolean hasWeathersForSeason(String seasonName) {
        return biomeData.getSeasonWeatherEntries().containsKey(seasonName);
    }

    /*
     * Every season name this biome actually defined a "weathers" block for,
     * in JSON declaration order. Used by WeatherManager as a deterministic
     * fallback sequence when the calendar's current season isn't among them.
     */
    public ObjectArrayList<String> getDefinedSeasonNames() {
        return biomeData.getSeasonNames();
    }

    public boolean hasAnyWeathers() {
        return !biomeData.getSeasonNames().isEmpty();
    }
}