package application.bootstrap.worldpipeline.biome;

import engine.root.StructPackage;

public class BiomeRiverStruct extends StructPackage {

    /*
     * How rivers behave in a biome: how much water its ground sheds into
     * them, how widely they wind across flat ground, how deep they cut below
     * the land and how steep the walls of that cut stand, so a shallow cut
     * with gentle walls is a vale and a deep one with sheer walls a canyon,
     * the width of the flat bank beside the water, how wide and deep the
     * channel runs against its flow, the blocks its bed, banks and cut walls
     * are made of, the chance a reach slips underground and runs as a cave
     * river wherever the land stands high enough over it, and the air such a
     * tunnel keeps over its water.
     */

    private final float runoff;
    private final float meander;
    private final int incisionBlocks;
    private final float wallSlope;
    private final int floodplainBlocks;
    private final float widthScale;
    private final float depthScale;
    private final String bedBlockName;
    private final String bankBlockName;
    private final String wallBlockName;
    private final float underground;
    private final int tunnelHeadroomBlocks;

    public BiomeRiverStruct(
            float runoff,
            float meander,
            int incisionBlocks,
            float wallSlope,
            int floodplainBlocks,
            float widthScale,
            float depthScale,
            String bedBlockName,
            String bankBlockName,
            String wallBlockName,
            float underground,
            int tunnelHeadroomBlocks) {

        this.runoff = runoff;
        this.meander = meander;
        this.incisionBlocks = incisionBlocks;
        this.wallSlope = wallSlope;
        this.floodplainBlocks = floodplainBlocks;
        this.widthScale = widthScale;
        this.depthScale = depthScale;
        this.bedBlockName = bedBlockName;
        this.bankBlockName = bankBlockName;
        this.wallBlockName = wallBlockName;
        this.underground = underground;
        this.tunnelHeadroomBlocks = tunnelHeadroomBlocks;
    }

    // Accessible \\

    public float getRunoff() {
        return runoff;
    }

    public float getMeander() {
        return meander;
    }

    public int getIncisionBlocks() {
        return incisionBlocks;
    }

    public float getWallSlope() {
        return wallSlope;
    }

    public int getFloodplainBlocks() {
        return floodplainBlocks;
    }

    public float getWidthScale() {
        return widthScale;
    }

    public float getDepthScale() {
        return depthScale;
    }

    public String getBedBlockName() {
        return bedBlockName;
    }

    public String getBankBlockName() {
        return bankBlockName;
    }

    public String getWallBlockName() {
        return wallBlockName;
    }

    public float getUnderground() {
        return underground;
    }

    public int getTunnelHeadroomBlocks() {
        return tunnelHeadroomBlocks;
    }
}
