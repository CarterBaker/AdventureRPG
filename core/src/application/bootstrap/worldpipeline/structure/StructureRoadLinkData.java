package application.bootstrap.worldpipeline.structure;

import engine.root.DataPackage;

public class StructureRoadLinkData extends DataPackage {

    /*
     * Whether and how a placed structure joins the world road network. A
     * connectable placement links to up to maxConnections of its nearest
     * connectable neighbours within maxDistanceBlocks, and a link exists if
     * either end chose the other, so the network never depends on which
     * side evaluated it first. roadName is the ROAD definition the link is
     * built from when this end decides the road style.
     */

    private final String roadName;
    private final int maxDistanceBlocks;
    private final int maxConnections;

    public StructureRoadLinkData(String roadName, int maxDistanceBlocks, int maxConnections) {
        this.roadName = roadName;
        this.maxDistanceBlocks = maxDistanceBlocks;
        this.maxConnections = maxConnections;
    }

    public String getRoadName() {
        return roadName;
    }

    public int getMaxDistanceBlocks() {
        return maxDistanceBlocks;
    }

    public int getMaxConnections() {
        return maxConnections;
    }
}
