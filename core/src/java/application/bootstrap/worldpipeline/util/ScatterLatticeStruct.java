package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
import engine.root.StructPackage;

public class ScatterLatticeStruct extends StructPackage {

    /*
     * The lattice of cells a scattered probable biome draws its patches in,
     * seen from one place. On a torus the cells divide the world's extent and
     * wrap with it, and the place is its world position. On a globe each cube
     * face is divided into whole square cells, the place is its position on
     * the face it stands on, and a cell beyond that face's edges is folded
     * onto the face it truly lies on: its hash is that face cell's, and what
     * it draws is laid in that face's own axes and turned into the place's, so
     * a patch is the same patch from either side of an edge. A cell resolved
     * here gives where a point drawn in it stands from the place and in the
     * world's own coordinates, and turns an offset from the place into the
     * cell's own axes.
     */

    // Query
    private boolean globe;
    private double queryX;
    private double queryZ;
    private double worldWidthBlocks;
    private double worldHeightBlocks;
    private int faceBlocks;
    private int queryFace;

    // Lattice
    private int cellCountX;
    private int cellCountZ;
    private double cellWidthBlocks;
    private double cellHeightBlocks;

    // Cell
    private int cellX;
    private int cellZ;
    private long cellKeyX;
    private int cellKeyZ;
    private int cellFace;
    private int cellTurns;
    private final GlobeFrameStruct fold = new GlobeFrameStruct();

    // Query \\

    public void setTorusQuery(double blockX, double blockZ, double worldWidthBlocks, double worldHeightBlocks) {
        this.globe = false;
        this.queryX = blockX;
        this.queryZ = blockZ;
        this.worldWidthBlocks = worldWidthBlocks;
        this.worldHeightBlocks = worldHeightBlocks;
    }

    public void setGlobeQuery(int faceBlocks, int face, double faceU, double faceV) {
        this.globe = true;
        this.faceBlocks = faceBlocks;
        this.queryFace = face;
        this.queryX = faceU;
        this.queryZ = faceV;
    }

    // Lattice \\

    // Cells as wide as the size asked for or a little wider, so a whole number of them fills the world or a face
    void configure(double cellSizeBlocks) {

        if (globe) {
            this.cellCountX = Math.max(1, (int) (faceBlocks / cellSizeBlocks));
            this.cellCountZ = cellCountX;
            this.cellWidthBlocks = (double) faceBlocks / cellCountX;
            this.cellHeightBlocks = cellWidthBlocks;
            return;
        }

        this.cellCountX = Math.max(1, (int) (worldWidthBlocks / cellSizeBlocks));
        this.cellCountZ = Math.max(1, (int) (worldHeightBlocks / cellSizeBlocks));
        this.cellWidthBlocks = worldWidthBlocks / cellCountX;
        this.cellHeightBlocks = worldHeightBlocks / cellCountZ;
    }

    // Resolves a cell of the place's lattice, its indices counted from the place's own frame
    void resolveCell(int cellX, int cellZ) {

        this.cellX = cellX;
        this.cellZ = cellZ;

        if (!globe) {
            this.cellKeyX = Math.floorMod(cellX, cellCountX);
            this.cellKeyZ = Math.floorMod(cellZ, cellCountZ);
            this.cellTurns = 0;
            return;
        }

        int span = cellCountX;

        GlobeUtility.foldPoint(
                span,
                queryFace,
                2.0 * cellX + 1.0 - span,
                2.0 * cellZ + 1.0 - span,
                2.0 * queryX / cellWidthBlocks - span,
                2.0 * queryZ / cellHeightBlocks - span,
                0,
                fold);

        int keyU = (int) Math.round((fold.getFoldA() - 1.0 + span) * 0.5);
        int keyV = (int) Math.round((fold.getFoldB() - 1.0 + span) * 0.5);

        this.cellFace = fold.getFace();
        this.cellKeyX = (long) cellFace * span + keyU;
        this.cellKeyZ = keyV;
        this.cellTurns = fold.getTurns();
    }

    // Cell \\

    // Where a point drawn at fractions of the cell's own axes stands in the place's frame
    double toQueryX(double fractionX, double fractionZ) {

        if (!globe)
            return (cellX + fractionX) * cellWidthBlocks;

        return (cellX + 0.5) * cellWidthBlocks + GlobeUtility.turnX(
                (fractionX - 0.5) * cellWidthBlocks, (fractionZ - 0.5) * cellHeightBlocks, -cellTurns);
    }

    double toQueryZ(double fractionX, double fractionZ) {

        if (!globe)
            return (cellZ + fractionZ) * cellHeightBlocks;

        return (cellZ + 0.5) * cellHeightBlocks + GlobeUtility.turnZ(
                (fractionX - 0.5) * cellWidthBlocks, (fractionZ - 0.5) * cellHeightBlocks, -cellTurns);
    }

    // The same point in the world's own coordinates — on a globe its face packed into x
    double toCanonicalX(double fractionX, double fractionZ) {

        if (!globe)
            return toQueryX(fractionX, fractionZ);

        int keyU = (int) (cellKeyX - (long) cellFace * cellCountX);

        return GlobeUtility.faceOriginX(cellFace, faceBlocks) + (keyU + fractionX) * cellWidthBlocks;
    }

    double toCanonicalZ(double fractionX, double fractionZ) {
        return globe ? (cellKeyZ + fractionZ) * cellHeightBlocks : toQueryZ(fractionX, fractionZ);
    }

    // An offset measured in the place's frame, turned into the cell's own axes
    double toCellX(double offsetX, double offsetZ) {
        return GlobeUtility.turnX(offsetX, offsetZ, cellTurns);
    }

    double toCellZ(double offsetX, double offsetZ) {
        return GlobeUtility.turnZ(offsetX, offsetZ, cellTurns);
    }

    // Accessible \\

    double getQueryX() {
        return queryX;
    }

    double getQueryZ() {
        return queryZ;
    }

    int getCellCountX() {
        return cellCountX;
    }

    int getCellCountZ() {
        return cellCountZ;
    }

    double getCellWidthBlocks() {
        return cellWidthBlocks;
    }

    double getCellHeightBlocks() {
        return cellHeightBlocks;
    }

    long getCellKeyX() {
        return cellKeyX;
    }

    int getCellKeyZ() {
        return cellKeyZ;
    }
}
