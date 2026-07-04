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
}