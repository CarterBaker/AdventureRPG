package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.StructPackage;

class TerrainCaveEntryStruct extends StructPackage {

    /*
     * One cave biome a surface biome holds beneath it, resolved: the cave
     * biome's profile, the share of underground regions it claims, the band
     * of heights it lies in and how far below the ground it may reach.
     */

    final TerrainCaveProfileStruct profile;
    final float chance;
    final int minHeightBlocks;
    final int maxHeightBlocks;
    final int maxDepthBlocks;

    TerrainCaveEntryStruct(
            TerrainCaveProfileStruct profile,
            float chance,
            int minHeightBlocks,
            int maxHeightBlocks,
            int maxDepthBlocks) {

        this.profile = profile;
        this.chance = chance;
        this.minHeightBlocks = minHeightBlocks;
        this.maxHeightBlocks = maxHeightBlocks;
        this.maxDepthBlocks = maxDepthBlocks;
    }

    // Accessible \\

    // Whether a block at this height and depth below the ground lies within the cave biome's band
    boolean reaches(int worldY, int groundHeight) {
        return worldY >= minHeightBlocks && worldY <= maxHeightBlocks && groundHeight - worldY <= maxDepthBlocks;
    }
}
