package application.bootstrap.worldpipeline.world;

import engine.root.StructPackage;

public class GlobeChartStruct extends StructPackage {

    /*
     * One grid's flat chart of a globe: the world it charts, the chart cell it
     * is anchored at, the canonical face cell that anchor stands on and the
     * quarter turns from the chart's axes into that face's. Every other chart
     * cell is found by laying its step from the anchor on the anchor face and
     * folding it over the edges it crosses (see GlobeUtility.resolveChartCell).
     * The grid moves the anchor to its active chunk every time that chunk
     * changes, keeping the anchor's place and turns as the chart already had
     * them, so the chart follows the focal entity across the globe without
     * ever renumbering what is loaded; on another world it starts over.
     */

    // Anchor
    private boolean anchored;
    private int worldID;
    private long anchorChunk;
    private long anchorCanonical;
    private int anchorTurns;

    // Anchor \\

    public void anchor(int worldID, long anchorChunk, long anchorCanonical, int anchorTurns) {
        this.anchored = true;
        this.worldID = worldID;
        this.anchorChunk = anchorChunk;
        this.anchorCanonical = anchorCanonical;
        this.anchorTurns = anchorTurns;
    }

    // Accessible \\

    public boolean isAnchored() {
        return anchored;
    }

    public int getWorldID() {
        return worldID;
    }

    public long getAnchorChunk() {
        return anchorChunk;
    }

    public long getAnchorCanonical() {
        return anchorCanonical;
    }

    public int getAnchorTurns() {
        return anchorTurns;
    }
}
