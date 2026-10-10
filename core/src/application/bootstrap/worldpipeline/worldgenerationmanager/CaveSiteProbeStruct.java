package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.StructPackage;

class CaveSiteProbeStruct extends StructPackage {

    /*
     * What the ground holds at one point, read straight from the biome field
     * without a chunk around it: its height, whether it stands dry, away
     * from the sea, still water and the sea's walled band, the share of
     * underground lakes its biomes hold, and the lowest block its caves
     * reach. Shaped exactly as terrain is shaped, up to the interpolation
     * a generated chunk applies.
     */

    int groundHeight;
    boolean dry;
    float caveLakes;
    int caveFloorY;
}
