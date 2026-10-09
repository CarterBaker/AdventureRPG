package application.bootstrap.worldpipeline.road;

import engine.root.DataPackage;

public class RoadData extends DataPackage {

    /*
     * Immutable road type built from ARPG by RoadBuilder: what a road is made
     * of and how it meets the land. Its surface draws each column from a
     * weighted mix of blocks, a shoulder may soften its edges into the
     * ground, a base fills under it where it runs above the land, and it
     * keeps a clear height over itself. Its grade is how steeply it may climb
     * per block, smoothed slopes lay half-step slabs so it ramps rather than
     * stairs, and a bridge and a tunnel, each optional, carry it over what
     * it cannot climb. The colors are the ones distant terrain and the map
     * draw it in.
     */

    // Identity
    private final String roadName;
    private final short roadID;
    private final int nameSeed;

    // Surface
    private final float widthBlocks;
    private final short[] surfaceBlockIDs;
    private final float[] surfaceCumulativeWeights;
    private final boolean smoothSlopes;

    // Edges
    private final short shoulderBlockID;
    private final float shoulderWidthBlocks;
    private final short baseBlockID;
    private final int clearanceBlocks;

    // Grade
    private final float maxGrade;

    // Crossings
    private final RoadBridgeStruct bridge;
    private final RoadTunnelStruct tunnel;

    // Colors
    private final int surfaceColor;
    private final int sideColor;
    private final int deckColor;

    // Constructor \\

    public RoadData(
            String roadName,
            short roadID,
            int nameSeed,
            float widthBlocks,
            short[] surfaceBlockIDs,
            float[] surfaceCumulativeWeights,
            boolean smoothSlopes,
            short shoulderBlockID,
            float shoulderWidthBlocks,
            short baseBlockID,
            int clearanceBlocks,
            float maxGrade,
            RoadBridgeStruct bridge,
            RoadTunnelStruct tunnel,
            int surfaceColor,
            int sideColor,
            int deckColor) {

        // Identity
        this.roadName = roadName;
        this.roadID = roadID;
        this.nameSeed = nameSeed;

        // Surface
        this.widthBlocks = widthBlocks;
        this.surfaceBlockIDs = surfaceBlockIDs;
        this.surfaceCumulativeWeights = surfaceCumulativeWeights;
        this.smoothSlopes = smoothSlopes;

        // Edges
        this.shoulderBlockID = shoulderBlockID;
        this.shoulderWidthBlocks = shoulderWidthBlocks;
        this.baseBlockID = baseBlockID;
        this.clearanceBlocks = clearanceBlocks;

        // Grade
        this.maxGrade = maxGrade;

        // Crossings
        this.bridge = bridge;
        this.tunnel = tunnel;

        // Colors
        this.surfaceColor = surfaceColor;
        this.sideColor = sideColor;
        this.deckColor = deckColor;
    }

    // Accessible \\

    public String getRoadName() {
        return roadName;
    }

    public short getRoadID() {
        return roadID;
    }

    public int getNameSeed() {
        return nameSeed;
    }

    public float getWidthBlocks() {
        return widthBlocks;
    }

    public float getHalfWidthBlocks() {
        return widthBlocks * 0.5f;
    }

    public short[] getSurfaceBlockIDs() {
        return surfaceBlockIDs;
    }

    public float[] getSurfaceCumulativeWeights() {
        return surfaceCumulativeWeights;
    }

    public boolean hasSmoothSlopes() {
        return smoothSlopes;
    }

    public short getShoulderBlockID() {
        return shoulderBlockID;
    }

    public float getShoulderWidthBlocks() {
        return shoulderWidthBlocks;
    }

    public boolean hasShoulder() {
        return shoulderWidthBlocks > 0f;
    }

    public short getBaseBlockID() {
        return baseBlockID;
    }

    public int getClearanceBlocks() {
        return clearanceBlocks;
    }

    public float getMaxGrade() {
        return maxGrade;
    }

    public RoadBridgeStruct getBridge() {
        return bridge;
    }

    public boolean hasBridge() {
        return bridge != null;
    }

    public RoadTunnelStruct getTunnel() {
        return tunnel;
    }

    public boolean hasTunnel() {
        return tunnel != null;
    }

    public int getSurfaceColor() {
        return surfaceColor;
    }

    public int getSideColor() {
        return sideColor;
    }

    public int getDeckColor() {
        return deckColor;
    }
}
