package application.bootstrap.entitypipeline.pathfinding;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class PathStruct extends StructPackage {

    /*
     * One walker's way through the world, written by PathfindingManager and
     * walked by an NPC's steering: its waypoints, each a block column and the
     * sub-block layer its feet stand on there, measured from the corner of the
     * chunk the search began in; how far along them the walker has come; the
     * goal the search was asked for in that same frame, so a goal that has
     * since moved can be told from one that has not; and whether the way
     * reaches that goal or only comes as near as the search could. It counts
     * how long the walker has gone without coming nearer where it heads, so a
     * way it cannot walk is searched again, and how long since the last
     * search, so searches are paced. Its arrays are sized once and reused.
     */

    // Frame
    private long frameChunkCoordinate;

    // Waypoints
    private final int[] waypointX;
    private final int[] waypointLayer;
    private final int[] waypointZ;
    private int count;
    private int index;

    // Goal
    private float goalX;
    private float goalZ;
    private boolean searched;
    private boolean complete;

    // Progress
    private float closestDistance;
    private float stuckSeconds;
    private boolean stuck;

    // Pacing
    private float searchSeconds;

    // Constructor \\

    public PathStruct() {

        // Waypoints
        this.waypointX = new int[EngineSetting.PATH_WAYPOINT_CAPACITY];
        this.waypointLayer = new int[EngineSetting.PATH_WAYPOINT_CAPACITY];
        this.waypointZ = new int[EngineSetting.PATH_WAYPOINT_CAPACITY];

        clear();
    }

    // Management \\

    // Forgets the way and the goal, ready to be searched at once
    public void clear() {

        count = 0;
        index = 0;
        searched = false;
        complete = false;
        searchSeconds = EngineSetting.PATH_REPATH_SECONDS;
        resetProgress();
    }

    // A fresh search in a chunk's frame toward a goal's column in that frame, its waypoints still to come
    public void begin(long frameChunkCoordinate, float goalX, float goalZ) {

        this.frameChunkCoordinate = frameChunkCoordinate;
        this.goalX = goalX;
        this.goalZ = goalZ;

        count = 0;
        index = 0;
        searched = true;
        complete = false;
        searchSeconds = 0f;
        resetProgress();
    }

    // False once the path is full — the rest of the way is searched for when this much is walked
    public boolean addWaypoint(int x, int layer, int z) {

        if (count >= waypointX.length)
            return false;

        waypointX[count] = x;
        waypointLayer[count] = layer;
        waypointZ[count] = z;
        count++;

        return true;
    }

    public void finish(boolean complete) {
        this.complete = complete;
    }

    public void advance() {
        index++;
        resetProgress();
    }

    // Progress \\

    // Records how far the walker stands from where it heads — true once it has gone too long without coming nearer
    public boolean track(float distance, float deltaTime) {

        if (distance < closestDistance - EngineSetting.PATH_STUCK_PROGRESS_BLOCKS) {
            closestDistance = distance;
            stuckSeconds = 0f;
            return false;
        }

        stuckSeconds += deltaTime;

        if (stuckSeconds < EngineSetting.PATH_STUCK_SECONDS)
            return false;

        stuck = true;
        closestDistance = distance;
        stuckSeconds = 0f;

        return true;
    }

    private void resetProgress() {
        closestDistance = Float.MAX_VALUE;
        stuckSeconds = 0f;
        stuck = false;
    }

    public void advanceClock(float deltaTime) {
        searchSeconds += deltaTime;
    }

    // Accessible \\

    public long getFrameChunkCoordinate() {
        return frameChunkCoordinate;
    }

    public boolean hasWaypoint() {
        return index < count;
    }

    public int getWaypointX() {
        return waypointX[index];
    }

    public int getWaypointLayer() {
        return waypointLayer[index];
    }

    public int getWaypointZ() {
        return waypointZ[index];
    }

    public float getGoalX() {
        return goalX;
    }

    public float getGoalZ() {
        return goalZ;
    }

    public boolean isSearched() {
        return searched;
    }

    // True when the way leads all the way to its goal, not only as near as the search came
    public boolean isComplete() {
        return complete;
    }

    public boolean isStuck() {
        return stuck;
    }

    // True once long enough has passed since the last search for another
    public boolean isDue() {
        return searchSeconds >= EngineSetting.PATH_REPATH_SECONDS;
    }
}
