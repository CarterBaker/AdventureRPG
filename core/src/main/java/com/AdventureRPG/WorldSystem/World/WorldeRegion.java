package com.AdventureRPG.WorldSystem.World;

public class WorldeRegion {
    int regionID;
    int climate;
    int height;
    Boolean river;
    Boolean road;

    public WorldeRegion(int regionID, int climate, int height, boolean river, boolean road) {
        this.regionID = regionID;
        this.climate = climate;
        this.height = height;
        this.river = river;
        this.road = road;
    }
}
