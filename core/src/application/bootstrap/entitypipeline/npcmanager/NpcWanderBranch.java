package application.bootstrap.entitypipeline.npcmanager;

import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.npc.NpcInstance;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

class NpcWanderBranch extends BranchPackage {

    /*
     * The wandering mind of an NPC for NpcManager. It writes what the NPC
     * wants into its EntityInputHandle exactly as the player's keys would and
     * never moves it itself. A resident stands idle for a while, then walks
     * toward a point drawn within its wander reach of home, measured across
     * the world's wrap from its home chunk; it stands again once it is near
     * enough, or once the walk has gone on longer than any walk should, as it
     * does when something stands in its way.
     */

    // Scratch
    private Vector3 offsetScratch;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.offsetScratch = new Vector3();
    }

    // Wander \\

    void wander(NpcInstance resident, float deltaTime) {

        EntityInputHandle input = resident.getEntityInstance().getEntityInputHandle();

        input.clear();

        if (!resident.isWalking()) {

            resident.advanceIdle(deltaTime);

            if (resident.getIdleSeconds() <= 0f)
                beginWalk(resident);

            return;
        }

        resident.advanceWalk(deltaTime);

        Vector3 offset = resolveOffset(resident);
        float arrive = EngineSetting.NPC_ARRIVE_DISTANCE_BLOCKS;

        if (offset.x * offset.x + offset.z * offset.z <= arrive * arrive
                || resident.getWalkSeconds() >= EngineSetting.NPC_WALK_MAX_SECONDS) {
            resident.beginIdle(drawIdleSeconds());
            return;
        }

        input.setFacingDirection(offset.x, 0f, offset.z);
        input.setForward(true);
        input.setWalk(true);
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

    // From the middle of the resident's footprint to its target, in blocks, in its home chunk's frame
    private Vector3 resolveOffset(NpcInstance resident) {

        EntityInstance entity = resident.getEntityInstance();
        WorldPositionStruct worldPositionStruct = entity.getWorldPositionStruct();
        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                entity.getWorldHandle(),
                resident.getHomeChunkCoordinate(),
                worldPositionStruct.getChunkCoordinate());
        Vector3 position = worldPositionStruct.getPosition();
        Vector3 size = entity.getSize();
        Vector3 target = resident.getWanderTarget();

        return offsetScratch.set(
                target.x - (Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE + position.x + size.x * 0.5f),
                0f,
                target.z - (Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE + position.z + size.z * 0.5f));
    }
}
