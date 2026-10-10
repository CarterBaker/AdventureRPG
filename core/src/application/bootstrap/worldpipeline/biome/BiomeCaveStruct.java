package application.bootstrap.worldpipeline.biome;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class BiomeCaveStruct extends StructPackage {

    /*
     * The caves a biome hollows beneath itself, each from 0 for none to 1:
     * how wide its long winding tunnels run, how often deep caverns swell,
     * how many thin passages wander between them and how often a still lake
     * fills a basin chamber. Then the lowest block they reach, how deep below
     * the ground they may run, and whether they may break through the
     * surface where an entrance opens. DEFAULT is what a biome without its
     * own caves carries.
     */

    public static final BiomeCaveStruct DEFAULT = new BiomeCaveStruct(
            EngineSetting.DEFAULT_BIOME_CAVE_TUNNELS,
            EngineSetting.DEFAULT_BIOME_CAVE_CAVERNS,
            EngineSetting.DEFAULT_BIOME_CAVE_NOODLES,
            EngineSetting.DEFAULT_BIOME_CAVE_LAKES,
            EngineSetting.DEFAULT_BIOME_CAVE_MIN_HEIGHT_BLOCKS,
            EngineSetting.DEFAULT_BIOME_CAVE_MAX_DEPTH_BLOCKS,
            EngineSetting.DEFAULT_BIOME_CAVE_ENTRANCES);

    private final float tunnels;
    private final float caverns;
    private final float noodles;
    private final float lakes;
    private final int minHeightBlocks;
    private final int maxDepthBlocks;
    private final boolean entrances;

    public BiomeCaveStruct(
            float tunnels,
            float caverns,
            float noodles,
            float lakes,
            int minHeightBlocks,
            int maxDepthBlocks,
            boolean entrances) {

        this.tunnels = tunnels;
        this.caverns = caverns;
        this.noodles = noodles;
        this.lakes = lakes;
        this.minHeightBlocks = minHeightBlocks;
        this.maxDepthBlocks = maxDepthBlocks;
        this.entrances = entrances;
    }

    // Accessible \\

    public float getTunnels() {
        return tunnels;
    }

    public float getCaverns() {
        return caverns;
    }

    public float getNoodles() {
        return noodles;
    }

    public float getLakes() {
        return lakes;
    }

    public int getMinHeightBlocks() {
        return minHeightBlocks;
    }

    public int getMaxDepthBlocks() {
        return maxDepthBlocks;
    }

    public boolean hasEntrances() {
        return entrances;
    }
}
