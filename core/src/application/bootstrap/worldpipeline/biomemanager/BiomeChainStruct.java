package application.bootstrap.worldpipeline.biomemanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.ProbablePatchStruct;
import engine.root.StructPackage;

class BiomeChainStruct extends StructPackage {

    /*
     * One step down a probable biome chain at one position: the patch the
     * biome at this depth fills, the absolute biome whose ground it stands on,
     * and the lake it belongs to when it or a biome above it holds still
     * water relative to its host. A lake is fixed by the outermost still-water
     * patch of the chain, so every still-water biome nested inside it joins
     * the same body at the same level. BiomeManager keeps one per depth in
     * its field scratch and reuses them, so walking a chain never allocates.
     */

    // Patch
    final ProbablePatchStruct patch = new ProbablePatchStruct();

    // Ground
    BiomeHandle baseBiome;

    // Lake
    boolean lake;
    long lakeHash;
    double lakeCenterX;
    double lakeCenterZ;
    int lakeOffsetBlocks;

    // Management \\

    // A biome painted on the world map fills no patch and stands on its own ground
    void resetRoot(BiomeHandle mapBiome) {
        patch.reset();
        baseBiome = mapBiome;
        lake = false;
    }

    // The biome takes its parent's ground when it is relative and its parent's lake when the parent is in one,
    // and otherwise opens a lake of its own when it holds still water relative to its host
    void linkBeneath(BiomeChainStruct parent, BiomeHandle biome) {

        baseBiome = biome.isRelative() ? parent.baseBiome : biome;

        if (parent.lake) {
            lake = true;
            lakeHash = parent.lakeHash;
            lakeCenterX = parent.lakeCenterX;
            lakeCenterZ = parent.lakeCenterZ;
            lakeOffsetBlocks = parent.lakeOffsetBlocks;
            return;
        }

        lake = biome.hasRelativeStillWater();

        if (!lake)
            return;

        lakeHash = patch.getPatchHash();
        lakeCenterX = patch.getCenterX();
        lakeCenterZ = patch.getCenterZ();
        lakeOffsetBlocks = biome.getWaterLevelOffsetBlocks();
    }
}
