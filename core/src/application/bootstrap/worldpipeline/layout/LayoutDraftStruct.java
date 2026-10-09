package application.bootstrap.worldpipeline.layout;

import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayoutDraftStruct extends StructPackage {

    /*
     * A layout while it is being planned: the world and reference column it
     * is measured around, the roads, walls and lots laid so far, and every
     * centreline a lot must keep clear of, each with how close a lot may come
     * to it. LayoutManager fits lots against it and seals it into an
     * immutable LayoutPlanStruct once planning ends.
     */

    // World
    private final WorldHandle worldHandle;
    private final double referenceX;
    private final double referenceZ;

    // Contents
    private final ObjectArrayList<RoadPathStruct> paths;
    private final ObjectArrayList<LayoutWallStruct> walls;
    private final ObjectArrayList<LayoutLotStruct> lots;

    // Obstacles — centrelines lots keep clear of, each with its clearance in blocks
    private final ObjectArrayList<RoadPathStruct> obstacles;
    private final DoubleArrayList obstacleClearances;

    // Constructor \\

    public LayoutDraftStruct(WorldHandle worldHandle, double referenceX, double referenceZ) {

        // World
        this.worldHandle = worldHandle;
        this.referenceX = referenceX;
        this.referenceZ = referenceZ;

        // Contents
        this.paths = new ObjectArrayList<>();
        this.walls = new ObjectArrayList<>();
        this.lots = new ObjectArrayList<>();

        // Obstacles
        this.obstacles = new ObjectArrayList<>();
        this.obstacleClearances = new DoubleArrayList();
    }

    // Management \\

    public void addPath(RoadPathStruct path) {
        paths.add(path);
    }

    public void addWall(LayoutWallStruct wall) {
        walls.add(wall);
    }

    public void addLot(LayoutLotStruct lot) {
        lots.add(lot);
    }

    public void addObstacle(RoadPathStruct centerline, double clearanceBlocks) {
        obstacles.add(centerline);
        obstacleClearances.add(clearanceBlocks);
    }

    // Accessible \\

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public double getReferenceX() {
        return referenceX;
    }

    public double getReferenceZ() {
        return referenceZ;
    }

    public ObjectArrayList<RoadPathStruct> getPaths() {
        return paths;
    }

    public ObjectArrayList<LayoutWallStruct> getWalls() {
        return walls;
    }

    public ObjectArrayList<LayoutLotStruct> getLots() {
        return lots;
    }

    public int getObstacleCount() {
        return obstacles.size();
    }

    public RoadPathStruct getObstacle(int index) {
        return obstacles.get(index);
    }

    public double getObstacleClearance(int index) {
        return obstacleClearances.getDouble(index);
    }
}
