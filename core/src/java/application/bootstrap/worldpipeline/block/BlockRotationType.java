package application.bootstrap.worldpipeline.block;

public enum BlockRotationType {

    /*
     * Defines how a block's orientation palette is interpreted during geometry
     * and texture resolution. Stored per-block in the rotation palette alongside
     * the block and biome palettes in each SubChunkInstance. Read by
     * FullGeometryBranch and other geometry branches to determine face mapping
     * and encoded face index during mesh assembly.
     */

    NONE, // Rotation palette ignored entirely
    CARDINAL, // Only N/E/S/W — UP/DOWN facing falls back to default
    FULL, // All 6 directions × 4 spins = 24 orientations
    NATURAL_FULL // Top/bottom use natural axis orientation, sides rotate freely
}