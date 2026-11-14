package application.bootstrap.worldpipeline.road;

import engine.root.HandlePackage;

public class RoadHandle extends HandlePackage {

    /*
     * Persistent road type. Wraps RoadData and delegates all access through
     * it.
     */

    // Internal
    private RoadData roadData;

    // Constructor \\

    public void constructor(RoadData roadData) {
        this.roadData = roadData;
    }

    // Accessible \\

    public RoadData getRoadData() {
        return roadData;
    }

    public String getRoadName() {
        return roadData.getRoadName();
    }

    public short getRoadID() {
        return roadData.getRoadID();
    }

    public int getNameSeed() {
        return roadData.getNameSeed();
    }

    public float getWidthBlocks() {
        return roadData.getWidthBlocks();
    }

    public float getHalfWidthBlocks() {
        return roadData.getHalfWidthBlocks();
    }

    public short[] getSurfaceBlockIDs() {
        return roadData.getSurfaceBlockIDs();
    }

    public float[] getSurfaceCumulativeWeights() {
        return roadData.getSurfaceCumulativeWeights();
    }

    public boolean hasSmoothSlopes() {
        return roadData.hasSmoothSlopes();
    }

    public short getShoulderBlockID() {
        return roadData.getShoulderBlockID();
    }

    public float getShoulderWidthBlocks() {
        return roadData.getShoulderWidthBlocks();
    }

    public boolean hasShoulder() {
        return roadData.hasShoulder();
    }

    public short getBaseBlockID() {
        return roadData.getBaseBlockID();
    }

    public int getClearanceBlocks() {
        return roadData.getClearanceBlocks();
    }

    public float getMaxGrade() {
        return roadData.getMaxGrade();
    }

    public RoadBridgeStruct getBridge() {
        return roadData.getBridge();
    }

    public boolean hasBridge() {
        return roadData.hasBridge();
    }

    public RoadTunnelStruct getTunnel() {
        return roadData.getTunnel();
    }

    public boolean hasTunnel() {
        return roadData.hasTunnel();
    }

    public int getSurfaceColor() {
        return roadData.getSurfaceColor();
    }

    public int getSideColor() {
        return roadData.getSideColor();
    }

    public int getDeckColor() {
        return roadData.getDeckColor();
    }
}
