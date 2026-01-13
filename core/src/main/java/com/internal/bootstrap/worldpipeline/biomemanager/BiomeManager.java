package com.internal.bootstrap.worldpipeline.biomemanager;

import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class BiomeManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> biomeName2BiomeID;
    private Int2ObjectOpenHashMap<BiomeHandle> biomeID2Biome;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = create(InternalLoadManager.class);

        // Retrieval Mapping
        this.biomeName2BiomeID = new Object2IntOpenHashMap<>();
        this.biomeID2Biome = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void awake() {
        compileBiomes();
    }

    @Override
    protected void release() {
        internalLoadManager = release(InternalLoadManager.class);
    }

    // Biome Management \\

    private void compileBiomes() {
        internalLoadManager.loadBiomes();
    }

    void addBiome(BiomeHandle biome) {
        biomeName2BiomeID.put(biome.getBiomeName(), biome.getBiomeID());
        biomeID2Biome.put(biome.getBiomeID(), biome);
    }

    // Accessible \\

    public int getBiomeIDFromBiomeName(String biomeName) {

        if (!biomeName2BiomeID.containsKey(biomeName))
            throwException("Biome not found: " + biomeName);

        return biomeName2BiomeID.getInt(biomeName);
    }

    public BiomeHandle getBiomeFromBiomeID(int biomeID) {

        BiomeHandle biome = biomeID2Biome.get(biomeID);

        if (biome == null)
            throwException("Biome ID not found: " + biomeID);

        return biome;
    }
}