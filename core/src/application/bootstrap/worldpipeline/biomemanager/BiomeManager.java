package application.bootstrap.worldpipeline.biomemanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import engine.root.ManagerPackage;
import engine.util.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class BiomeManager extends ManagerPackage {

    // Palette
    private Object2ShortOpenHashMap<String> biomeName2BiomeID;
    private Short2ObjectOpenHashMap<BiomeHandle> biomeID2BiomeHandle;

    // Base \\

    @Override
    protected void create() {

        this.biomeName2BiomeID = new Object2ShortOpenHashMap<>();
        this.biomeID2BiomeHandle = new Short2ObjectOpenHashMap<>();

        create(InternalLoader.class);
    }

    // Management \\

    void addBiome(BiomeHandle biomeHandle) {

        if (biomeID2BiomeHandle.containsKey(biomeHandle.getBiomeID())) {
            BiomeHandle existing = biomeID2BiomeHandle.get(biomeHandle.getBiomeID());
            if (RegistryUtility.isCollision(biomeHandle.getBiomeName(), existing.getBiomeName(),
                    biomeHandle.getBiomeID()))
                throwException("Biome ID collision: '"
                        + biomeHandle.getBiomeName() + "' collides with '"
                        + existing.getBiomeName() + "' (ID " + biomeHandle.getBiomeID()
                        + ") — rename one biome to resolve");
        }

        biomeName2BiomeID.put(biomeHandle.getBiomeName(), biomeHandle.getBiomeID());
        biomeID2BiomeHandle.put(biomeHandle.getBiomeID(), biomeHandle);
    }

    // On-Demand \\

    public void request(String biomeName) {
        ((InternalLoader) internalLoader).request(biomeName);
    }

    // Accessible \\

    public boolean hasBiome(String biomeName) {
        return biomeName2BiomeID.containsKey(biomeName);
    }

    public short getBiomeIDFromBiomeName(String biomeName) {

        if (!biomeName2BiomeID.containsKey(biomeName))
            request(biomeName);

        return biomeName2BiomeID.getShort(biomeName);
    }

    public BiomeHandle getBiomeHandleFromBiomeID(short biomeID) {

        BiomeHandle handle = biomeID2BiomeHandle.get(biomeID);

        if (handle == null)
            throwException("No handle registered for biome ID: " + biomeID);

        return handle;
    }

    public BiomeHandle getBiomeHandleFromBiomeName(String biomeName) {
        return getBiomeHandleFromBiomeID(getBiomeIDFromBiomeName(biomeName));
    }
}