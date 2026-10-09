package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.road.RoadQueryStruct;
import engine.root.AsyncContainerPackage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SettlementPlanAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for planning one settlement or one road between
     * settlements: the centreline being drawn, where a point stands against
     * a road, the streets laid so far in the order lots fill them and the
     * spokes among them, the queue of roles its lots fill, its exits, and
     * the gates cut through its wall.
     */

    // Centreline
    DoubleArrayList pointX;
    DoubleArrayList pointZ;

    // Query
    RoadQueryStruct query;

    // Streets
    ObjectArrayList<RoadPathStruct> streets;
    ObjectArrayList<RoadPathStruct> spokes;

    // Lots
    ObjectArrayList<String> roleQueue;

    // Exits
    DoubleArrayList exitX;
    DoubleArrayList exitZ;
    FloatArrayList exitY;
    DoubleArrayList exitHeadingX;
    DoubleArrayList exitHeadingZ;

    // Gates
    FloatArrayList gateStarts;
    FloatArrayList gateEnds;

    @Override
    protected void create() {
        this.pointX = new DoubleArrayList();
        this.pointZ = new DoubleArrayList();
        this.query = new RoadQueryStruct();
        this.streets = new ObjectArrayList<>();
        this.spokes = new ObjectArrayList<>();
        this.roleQueue = new ObjectArrayList<>();
        this.exitX = new DoubleArrayList();
        this.exitZ = new DoubleArrayList();
        this.exitY = new FloatArrayList();
        this.exitHeadingX = new DoubleArrayList();
        this.exitHeadingZ = new DoubleArrayList();
        this.gateStarts = new FloatArrayList();
        this.gateEnds = new FloatArrayList();
    }

    @Override
    public void reset() {
        pointX.clear();
        pointZ.clear();
        streets.clear();
        spokes.clear();
        roleQueue.clear();
        exitX.clear();
        exitZ.clear();
        exitY.clear();
        exitHeadingX.clear();
        exitHeadingZ.clear();
        gateStarts.clear();
        gateEnds.clear();
    }
}
