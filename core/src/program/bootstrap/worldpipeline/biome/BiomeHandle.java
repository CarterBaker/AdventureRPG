package program.bootstrap.worldpipeline.biome;

import program.core.engine.HandlePackage;
import program.core.util.mathematics.extrasa.Color;

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
}