package com.AdventureRPG.bootstrap.worldpipeline.biome;

import com.AdventureRPG.core.engine.HandlePackage;

public class BiomeHandle extends HandlePackage {

    // Identity
    private String biomeName;
    private int biomeID;

    // Constructor \\

    public void constructor(String biomeName, int biomeID) {

        this.biomeName = biomeName;
        this.biomeID = biomeID;
    }

    // Accessible \\

    public String getBiomeName() {
        return biomeName;
    }

    public int getBiomeID() {
        return biomeID;
    }
}