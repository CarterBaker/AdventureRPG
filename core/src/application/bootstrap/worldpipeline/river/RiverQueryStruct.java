package application.bootstrap.worldpipeline.river;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import engine.root.StructPackage;

public class RiverQueryStruct extends StructPackage {

    /*
     * Where one point stands against a reach's centreline, written in place
     * by RiverPathUtility.locate(): its distance from the line, how far along
     * the reach the nearest place on it lies, the channel's half width and
     * depth there interpolated between its points, and the level, zone and
     * biome of the nearer point. Reused per worker, never allocated per
     * query.
     */

    // Line
    private double distance;
    private float along;

    // Channel
    private int levelBlocks;
    private float halfWidthBlocks;
    private float depthBlocks;
    private byte zone;
    private BiomeHandle biome;

    // Management \\

    public void set(
            double distance,
            float along,
            int levelBlocks,
            float halfWidthBlocks,
            float depthBlocks,
            byte zone,
            BiomeHandle biome) {

        // Line
        this.distance = distance;
        this.along = along;

        // Channel
        this.levelBlocks = levelBlocks;
        this.halfWidthBlocks = halfWidthBlocks;
        this.depthBlocks = depthBlocks;
        this.zone = zone;
        this.biome = biome;
    }

    // Accessible \\

    public double getDistance() {
        return distance;
    }

    public float getAlong() {
        return along;
    }

    public int getLevelBlocks() {
        return levelBlocks;
    }

    public float getHalfWidthBlocks() {
        return halfWidthBlocks;
    }

    public float getDepthBlocks() {
        return depthBlocks;
    }

    public byte getZone() {
        return zone;
    }

    public BiomeHandle getBiome() {
        return biome;
    }
}
