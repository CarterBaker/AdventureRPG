package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class PlacementLatticeStruct extends StructPackage {

    /*
     * The placement cells a planner rolls its sites in, around one home chunk.
     * On a torus they are the cells StructurePlacementUtility lays, a spacing
     * wide and wrapping with the world, and positions are the world's own. On
     * a globe each cube face holds a whole number of square cells, as near the
     * spacing as fits, laid on the home face's plane and run on past its
     * edges; a cell resolved here is folded onto the face cell it truly is, so
     * it rolls by that cell and draws its anchor and every offset in that
     * face's axes, turned into the home's. Ranges are given and anchors
     * returned in the home's coordinates, the chart its grid holds it in, so a
     * site stands the same however a chart lays it.
     */

    // Home
    private SurfacePointStruct home;
    private WorldHandle worldHandle;
    private boolean globe;

    // Lattice
    private int spacingBlocks;
    private int cellsPerFace;
    private double cellBlocks;

    // Cell
    private int cellX;
    private int cellZ;
    private int keyX;
    private int keyZ;
    private int chartTurns;
    private int faceTurns;
    private final GlobeFrameStruct fold = new GlobeFrameStruct();

    // Home \\

    public void setHome(SurfacePointStruct home) {
        this.home = home;
        this.worldHandle = home.getWorldHandle();
        this.globe = home.isGlobe();
    }

    // Lattice \\

    public void configure(int spacingBlocks) {

        this.spacingBlocks = spacingBlocks;

        if (!globe)
            return;

        this.cellsPerFace = Math.max(1, home.getFaceBlocks() / spacingBlocks);
        this.cellBlocks = (double) home.getFaceBlocks() / cellsPerFace;
    }

    // Every cell whose spacing can reach a block range of the home's coordinates
    public void collectCells(
            long minX,
            long minZ,
            long maxX,
            long maxZ,
            IntArrayList outCellsX,
            IntArrayList outCellsZ) {

        if (!globe) {
            StructurePlacementUtility.collectCells(
                    minX, maxX, worldHandle.getWorldScale().x, spacingBlocks, outCellsX);
            StructurePlacementUtility.collectCells(
                    minZ, maxZ, worldHandle.getWorldScale().y, spacingBlocks, outCellsZ);
            return;
        }

        double firstU = Math.min(home.toFaceU(minX, minZ), home.toFaceU(maxX, maxZ));
        double lastU = Math.max(home.toFaceU(minX, minZ), home.toFaceU(maxX, maxZ));
        double firstV = Math.min(home.toFaceV(minX, minZ), home.toFaceV(maxX, maxZ));
        double lastV = Math.max(home.toFaceV(minX, minZ), home.toFaceV(maxX, maxZ));

        addCellSpan(firstU, lastU, outCellsX);
        addCellSpan(firstV, lastV, outCellsZ);
    }

    private void addCellSpan(double first, double last, IntArrayList outCells) {

        outCells.clear();

        for (int cell = (int) Math.floor(first / cellBlocks); cell <= (int) Math.floor(last / cellBlocks); cell++)
            outCells.add(cell);
    }

    // Resolves one collected cell: the cell it truly is and the turns into its axes
    public void resolveCell(int cellX, int cellZ) {

        this.cellX = cellX;
        this.cellZ = cellZ;

        if (!globe) {
            this.keyX = cellX;
            this.keyZ = cellZ;
            this.chartTurns = 0;
            this.faceTurns = 0;
            return;
        }

        int span = cellsPerFace;

        GlobeUtility.foldPoint(
                span,
                home.getHomeFace(),
                2.0 * cellX + 1.0 - span,
                2.0 * cellZ + 1.0 - span,
                2.0 * home.getHomeU() / cellBlocks - span,
                2.0 * home.getHomeV() / cellBlocks - span,
                home.getHomeTurns(),
                fold);

        this.keyX = (int) GlobeUtility.faceOriginX(fold.getFace(), span)
                + (int) Math.round((fold.getFoldA() - 1.0 + span) * 0.5);
        this.keyZ = (int) Math.round((fold.getFoldB() - 1.0 + span) * 0.5);
        this.chartTurns = fold.getTurns();
        this.faceTurns = chartTurns - home.getHomeTurns();
    }

    // Anchor \\

    // The anchor of the resolved cell, rolled along each of the cell's own axes and held separation blocks clear of
    // its edges, in the home's coordinates
    public long anchorX(float rollX, float rollZ, int separationBlocks) {

        if (!globe)
            return StructurePlacementUtility.computeCellAnchor(cellX, spacingBlocks, separationBlocks, rollX);

        return (long) Math.floor(home.toChartX(anchorU(rollX, rollZ, separationBlocks),
                anchorV(rollX, rollZ, separationBlocks)));
    }

    public long anchorZ(float rollX, float rollZ, int separationBlocks) {

        if (!globe)
            return StructurePlacementUtility.computeCellAnchor(cellZ, spacingBlocks, separationBlocks, rollZ);

        return (long) Math.floor(home.toChartZ(anchorU(rollX, rollZ, separationBlocks),
                anchorV(rollX, rollZ, separationBlocks)));
    }

    private double anchorU(float rollX, float rollZ, int separationBlocks) {
        return (cellX + 0.5) * cellBlocks + GlobeUtility.turnX(
                drawOffset(rollX, separationBlocks), drawOffset(rollZ, separationBlocks), -faceTurns);
    }

    private double anchorV(float rollX, float rollZ, int separationBlocks) {
        return (cellZ + 0.5) * cellBlocks + GlobeUtility.turnZ(
                drawOffset(rollX, separationBlocks), drawOffset(rollZ, separationBlocks), -faceTurns);
    }

    // A rolled place along one of the cell's axes, from the cell's centre
    private double drawOffset(float roll, int separationBlocks) {

        double usable = cellBlocks - separationBlocks;

        return separationBlocks * 0.5 + Math.min(roll * usable, usable - 1.0) - cellBlocks * 0.5;
    }

    // Turns \\

    // An offset drawn in the resolved cell's own axes, turned into the home's coordinates
    public double turnToChartX(double offsetX, double offsetZ) {
        return GlobeUtility.turnX(offsetX, offsetZ, -chartTurns);
    }

    public double turnToChartZ(double offsetX, double offsetZ) {
        return GlobeUtility.turnZ(offsetX, offsetZ, -chartTurns);
    }

    // Accessible \\

    // The cell's key, its own cell on the world, what every roll in it is drawn by
    public int getKeyX() {
        return keyX;
    }

    public int getKeyZ() {
        return keyZ;
    }

    // Quarter turns, each taking +X to +Z, from the home's axes into the cell's own
    public int getChartTurns() {
        return chartTurns;
    }
}
