package application.bootstrap.worldpipeline.settlement;

import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import engine.root.StructPackage;

public class SettlementPlanStruct extends StructPackage {

    /*
     * A planned settlement, immutable once planned: the site it stands on, the
     * layout it lays, and its exits, the far ends of the streets leaving it,
     * each with the height of the street there and the heading it leaves
     * along, where the roads to its neighbours and its trails set out from.
     * Exits are in the layout's own unwrapped blocks.
     */

    // Site
    private final SettlementSiteStruct site;

    // Layout
    private final LayoutPlanStruct layout;

    // Exits
    private final double[] exitX;
    private final double[] exitZ;
    private final float[] exitY;
    private final double[] exitHeadingX;
    private final double[] exitHeadingZ;

    // Constructor \\

    public SettlementPlanStruct(
            SettlementSiteStruct site,
            LayoutPlanStruct layout,
            double[] exitX,
            double[] exitZ,
            float[] exitY,
            double[] exitHeadingX,
            double[] exitHeadingZ) {

        // Site
        this.site = site;

        // Layout
        this.layout = layout;

        // Exits
        this.exitX = exitX;
        this.exitZ = exitZ;
        this.exitY = exitY;
        this.exitHeadingX = exitHeadingX;
        this.exitHeadingZ = exitHeadingZ;
    }

    // Accessible \\

    public SettlementSiteStruct getSite() {
        return site;
    }

    public LayoutPlanStruct getLayout() {
        return layout;
    }

    public int getExitCount() {
        return exitX.length;
    }

    public double getExitX(int index) {
        return exitX[index];
    }

    public double getExitZ(int index) {
        return exitZ[index];
    }

    public float getExitY(int index) {
        return exitY[index];
    }

    public double getExitHeadingX(int index) {
        return exitHeadingX[index];
    }

    public double getExitHeadingZ(int index) {
        return exitHeadingZ[index];
    }
}
