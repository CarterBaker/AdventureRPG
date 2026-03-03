package com.internal.bootstrap.worldpipeline.biomemanager;

import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;

public class BiomeManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2ShortOpenHashMap<String> biomeName2BiomeID;
    private Short2ObjectOpenHashMap<BiomeHandle> biomeID2Biome;

    // Base \\

    @Override
    protected void create() {
        this.internalLoadManager = create(InternalLoadManager.class);
        this.biomeName2BiomeID = new Object2ShortOpenHashMap<>();
        this.biomeID2Biome = new Short2ObjectOpenHashMap<>();
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

        // Collision guard — same as BlockManager
        if (biomeID2Biome.containsKey(biome.getBiomeID())) {
            BiomeHandle existing = biomeID2Biome.get(biome.getBiomeID());
            if (RegistryUtility.isCollision(biome.getBiomeName(), existing.getBiomeName(), biome.getBiomeID()))
                throwException("Biome ID collision: '"
                        + biome.getBiomeName() + "' collides with '"
                        + existing.getBiomeName() + "' (ID " + biome.getBiomeID() + ") — rename one biome to resolve");
        }

        biomeName2BiomeID.put(biome.getBiomeName(), biome.getBiomeID());
        biomeID2Biome.put(biome.getBiomeID(), biome);
    }

    // Accessible \\

    public short getBiomeIDFromBiomeName(String biomeName) {
        if (!biomeName2BiomeID.containsKey(biomeName))
            throwException("Biome not found: " + biomeName);
        return biomeName2BiomeID.getShort(biomeName);
    }

    public BiomeHandle getBiomeFromBiomeID(short biomeID) {
        BiomeHandle biome = biomeID2Biome.get(biomeID);
        if (biome == null)
            throwException("Biome ID not found: " + biomeID);
        return biome;
    }
}