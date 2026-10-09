package application.bootstrap.worldpipeline.tree;

import engine.root.StructPackage;

public class TreeTrunkStruct extends StructPackage {

    /*
     * A species' trunk at maturity: the range of heights a grown tree reaches,
     * its radius at the ground, how much it narrows to its top and swells into
     * a root flare at its foot, how far up the trunk runs as a single leader
     * before the crown takes over, how far it leans and meanders, and how
     * many stems rise from one root and how far apart.
     */

    private final float minHeightBlocks;
    private final float maxHeightBlocks;
    private final float radiusBlocks;
    private final float taper;
    private final float flare;
    private final float leader;
    private final float leanDegrees;
    private final float wobble;
    private final int stems;
    private final float stemSpreadBlocks;

    public TreeTrunkStruct(
            float minHeightBlocks,
            float maxHeightBlocks,
            float radiusBlocks,
            float taper,
            float flare,
            float leader,
            float leanDegrees,
            float wobble,
            int stems,
            float stemSpreadBlocks) {

        this.minHeightBlocks = minHeightBlocks;
        this.maxHeightBlocks = maxHeightBlocks;
        this.radiusBlocks = radiusBlocks;
        this.taper = taper;
        this.flare = flare;
        this.leader = leader;
        this.leanDegrees = leanDegrees;
        this.wobble = wobble;
        this.stems = stems;
        this.stemSpreadBlocks = stemSpreadBlocks;
    }

    // Accessible \\

    public float getMinHeightBlocks() {
        return minHeightBlocks;
    }

    public float getMaxHeightBlocks() {
        return maxHeightBlocks;
    }

    public float getRadiusBlocks() {
        return radiusBlocks;
    }

    public float getTaper() {
        return taper;
    }

    public float getFlare() {
        return flare;
    }

    public float getLeader() {
        return leader;
    }

    public float getLeanDegrees() {
        return leanDegrees;
    }

    public float getWobble() {
        return wobble;
    }

    public int getStems() {
        return stems;
    }

    public float getStemSpreadBlocks() {
        return stemSpreadBlocks;
    }
}
