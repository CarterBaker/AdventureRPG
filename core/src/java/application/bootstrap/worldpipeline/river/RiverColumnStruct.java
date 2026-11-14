package application.bootstrap.worldpipeline.river;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class RiverColumnStruct extends StructPackage {

    /*
     * What the rivers make of one block column, written in place by
     * RiverManager: the ground they leave it, the still water they stand
     * over it, the part of the river it lies in — channel, bank, the wall of
     * the cut, a tunnel, the curtain pouring into the sea or the trench past
     * it — the band a tunnel hollows through it with the level of its water,
     * how high caves may reach under a river and how low beside a tunnel, and
     * the biome whose river blocks dress it. Untouched columns keep their
     * ground and report no zone.
     */

    public static final byte ZONE_NONE = 0;
    public static final byte ZONE_CHANNEL = 1;
    public static final byte ZONE_BANK = 2;
    public static final byte ZONE_WALL = 3;
    public static final byte ZONE_TUNNEL = 4;
    public static final byte ZONE_CURTAIN = 5;
    public static final byte ZONE_TRENCH = 6;

    public static final int NONE = EngineSetting.RIVER_LEVEL_NONE;
    public static final int CAVE_FLOOR_FREE = Integer.MIN_VALUE;
    public static final int CAVE_CEILING_FREE = Integer.MAX_VALUE;

    // Ground
    private int groundHeightBlocks;
    private int waterLevelBlocks;
    private byte zone;
    private BiomeHandle biome;

    // Tunnel
    private int tunnelFloorY;
    private int tunnelCeilingY;
    private int tunnelLevelY;

    // Caves
    private int caveFloorMinY;
    private int caveCeilingMaxY;

    // Management \\

    public void reset(int groundHeightBlocks) {
        this.groundHeightBlocks = groundHeightBlocks;
        this.waterLevelBlocks = NONE;
        this.zone = ZONE_NONE;
        this.biome = null;
        this.tunnelFloorY = NONE;
        this.tunnelCeilingY = NONE;
        this.tunnelLevelY = NONE;
        this.caveFloorMinY = CAVE_FLOOR_FREE;
        this.caveCeilingMaxY = CAVE_CEILING_FREE;
    }

    public void setGround(int groundHeightBlocks, byte zone, BiomeHandle biome) {
        this.groundHeightBlocks = groundHeightBlocks;
        this.zone = zone;
        this.biome = biome;
    }

    public void setWaterLevel(int waterLevelBlocks) {
        this.waterLevelBlocks = waterLevelBlocks;
    }

    public void setTunnel(int floorY, int ceilingY, int levelY) {
        this.tunnelFloorY = floorY;
        this.tunnelCeilingY = ceilingY;
        this.tunnelLevelY = levelY;
    }

    public void setCaveFloorMinY(int caveFloorMinY) {
        this.caveFloorMinY = caveFloorMinY;
    }

    public void setCaveCeilingMaxY(int caveCeilingMaxY) {
        this.caveCeilingMaxY = caveCeilingMaxY;
    }

    // Accessible \\

    public boolean isAffected() {
        return zone != ZONE_NONE || caveFloorMinY != CAVE_FLOOR_FREE || caveCeilingMaxY != CAVE_CEILING_FREE;
    }

    public int getGroundHeightBlocks() {
        return groundHeightBlocks;
    }

    public boolean hasWater() {
        return waterLevelBlocks != NONE;
    }

    public int getWaterLevelBlocks() {
        return waterLevelBlocks;
    }

    public byte getZone() {
        return zone;
    }

    public BiomeHandle getBiome() {
        return biome;
    }

    public boolean hasTunnel() {
        return tunnelFloorY != NONE;
    }

    public int getTunnelFloorY() {
        return tunnelFloorY;
    }

    public int getTunnelCeilingY() {
        return tunnelCeilingY;
    }

    public int getTunnelLevelY() {
        return tunnelLevelY;
    }

    public int getCaveFloorMinY() {
        return caveFloorMinY;
    }

    public int getCaveCeilingMaxY() {
        return caveCeilingMaxY;
    }
}
