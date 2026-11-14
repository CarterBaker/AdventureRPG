package application.bootstrap.entitypipeline.npcmanager;

import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.entitypipeline.npc.NpcInstance;
import application.bootstrap.entitypipeline.npc.NpcMind;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class NpcWanderBranch extends BranchPackage {

    /*
     * The wandering mind of an NPC for NpcManager. A calm resident stands idle
     * for a while, then walks toward a point drawn within its wander reach of
     * home, in its home chunk's frame, along the way NpcSteerBranch finds it;
     * it stands again once it is near enough, or once the walk has gone on
     * longer than any walk should, as it does when no way leads there.
     * returnHome() walks a resident whose fight is over back to its spot, and
     * calms it there, standing idle before it wanders again; one that cannot
     * find its way home in NPC_RETURN_MAX_SECONDS calms wherever it stands,
     * still wandering about its spot from there. Like every mind,
     * it only writes what the NPC wants into its input.
     */

    // Internal
    private NpcSteerBranch npcSteerBranch;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.npcSteerBranch = get(NpcSteerBranch.class);
    }

    // Wander \\

    void wander(NpcInstance resident, float deltaTime) {

        if (!resident.isWalking()) {

            resident.advanceIdle(deltaTime);

            if (resident.getIdleSeconds() <= 0f)
                beginWalk(resident);

            return;
        }

        resident.advanceWalk(deltaTime);

        Vector3 target = resident.getWanderTarget();
        boolean walking = resident.getWalkSeconds() < EngineSetting.NPC_WALK_MAX_SECONDS
                && npcSteerBranch.steer(
                        resident,
                        resident.getHomeChunkCoordinate(),
                        target.x,
                        target.y,
                        target.z,
                        EngineSetting.NPC_ARRIVE_DISTANCE_BLOCKS,
                        deltaTime);

        if (!walking) {
            resident.beginIdle(drawIdleSeconds());
            return;
        }

        resident.getEntityInstance().getEntityInputHandle().setWalk(true);
    }

    // A point drawn evenly over the disc of the resident's wander reach about home
    private void beginWalk(NpcInstance resident) {

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector3 home = resident.getHomePosition();
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = (float) Math.sqrt(random.nextDouble()) * resident.getNpcHandle().getWanderBlocks();

        resident.beginWalk(
                home.x + (float) Math.cos(angle) * radius,
                home.z + (float) Math.sin(angle) * radius);
    }

    private float drawIdleSeconds() {
        return EngineSetting.NPC_IDLE_MIN_SECONDS + ThreadLocalRandom.current().nextFloat()
                * (EngineSetting.NPC_IDLE_MAX_SECONDS - EngineSetting.NPC_IDLE_MIN_SECONDS);
    }

    // Return \\

    // Back to its spot at an even pace, calmed there — or wherever it stands once the way home has taken too long —
    // and left standing for a while
    void returnHome(NpcInstance resident, float deltaTime) {

        Vector3 home = resident.getHomePosition();

        resident.advanceWalk(deltaTime);

        if (resident.getWalkSeconds() < EngineSetting.NPC_RETURN_MAX_SECONDS && npcSteerBranch.steer(
                resident,
                resident.getHomeChunkCoordinate(),
                home.x,
                home.y,
                home.z,
                EngineSetting.NPC_ARRIVE_DISTANCE_BLOCKS,
                deltaTime))
            return;

        resident.setMind(NpcMind.CALM);
        resident.beginIdle(drawIdleSeconds());
    }
}
