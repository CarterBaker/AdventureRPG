package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biome.BiomeVeinStruct;
import application.bootstrap.worldpipeline.cavebiome.CaveBiomeHandle;
import engine.root.StructPackage;

class TerrainCaveProfileStruct extends StructPackage {

    /*
     * One cave biome's dressing resolved against the block palette: the rock
     * it lines its caves with and how deep the lining runs, the blocks of its
     * floors, ceilings and still water beds, the coverage it lays over
     * floors, walls and ceilings with how many levels a cell may fall short
     * of each, the stalactites and stalagmites it grows and its veins with
     * their block IDs. Built once per cave biome by WorldGenerationManager
     * and shared by every thread.
     */

    final CaveBiomeHandle caveBiomeHandle;
    final short rockBlockID;
    final short floorBlockID;
    final short ceilingBlockID;
    final short bedBlockID;
    final int shellBlocks;
    final short floorCoverage;
    final int floorCoverageVariance;
    final short wallCoverage;
    final int wallCoverageVariance;
    final short ceilingCoverage;
    final int ceilingCoverageVariance;
    final boolean speleothems;
    final short speleothemBlockID;
    final float stalactites;
    final float stalagmites;
    final int maxLengthBlocks;
    final float giants;
    final int maxGiantRadiusBlocks;
    final BiomeVeinStruct[] veins;
    final short[] veinBlockIDs;

    TerrainCaveProfileStruct(
            CaveBiomeHandle caveBiomeHandle,
            short rockBlockID,
            short floorBlockID,
            short ceilingBlockID,
            short bedBlockID,
            int shellBlocks,
            short floorCoverage,
            int floorCoverageVariance,
            short wallCoverage,
            int wallCoverageVariance,
            short ceilingCoverage,
            int ceilingCoverageVariance,
            boolean speleothems,
            short speleothemBlockID,
            float stalactites,
            float stalagmites,
            int maxLengthBlocks,
            float giants,
            int maxGiantRadiusBlocks,
            BiomeVeinStruct[] veins,
            short[] veinBlockIDs) {

        this.caveBiomeHandle = caveBiomeHandle;
        this.rockBlockID = rockBlockID;
        this.floorBlockID = floorBlockID;
        this.ceilingBlockID = ceilingBlockID;
        this.bedBlockID = bedBlockID;
        this.shellBlocks = shellBlocks;
        this.floorCoverage = floorCoverage;
        this.floorCoverageVariance = floorCoverageVariance;
        this.wallCoverage = wallCoverage;
        this.wallCoverageVariance = wallCoverageVariance;
        this.ceilingCoverage = ceilingCoverage;
        this.ceilingCoverageVariance = ceilingCoverageVariance;
        this.speleothems = speleothems;
        this.speleothemBlockID = speleothemBlockID;
        this.stalactites = stalactites;
        this.stalagmites = stalagmites;
        this.maxLengthBlocks = maxLengthBlocks;
        this.giants = giants;
        this.maxGiantRadiusBlocks = maxGiantRadiusBlocks;
        this.veins = veins;
        this.veinBlockIDs = veinBlockIDs;
    }
}
