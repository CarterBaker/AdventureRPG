package com.AdventureRPG.WorldPipeline;

public class WorldRegion {

    // Region
    public final int regionID;
    public final int climate;
    public final int elevation;

    // Base \\

    public WorldRegion(int regionID, int climate, int elevation) {

        // Region
        this.regionID = regionID;
        this.climate = climate;
        this.elevation = elevation;
    }
}
