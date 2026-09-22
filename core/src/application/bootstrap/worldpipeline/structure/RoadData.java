package application.bootstrap.worldpipeline.structure;

import engine.root.DataPackage;

public class RoadData extends DataPackage {

    /*
     * The blocks and shape rules a road is built from. A road is a band
     * widthBlocks wide around a planned centreline. The surface is rolled per
     * block from a weighted list so a road reads as worn rather than tiled,
     * with an optional edge block along both margins. Height along the path
     * is the terrain smoothed and grade-limited to maxGrade; wherever that
     * height ends up more than maxFillBlocks above the ground the road
     * becomes a bridge, and wherever the ground rises more than
     * tunnelHeightBlocks + 2 above it the road becomes a tunnel. alwaysTunnel
     * forces every point into a tunnel — a dungeon corridor, which stays an
     * enclosed passage even where it breaks the surface. The cumulative
     * weights array is normalized to end at 1.0.
     */

    private final int widthBlocks;

    private final short[] surfaceBlockIDs;
    private final float[] surfaceCumulativeWeights;
    private final short edgeBlockID;
    private final short foundationBlockID;

    private final int clearanceBlocks;
    private final int maxFillBlocks;
    private final float maxGrade;
    private final int smoothingRadiusBlocks;

    private final int tunnelHeightBlocks;
    private final short tunnelWallBlockID;
    private final short tunnelCeilingBlockID;
    private final boolean alwaysTunnel;

    private final short bridgeDeckBlockID;
    private final short bridgeRailBlockID;
    private final short bridgeSupportBlockID;
    private final int supportSpacingBlocks;

    public RoadData(
            int widthBlocks,
            short[] surfaceBlockIDs,
            float[] surfaceCumulativeWeights,
            short edgeBlockID,
            short foundationBlockID,
            int clearanceBlocks,
            int maxFillBlocks,
            float maxGrade,
            int smoothingRadiusBlocks,
            int tunnelHeightBlocks,
            short tunnelWallBlockID,
            short tunnelCeilingBlockID,
            boolean alwaysTunnel,
            short bridgeDeckBlockID,
            short bridgeRailBlockID,
            short bridgeSupportBlockID,
            int supportSpacingBlocks) {

        this.widthBlocks = widthBlocks;
        this.surfaceBlockIDs = surfaceBlockIDs;
        this.surfaceCumulativeWeights = surfaceCumulativeWeights;
        this.edgeBlockID = edgeBlockID;
        this.foundationBlockID = foundationBlockID;
        this.clearanceBlocks = clearanceBlocks;
        this.maxFillBlocks = maxFillBlocks;
        this.maxGrade = maxGrade;
        this.smoothingRadiusBlocks = smoothingRadiusBlocks;
        this.tunnelHeightBlocks = tunnelHeightBlocks;
        this.tunnelWallBlockID = tunnelWallBlockID;
        this.tunnelCeilingBlockID = tunnelCeilingBlockID;
        this.alwaysTunnel = alwaysTunnel;
        this.bridgeDeckBlockID = bridgeDeckBlockID;
        this.bridgeRailBlockID = bridgeRailBlockID;
        this.bridgeSupportBlockID = bridgeSupportBlockID;
        this.supportSpacingBlocks = supportSpacingBlocks;
    }

    /*
     * Picks a surface block for a roll in [0, 1). Linear — a road rarely
     * lists more than three or four surface blocks.
     */
    public short pickSurfaceBlockID(float roll) {

        for (int i = 0; i < surfaceCumulativeWeights.length; i++)
            if (roll < surfaceCumulativeWeights[i])
                return surfaceBlockIDs[i];

        return surfaceBlockIDs[surfaceBlockIDs.length - 1];
    }

    public float getHalfWidthBlocks() {
        return widthBlocks * 0.5f;
    }

    public int getWidthBlocks() {
        return widthBlocks;
    }

    public short getEdgeBlockID() {
        return edgeBlockID;
    }

    public boolean hasEdgeBlock() {
        return edgeBlockID != StructureTemplateData.BLOCK_SKIP;
    }

    public short getFoundationBlockID() {
        return foundationBlockID;
    }

    public int getClearanceBlocks() {
        return clearanceBlocks;
    }

    public int getMaxFillBlocks() {
        return maxFillBlocks;
    }

    public float getMaxGrade() {
        return maxGrade;
    }

    public int getSmoothingRadiusBlocks() {
        return smoothingRadiusBlocks;
    }

    public int getTunnelHeightBlocks() {
        return tunnelHeightBlocks;
    }

    public int getTunnelThresholdBlocks() {
        return tunnelHeightBlocks + 2;
    }

    public short getTunnelWallBlockID() {
        return tunnelWallBlockID;
    }

    public short getTunnelCeilingBlockID() {
        return tunnelCeilingBlockID;
    }

    public boolean isAlwaysTunnel() {
        return alwaysTunnel;
    }

    public short getBridgeDeckBlockID() {
        return bridgeDeckBlockID;
    }

    public short getBridgeRailBlockID() {
        return bridgeRailBlockID;
    }

    public boolean hasBridgeRail() {
        return bridgeRailBlockID != StructureTemplateData.BLOCK_SKIP;
    }

    public short getBridgeSupportBlockID() {
        return bridgeSupportBlockID;
    }

    public int getSupportSpacingBlocks() {
        return supportSpacingBlocks;
    }
}
