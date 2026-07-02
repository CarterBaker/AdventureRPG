package application.bootstrap.worldpipeline.biome;

import application.bootstrap.calendarpipeline.clock.Season;
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

    public String[] getWeatherNamesForSeason(Season season) {

        ObjectArrayList<String> names = biomeData.getSeasonWeatherNames().get(season);

        if (names == null)
            return new String[0];

        return names.toArray(new String[0]);
    }
}