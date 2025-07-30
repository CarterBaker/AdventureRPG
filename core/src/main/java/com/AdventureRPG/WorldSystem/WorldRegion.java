package com.AdventureRPG.WorldSystem;

public class WorldRegion {

    // Variables

    // Base

    public final int regionID;
    public final int climate;
    public final int elevation;
    public final Boolean river;
    public final Boolean road;

    public WorldRegion(int regionID, int climate, int elevation, boolean river, boolean road) {
        this.regionID = regionID;
        this.climate = climate;
        this.elevation = elevation;
        this.river = river;
        this.road = road;
    }
}
