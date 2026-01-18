package com.internal.bootstrap.worldpipeline.biome;

import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.Extras.Color;

public class BiomeHandle extends HandlePackage {

    // Identity
    private String biomeName;
    private int biomeID;
    private Color biomeColor;

    // Constructor \\

    public void constructor(String biomeName, int biomeID) {

        this.biomeName = biomeName;
        this.biomeID = biomeID;
        this.biomeColor = Color.WHITE;
    }

    // Accessible \\

    public String getBiomeName() {
        return biomeName;
    }

    public int getBiomeID() {
        return biomeID;
    }

    public Color getBiomeColor() {
        return biomeColor;
    }
}