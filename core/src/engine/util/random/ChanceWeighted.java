package engine.util.random;

public interface ChanceWeighted {

    /*
     * Implemented by any struct that participates in weighted-chance
     * selection — e.g. a named weather within a biome's seasonal pool, or a
     * cloud archetype referenced by a weather. Chance values are relative
     * weights, not percentages — they only need to be positive and
     * consistent within a single list.
     */

    float getChance();
}