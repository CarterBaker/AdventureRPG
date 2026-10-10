package application.bootstrap.worldpipeline.covering;

import engine.root.HandlePackage;

public class CoveringHandle extends HandlePackage {

    /*
     * Persistent covering definition record. Wraps CoveringData and delegates
     * all access through it. Registered in CoveringManager from bootstrap to
     * shutdown.
     */

    // Internal
    private CoveringData coveringData;

    // Constructor \\

    public void constructor(CoveringData coveringData) {
        this.coveringData = coveringData;
    }

    // Accessible \\

    public CoveringData getCoveringData() {
        return coveringData;
    }

    public String getCoveringName() {
        return coveringData.getCoveringName();
    }

    public short getCoveringID() {
        return coveringData.getCoveringID();
    }

    public int getNameSeed() {
        return coveringData.getNameSeed();
    }

    public boolean canHost(short blockID) {
        return coveringData.canHost(blockID);
    }

    public int getTopTileID() {
        return coveringData.getTopTileID();
    }

    public int getSideTileID() {
        return coveringData.getSideTileID();
    }

    public boolean hasSide() {
        return coveringData.hasSide();
    }

    public int getMapColor() {
        return coveringData.getMapColor();
    }

    public float getTintStrength() {
        return coveringData.getTintStrength();
    }

    public float getGrowthChance() {
        return coveringData.getGrowthChance();
    }

    public float getSpreadChance() {
        return coveringData.getSpreadChance();
    }

    public int getSpreadLevel() {
        return coveringData.getSpreadLevel();
    }

    public boolean requiresOpenTop() {
        return coveringData.requiresOpenTop();
    }

    public int getMoistureRadius() {
        return coveringData.getMoistureRadius();
    }

    public boolean requiresMoisture() {
        return coveringData.requiresMoisture();
    }

    public String getDropItemName() {
        return coveringData.getDropItemName();
    }

    public boolean hasDrop() {
        return coveringData.hasDrop();
    }
}
