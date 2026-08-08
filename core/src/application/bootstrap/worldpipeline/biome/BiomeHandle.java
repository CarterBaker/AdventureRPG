package application.bootstrap.worldpipeline.biome;

import engine.graphics.color.Color;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeHandle extends HandlePackage {

    /*
     * Persistent biome record. Wraps BiomeData and delegates all access
     * through it. Registered in BiomeManager from bootstrap to shutdown.
     */

    private static final ObjectArrayList<String> EMPTY_NAMES = new ObjectArrayList<>();
    private static final FloatArrayList EMPTY_CHANCES = new FloatArrayList();

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

    public ObjectArrayList<String> getWeatherNamesForSeason(String seasonName) {
        ObjectArrayList<String> names = biomeData.getWeatherNamesForSeason(seasonName);
        return names != null ? names : EMPTY_NAMES;
    }

    public FloatArrayList getWeatherChancesForSeason(String seasonName) {
        FloatArrayList chances = biomeData.getWeatherChancesForSeason(seasonName);
        return chances != null ? chances : EMPTY_CHANCES;
    }

    public boolean hasWeathersForSeason(String seasonName) {
        return biomeData.getWeatherNamesForSeason(seasonName) != null;
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

    public int getMapColor() {
        return biomeData.getMapColor();
    }

    public boolean hasMapColor() {
        return biomeData.hasMapColor();
    }

    public ObjectArrayList<String> getProbableBiomeNames() {
        return biomeData.getProbableBiomeNames();
    }

    public FloatArrayList getProbableBiomeChances() {
        return biomeData.getProbableBiomeChances();
    }
}