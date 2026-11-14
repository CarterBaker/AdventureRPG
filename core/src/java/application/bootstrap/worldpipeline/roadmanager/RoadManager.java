package application.bootstrap.worldpipeline.roadmanager;

import application.bootstrap.worldpipeline.road.RoadHandle;
import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RoadManager extends ManagerPackage {

    /*
     * Owns the road palette, every type of road the world can lay and what
     * each is made of, and is the one way a road is planned against the land
     * or laid into a chunk. Planning samples the terrain along a centreline
     * and settles the road's heights and its bridges and tunnels once, so
     * every chunk it crosses lays the same road; laying writes only the
     * chunk's own columns. Every road is resolved in awake(), before any
     * chunk generates. Road IDs are assigned in registration order.
     */

    // Internal
    private RoadPlanBranch roadPlanBranch;
    private RoadStampBranch roadStampBranch;

    // Palette
    private Object2IntOpenHashMap<String> roadName2RoadID;
    private ObjectArrayList<RoadHandle> roadID2RoadHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.roadName2RoadID = RegistryUtility.createNameIndex();
        this.roadID2RoadHandle = RegistryUtility.createPalette();

        create(RoadLoader.class);
        this.roadPlanBranch = create(RoadPlanBranch.class);
        this.roadStampBranch = create(RoadStampBranch.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Management \\

    short registerRoadName(String roadName) {
        return (short) RegistryUtility.registerID(
                roadName2RoadID, roadID2RoadHandle, roadName, EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addRoadHandle(RoadHandle roadHandle) {

        short roadID = roadHandle.getRoadID();

        if (roadID2RoadHandle.get(roadID) != null)
            throwException("Duplicate road name: '" + roadHandle.getRoadName() + "' was registered more than once");

        roadID2RoadHandle.set(roadID, roadHandle);
    }

    // On-Demand \\

    public void request(String roadName) {
        ((RoadLoader) internalLoader).request(roadName);
    }

    // Plan \\

    // Worker — a centreline settled against the land: its heights, bridges and tunnels, either end pinned to a
    // height unless given ROAD_HEIGHT_FREE
    public RoadPathStruct planPath(
            WorldHandle worldHandle,
            RoadHandle roadHandle,
            DoubleArrayList pointX,
            DoubleArrayList pointZ,
            float startY,
            float endY) {
        return roadPlanBranch.plan(worldHandle, roadHandle, pointX, pointZ, startY, endY);
    }

    // Stamp \\

    // Worker — the road's share of a chunk
    public void stampPath(
            WorldHandle worldHandle,
            long chunkCoordinate,
            SubChunkInstance[] subChunks,
            RoadPathStruct path) {
        roadStampBranch.stamp(worldHandle, chunkCoordinate, subChunks, path);
    }

    // Accessible \\

    public boolean hasRoad(String roadName) {
        return RegistryUtility.getHandle(roadName2RoadID, roadID2RoadHandle, roadName) != null;
    }

    public RoadHandle getRoadHandleFromRoadName(String roadName) {

        RoadHandle handle = RegistryUtility.getHandle(roadName2RoadID, roadID2RoadHandle, roadName);

        if (handle == null) {
            request(roadName);
            handle = RegistryUtility.getHandle(roadName2RoadID, roadID2RoadHandle, roadName);
        }

        if (handle == null)
            throwException("Road \"" + roadName + "\" was not registered after its on-demand load "
                    + "completed — check for a resource-name/path mismatch in the road directory.");

        return handle;
    }

    public RoadHandle getRoadHandleFromRoadID(short roadID) {

        RoadHandle handle = RegistryUtility.getHandle(roadID2RoadHandle, roadID);

        if (handle == null)
            throwException("No handle registered for road ID: " + roadID);

        return handle;
    }
}
