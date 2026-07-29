package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;
import engine.util.random.ChanceWeighted;

class WeatherPoolEntryStruct extends StructPackage implements ChanceWeighted {

    /*
     * Resolved pairing of a live WeatherHandle and its relative chance
     * weight within a biome/season pool. Consumed by RegionSampleSystem
     * for chance-weighted, noise-blended sampling. Season pool entries are
     * built once per season change and never mutated again; WeatherManager
     * also keeps a small pool of these it reuses and mutates in place via
     * set() when resolving a "next weather" biased pool, so biasing never
     * allocates a fresh entry per pattern reevaluation.
     */

    // Internal
    private WeatherHandle weatherHandle;
    private float chance;

    // Constructor \\

    WeatherPoolEntryStruct(WeatherHandle weatherHandle, float chance) {

        // Internal
        this.weatherHandle = weatherHandle;
        this.chance = chance;
    }

    // Mutation \\

    void set(WeatherHandle weatherHandle, float chance) {
        this.weatherHandle = weatherHandle;
        this.chance = chance;
    }

    // Accessible \\

    WeatherHandle getWeatherHandle() {
        return weatherHandle;
    }

    @Override
    public float getChance() {
        return chance;
    }
}