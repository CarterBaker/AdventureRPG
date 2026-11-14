package application.bootstrap.worldpipeline.settlement;

import application.bootstrap.worldpipeline.layout.LayoutRangeStruct;
import application.bootstrap.worldpipeline.layout.LayoutRoadKind;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SettlementData extends DataPackage {

    /*
     * Immutable settlement type built from ARPG by SettlementBuilder: how
     * large a place of its kind grows and how it is laid out, whatever
     * architecture builds it. Its weight is how often a free site grows one,
     * its ground rules where one may stand. A settlement grows around a
     * centre role ringed by a round street, with streets leaving the ring
     * and lanes branching off them, lots set back along them filled first by
     * its roles and then by its fill role, a few buildings bridged over its
     * streets, an optional curtain wall with towers and gates, and trails out
     * to outposts or dead ends. Its link names the kind of road it is joined
     * to its neighbours by, and its reach is the farthest anything it lays
     * can stand from its centre.
     */

    // Identity
    private final String settlementName;
    private final short settlementID;
    private final int nameSeed;
    private final String displayName;

    // Frequency
    private final float weight;

    // Ground
    private final int radiusBlocks;
    private final int minGroundHeightBlocks;
    private final int maxGroundHeightBlocks;
    private final int maxSlopeBlocks;

    // Centre
    private final String centerRole;

    // Streets
    private final LayoutRangeStruct spokes;
    private final LayoutRangeStruct spokeLengthBlocks;
    private final LayoutRangeStruct branches;
    private final LayoutRangeStruct branchLengthBlocks;
    private final float bend;

    // Lots
    private final int setbackBlocks;
    private final int maxLots;
    private final String fillRole;
    private final ObjectArrayList<SettlementRoleStruct> roles;
    private final LayoutRangeStruct overpasses;

    // Wall — a radius of zero raises none
    private final int wallRadiusBlocks;
    private final int towerSpacingBlocks;

    // Outposts
    private final LayoutRangeStruct outposts;
    private final LayoutRangeStruct trailLengthBlocks;
    private final float outpostChance;

    // Links
    private final LayoutRoadKind link;

    // Reach
    private final int reachBlocks;

    // Constructor \\

    public SettlementData(
            String settlementName,
            short settlementID,
            int nameSeed,
            String displayName,
            float weight,
            int radiusBlocks,
            int minGroundHeightBlocks,
            int maxGroundHeightBlocks,
            int maxSlopeBlocks,
            String centerRole,
            LayoutRangeStruct spokes,
            LayoutRangeStruct spokeLengthBlocks,
            LayoutRangeStruct branches,
            LayoutRangeStruct branchLengthBlocks,
            float bend,
            int setbackBlocks,
            int maxLots,
            String fillRole,
            ObjectArrayList<SettlementRoleStruct> roles,
            LayoutRangeStruct overpasses,
            int wallRadiusBlocks,
            int towerSpacingBlocks,
            LayoutRangeStruct outposts,
            LayoutRangeStruct trailLengthBlocks,
            float outpostChance,
            LayoutRoadKind link,
            int reachBlocks) {

        // Identity
        this.settlementName = settlementName;
        this.settlementID = settlementID;
        this.nameSeed = nameSeed;
        this.displayName = displayName;

        // Frequency
        this.weight = weight;

        // Ground
        this.radiusBlocks = radiusBlocks;
        this.minGroundHeightBlocks = minGroundHeightBlocks;
        this.maxGroundHeightBlocks = maxGroundHeightBlocks;
        this.maxSlopeBlocks = maxSlopeBlocks;

        // Centre
        this.centerRole = centerRole;

        // Streets
        this.spokes = spokes;
        this.spokeLengthBlocks = spokeLengthBlocks;
        this.branches = branches;
        this.branchLengthBlocks = branchLengthBlocks;
        this.bend = bend;

        // Lots
        this.setbackBlocks = setbackBlocks;
        this.maxLots = maxLots;
        this.fillRole = fillRole;
        this.roles = roles;
        this.overpasses = overpasses;

        // Wall
        this.wallRadiusBlocks = wallRadiusBlocks;
        this.towerSpacingBlocks = towerSpacingBlocks;

        // Outposts
        this.outposts = outposts;
        this.trailLengthBlocks = trailLengthBlocks;
        this.outpostChance = outpostChance;

        // Links
        this.link = link;

        // Reach
        this.reachBlocks = reachBlocks;
    }

    // Accessible \\

    public String getSettlementName() {
        return settlementName;
    }

    public short getSettlementID() {
        return settlementID;
    }

    public int getNameSeed() {
        return nameSeed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getWeight() {
        return weight;
    }

    public int getRadiusBlocks() {
        return radiusBlocks;
    }

    public int getMinGroundHeightBlocks() {
        return minGroundHeightBlocks;
    }

    public int getMaxGroundHeightBlocks() {
        return maxGroundHeightBlocks;
    }

    public int getMaxSlopeBlocks() {
        return maxSlopeBlocks;
    }

    public String getCenterRole() {
        return centerRole;
    }

    public LayoutRangeStruct getSpokes() {
        return spokes;
    }

    public LayoutRangeStruct getSpokeLengthBlocks() {
        return spokeLengthBlocks;
    }

    public LayoutRangeStruct getBranches() {
        return branches;
    }

    public LayoutRangeStruct getBranchLengthBlocks() {
        return branchLengthBlocks;
    }

    public float getBend() {
        return bend;
    }

    public int getSetbackBlocks() {
        return setbackBlocks;
    }

    public int getMaxLots() {
        return maxLots;
    }

    public String getFillRole() {
        return fillRole;
    }

    public ObjectArrayList<SettlementRoleStruct> getRoles() {
        return roles;
    }

    public LayoutRangeStruct getOverpasses() {
        return overpasses;
    }

    public int getWallRadiusBlocks() {
        return wallRadiusBlocks;
    }

    public boolean hasWall() {
        return wallRadiusBlocks > 0;
    }

    public int getTowerSpacingBlocks() {
        return towerSpacingBlocks;
    }

    public LayoutRangeStruct getOutposts() {
        return outposts;
    }

    public LayoutRangeStruct getTrailLengthBlocks() {
        return trailLengthBlocks;
    }

    public float getOutpostChance() {
        return outpostChance;
    }

    public LayoutRoadKind getLink() {
        return link;
    }

    public int getReachBlocks() {
        return reachBlocks;
    }
}
