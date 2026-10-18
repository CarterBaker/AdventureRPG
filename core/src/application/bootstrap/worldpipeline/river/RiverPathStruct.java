package application.bootstrap.worldpipeline.river;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import engine.root.StructPackage;

public class RiverPathStruct extends StructPackage {

    /*
     * One planned reach of river, from a drainage cell's heart to the heart
     * of the cell it drains into, immutable once planned. Its centreline runs
     * as points a few blocks apart, held as offsets from its origin in the
     * unwrapped blocks of the plan, each with how far along the reach it lies,
     * the level of the water's surface there, the channel's half width and
     * depth, how the river runs there — open, underground, pouring off its
     * lip into the sea or as a trench the tide fills past it — and the biome
     * whose river settings shape it. The bounds take in the widest valley the
     * reach can cut, so a chunk tells whether the reach can touch it at a
     * glance.
     */

    public static final byte ZONE_OPEN = 0;
    public static final byte ZONE_TUNNEL = 1;
    public static final byte ZONE_CURTAIN = 2;
    public static final byte ZONE_TRENCH = 3;

    // Network
    private final RiverNetworkStruct network;
    private final int sourceIndex;

    // Origin — unwrapped blocks
    private final double originX;
    private final double originZ;

    // Points — offsets from the origin
    private final float[] pointX;
    private final float[] pointZ;
    private final float[] pointDistances;
    private final short[] pointLevels;
    private final float[] pointHalfWidths;
    private final float[] pointDepths;
    private final byte[] pointZones;
    private final byte[] pointBiomes;
    private final BiomeHandle[] biomes;

    // Bounds — offsets from the origin
    private final float minX;
    private final float maxX;
    private final float minZ;
    private final float maxZ;
    private final float reachBlocks;

    // Constructor \\

    public RiverPathStruct(
            RiverNetworkStruct network,
            int sourceIndex,
            double originX,
            double originZ,
            float[] pointX,
            float[] pointZ,
            float[] pointDistances,
            short[] pointLevels,
            float[] pointHalfWidths,
            float[] pointDepths,
            byte[] pointZones,
            byte[] pointBiomes,
            BiomeHandle[] biomes,
            float reachBlocks) {

        // Network
        this.network = network;
        this.sourceIndex = sourceIndex;

        // Origin
        this.originX = originX;
        this.originZ = originZ;

        // Points
        this.pointX = pointX;
        this.pointZ = pointZ;
        this.pointDistances = pointDistances;
        this.pointLevels = pointLevels;
        this.pointHalfWidths = pointHalfWidths;
        this.pointDepths = pointDepths;
        this.pointZones = pointZones;
        this.pointBiomes = pointBiomes;
        this.biomes = biomes;

        // Bounds
        float lowX = Float.MAX_VALUE;
        float highX = -Float.MAX_VALUE;
        float lowZ = Float.MAX_VALUE;
        float highZ = -Float.MAX_VALUE;

        for (int i = 0; i < pointX.length; i++) {
            lowX = Math.min(lowX, pointX[i]);
            highX = Math.max(highX, pointX[i]);
            lowZ = Math.min(lowZ, pointZ[i]);
            highZ = Math.max(highZ, pointZ[i]);
        }

        this.minX = lowX - reachBlocks;
        this.maxX = highX + reachBlocks;
        this.minZ = lowZ - reachBlocks;
        this.maxZ = highZ + reachBlocks;
        this.reachBlocks = reachBlocks;
    }

    // Bounds \\

    // Whether a span of offsets, inclusive, can lie within the reach's widest valley
    public boolean reaches(double fromX, double fromZ, double toX, double toZ) {
        return toX >= minX && fromX <= maxX && toZ >= minZ && fromZ <= maxZ;
    }

    // Accessible \\

    public RiverNetworkStruct getNetwork() {
        return network;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public double getOriginX() {
        return originX;
    }

    public double getOriginZ() {
        return originZ;
    }

    public int getPointCount() {
        return pointX.length;
    }

    public float getPointX(int index) {
        return pointX[index];
    }

    public float getPointZ(int index) {
        return pointZ[index];
    }

    public float getPointDistance(int index) {
        return pointDistances[index];
    }

    public int getPointLevel(int index) {
        return pointLevels[index];
    }

    public float getPointHalfWidth(int index) {
        return pointHalfWidths[index];
    }

    public float getPointDepth(int index) {
        return pointDepths[index];
    }

    public byte getPointZone(int index) {
        return pointZones[index];
    }

    public BiomeHandle getPointBiome(int index) {
        return biomes[pointBiomes[index]];
    }

    public float getReachBlocks() {
        return reachBlocks;
    }
}
