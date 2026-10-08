package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.BiomeVeinStruct;
import engine.root.StructPackage;

class TerrainSurfaceProfileStruct extends StructPackage {

    /*
     * One biome's dressing resolved against the block palette: the blocks it
     * lays on land, under water and on faces too steep to hold soil, the
     * slope those faces start at, its veins with their block IDs, and the map
     * colors distant terrain draws it with. Built once per biome and revision
     * by WorldGenerationManager and shared by every thread.
     */

    final BiomeHandle biomeHandle;
    final short surfaceBlockID;
    final short subsurfaceBlockID;
    final short underwaterBlockID;
    final short rockBlockID;
    final float rockSlope;
    final BiomeVeinStruct[] veins;
    final short[] veinBlockIDs;
    final int surfaceTopColor;
    final int underwaterTopColor;
    final int underwaterSideColor;
    final int rockSideColor;

    TerrainSurfaceProfileStruct(
            BiomeHandle biomeHandle,
            short surfaceBlockID,
            short subsurfaceBlockID,
            short underwaterBlockID,
            short rockBlockID,
            float rockSlope,
            BiomeVeinStruct[] veins,
            short[] veinBlockIDs,
            int surfaceTopColor,
            int underwaterTopColor,
            int underwaterSideColor,
            int rockSideColor) {

        this.biomeHandle = biomeHandle;
        this.surfaceBlockID = surfaceBlockID;
        this.subsurfaceBlockID = subsurfaceBlockID;
        this.underwaterBlockID = underwaterBlockID;
        this.rockBlockID = rockBlockID;
        this.rockSlope = rockSlope;
        this.veins = veins;
        this.veinBlockIDs = veinBlockIDs;
        this.surfaceTopColor = surfaceTopColor;
        this.underwaterTopColor = underwaterTopColor;
        this.underwaterSideColor = underwaterSideColor;
        this.rockSideColor = rockSideColor;
    }
}
