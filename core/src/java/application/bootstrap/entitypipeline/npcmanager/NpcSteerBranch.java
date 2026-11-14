package application.bootstrap.entitypipeline.npcmanager;

import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.npc.NpcInstance;
import application.bootstrap.entitypipeline.pathfinding.PathStruct;
import application.bootstrap.entitypipeline.pathfindingmanager.PathfindingManager;
import application.bootstrap.physicspipeline.util.SubBlockSampleUtility;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

class NpcSteerBranch extends BranchPackage {

    /*
     * The legs of an NPC's minds for NpcManager. steer() walks a resident
     * toward a goal on the ground of a chunk by writing what it wants into its
     * EntityInputHandle exactly as the player's keys would, never moving it
     * itself: it follows the path PathfindingManager found it, waypoint by
     * waypoint, jumping where the next stands a full block or more above its
     * feet, and goes straight for the goal once the path is walked. It
     * searches again once the goal has drifted from where the path was found
     * to lead, once a path that fell short of the goal is walked, or once it
     * has gone too long without coming nearer where it heads, and never more
     * often than PATH_REPATH_SECONDS, so a goal it cannot reach is not searched
     * every frame. Until its first search is granted it heads straight for the
     * goal. resolveOffset() is the one measure of where a point lies from an
     * NPC, across the world's wrap.
     */

    // Internal
    private PathfindingManager pathfindingManager;

    // Scratch
    private Vector3 goalScratch;
    private Vector3 headingScratch;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.goalScratch = new Vector3();
        this.headingScratch = new Vector3();
    }

    @Override
    protected void get() {

        // Internal
        this.pathfindingManager = get(PathfindingManager.class);
    }

    // Steer \\

    // False once the resident stands within arrival distance of the goal, which leaves it standing
    boolean steer(
            NpcInstance resident,
            long goalChunkCoordinate,
            float goalX,
            float goalY,
            float goalZ,
            float arriveDistance,
            float deltaTime) {

        EntityInstance entity = resident.getEntityInstance();
        PathStruct path = resident.getPathStruct();
        Vector3 goal = resolveOffset(entity, goalChunkCoordinate, goalX, goalY, goalZ, goalScratch);

        path.advanceClock(deltaTime);

        if (goal.x * goal.x + goal.z * goal.z <= arriveDistance * arriveDistance)
            return false;

        if (isSearchDue(entity, path, goalChunkCoordinate, goalX, goalZ))
            pathfindingManager.findPath(entity, goalChunkCoordinate, goalX, goalY, goalZ, path);

        Vector3 heading = resolveHeading(entity, path, goal, deltaTime);
        EntityInputHandle input = entity.getEntityInputHandle();

        input.setFacingDirection(heading.x, 0f, heading.z);
        input.setForward(true);

        return true;
    }

    // A walker is searched for again when it has no way yet, has drifted from its goal, has walked a way that fell
    // short of it, or is stuck — never sooner than the pacing allows
    private boolean isSearchDue(
            EntityInstance entity,
            PathStruct path,
            long goalChunkCoordinate,
            float goalX,
            float goalZ) {

        if (!path.isDue())
            return false;

        if (!path.isSearched() || path.isStuck())
            return true;

        if (!path.hasWaypoint() && !path.isComplete())
            return true;

        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                entity.getWorldHandle(),
                path.getFrameChunkCoordinate(),
                goalChunkCoordinate);
        float driftX = Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE + goalX - path.getGoalX();
        float driftZ = Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE + goalZ - path.getGoalZ();
        float drift = EngineSetting.PATH_GOAL_DRIFT_BLOCKS;

        return driftX * driftX + driftZ * driftZ > drift * drift;
    }

    // Where the walker heads this frame — the next waypoint not yet reached, else the goal itself
    private Vector3 resolveHeading(EntityInstance entity, PathStruct path, Vector3 goal, float deltaTime) {

        long frameChunkCoordinate = path.getFrameChunkCoordinate();
        int feetLayer = SubBlockSampleUtility.toSub(
                entity.getWorldPositionStruct().getPosition().y + EngineSetting.PATH_FEET_LIFT_BLOCKS);

        while (path.hasWaypoint()) {

            Vector3 waypoint = resolveOffset(
                    entity,
                    frameChunkCoordinate,
                    path.getWaypointX() + EngineSetting.PATH_COLUMN_CENTER,
                    0f,
                    path.getWaypointZ() + EngineSetting.PATH_COLUMN_CENTER,
                    headingScratch);
            float distance = (float) Math.sqrt(waypoint.x * waypoint.x + waypoint.z * waypoint.z);
            int rise = path.getWaypointLayer() - feetLayer;

            if (distance <= EngineSetting.PATH_WAYPOINT_REACH_BLOCKS && rise < EngineSetting.PATH_JUMP_LAYERS) {
                path.advance();
                continue;
            }

            boolean stuck = path.track(distance, deltaTime);

            if (stuck || (rise >= EngineSetting.PATH_JUMP_LAYERS && distance <= EngineSetting.PATH_JUMP_REACH_BLOCKS))
                entity.getEntityInputHandle().setJump(true);

            return waypoint;
        }

        if (path.track((float) Math.sqrt(goal.x * goal.x + goal.z * goal.z), deltaTime))
            entity.getEntityInputHandle().setJump(true);

        return headingScratch.set(goal);
    }

    // Offset \\

    // From the middle of the NPC's footprint, at its feet, to a point given in a chunk's frame, in blocks
    Vector3 resolveOffset(EntityInstance entity, long chunkCoordinate, float x, float y, float z, Vector3 out) {

        WorldPositionStruct worldPositionStruct = entity.getWorldPositionStruct();
        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                entity.getWorldHandle(),
                chunkCoordinate,
                worldPositionStruct.getChunkCoordinate());
        Vector3 position = worldPositionStruct.getPosition();
        Vector3 size = entity.getSize();

        return out.set(
                x - (Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE + position.x + size.x * 0.5f),
                y - position.y,
                z - (Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE + position.z + size.z * 0.5f));
    }
}
