package com.AdventureRPG.WorldSystem;

public class WorldRegion {

    // Variables

    // Base

    public final int regionID;
    public final int climate;
    public final int elevation;

    public WorldRegion(int regionID, int climate, int elevation) {
        this.regionID = regionID;
        this.climate = climate;
        this.elevation = elevation;
    }
}
