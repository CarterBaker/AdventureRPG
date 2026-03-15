package com.internal.bootstrap.worldpipeline.biome;

import com.internal.core.engine.DataPackage;
import com.internal.core.util.mathematics.extras.Color;

public class BiomeData extends DataPackage {

    /*
     * Persistent biome record. Holds identity and biome color.
     * Color is immutable — set at bootstrap from JSON definition.
     * Owned by BiomeHandle for the full engine session.
     */

    // Identity
    private final String biomeName;
    private final short biomeID;

    // Color
    private final Color biomeColor;

    // Constructor \\

    public BiomeData(String biomeName, short biomeID, Color biomeColor) {
        this.biomeName = biomeName;
        this.biomeID = biomeID;
        this.biomeColor = biomeColor;
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
}