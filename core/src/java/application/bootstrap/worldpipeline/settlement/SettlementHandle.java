package application.bootstrap.worldpipeline.settlement;

import application.bootstrap.worldpipeline.layout.LayoutRangeStruct;
import application.bootstrap.worldpipeline.layout.LayoutRoadKind;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SettlementHandle extends HandlePackage {

    /*
     * Persistent settlement type. Wraps SettlementData and delegates all
     * access through it.
     */

    // Internal
    private SettlementData settlementData;

    // Constructor \\

    public void constructor(SettlementData settlementData) {
        this.settlementData = settlementData;
    }

    // Accessible \\

    public SettlementData getSettlementData() {
        return settlementData;
    }

    public String getSettlementName() {
        return settlementData.getSettlementName();
    }

    public short getSettlementID() {
        return settlementData.getSettlementID();
    }

    public int getNameSeed() {
        return settlementData.getNameSeed();
    }

    public String getDisplayName() {
        return settlementData.getDisplayName();
    }

    public float getWeight() {
        return settlementData.getWeight();
    }

    public int getRadiusBlocks() {
        return settlementData.getRadiusBlocks();
    }

    public int getMinGroundHeightBlocks() {
        return settlementData.getMinGroundHeightBlocks();
    }

    public int getMaxGroundHeightBlocks() {
        return settlementData.getMaxGroundHeightBlocks();
    }

    public int getMaxSlopeBlocks() {
        return settlementData.getMaxSlopeBlocks();
    }

    public String getCenterRole() {
        return settlementData.getCenterRole();
    }

    public LayoutRangeStruct getSpokes() {
        return settlementData.getSpokes();
    }

    public LayoutRangeStruct getSpokeLengthBlocks() {
        return settlementData.getSpokeLengthBlocks();
    }

    public LayoutRangeStruct getBranches() {
        return settlementData.getBranches();
    }

    public LayoutRangeStruct getBranchLengthBlocks() {
        return settlementData.getBranchLengthBlocks();
    }

    public float getBend() {
        return settlementData.getBend();
    }

    public int getSetbackBlocks() {
        return settlementData.getSetbackBlocks();
    }

    public int getMaxLots() {
        return settlementData.getMaxLots();
    }

    public String getFillRole() {
        return settlementData.getFillRole();
    }

    public ObjectArrayList<SettlementRoleStruct> getRoles() {
        return settlementData.getRoles();
    }

    public LayoutRangeStruct getOverpasses() {
        return settlementData.getOverpasses();
    }

    public int getWallRadiusBlocks() {
        return settlementData.getWallRadiusBlocks();
    }

    public boolean hasWall() {
        return settlementData.hasWall();
    }

    public int getTowerSpacingBlocks() {
        return settlementData.getTowerSpacingBlocks();
    }

    public LayoutRangeStruct getOutposts() {
        return settlementData.getOutposts();
    }

    public LayoutRangeStruct getTrailLengthBlocks() {
        return settlementData.getTrailLengthBlocks();
    }

    public float getOutpostChance() {
        return settlementData.getOutpostChance();
    }

    public LayoutRoadKind getLink() {
        return settlementData.getLink();
    }

    public int getReachBlocks() {
        return settlementData.getReachBlocks();
    }
}
