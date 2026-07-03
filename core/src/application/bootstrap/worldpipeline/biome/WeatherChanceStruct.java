package application.bootstrap.worldpipeline.biome;

import engine.root.StructPackage;
import engine.util.random.ChanceWeighted;

public class WeatherChanceStruct extends StructPackage implements ChanceWeighted {

    /*
     * One named weather entry within a biome's seasonal pool, paired with
     * its relative chance weight. A larger chance means this weather owns
     * a proportionally larger band of the noise range RegionSampleBranch
     * samples against — it shows up more often, not exclusively.
     */

    // Internal
    private final String weatherName;
    private final float chance;

    // Constructor \\

    public WeatherChanceStruct(String weatherName, float chance) {

        // Internal
        this.weatherName = weatherName;
        this.chance = chance;
    }

    // Accessible \\

    public String getWeatherName() {
        return weatherName;
    }

    @Override
    public float getChance() {
        return chance;
    }
}