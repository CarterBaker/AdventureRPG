package application.bootstrap.entitypipeline.npcmanager;

import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.combatpipeline.combatmanager.CombatManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.npc.NpcInstance;
import application.bootstrap.entitypipeline.npc.NpcMind;
import application.bootstrap.entitypipeline.npc.NpcTemperamentStruct;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class NpcPerceptionBranch extends BranchPackage {

    /*
     * The senses and temper of an NPC for NpcManager, settling each frame what
     * it is minded to do. A foe is a being that provokes it — whatever last
     * struck it, or struck one it defends and likes no less than whatever
     * struck it — or one it thinks so little of
     * that its opinion, through ProgressionManager, has fallen to its
     * hostility. It looks about only when its perception comes due, every
     * NPC_PERCEPTION_SECONDS jittered so residents spread across frames, for
     * the nearest such being in its world, still standing, within its sight
     * and seen, a ray from eye to eye meeting no solid block first; a
     * provoker comes before every other. A foe is kept while it stays hostile
     * and within NPC_SIGHT_KEEP_SCALE of its sight, and let go the moment it
     * falls or leaves the world. A resident led further from home than its
     * leash gives the fight up, forgets its provocation and walks home,
     * heeding nothing on the way but a fresh blow. With a foe it fights,
     * unless its health has fallen to its flee share, when it runs instead.
     */

    // Internal
    private CombatManager combatManager;
    private ProgressionManager progressionManager;
    private RaycastManager raycastManager;
    private NpcSteerBranch npcSteerBranch;

    // Scratch
    private Vector3 offsetScratch;
    private Vector3 eyeScratch;
    private Vector3 otherEyeScratch;
    private Vector3 directionScratch;
    private BlockCastStruct blockCast;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.offsetScratch = new Vector3();
        this.eyeScratch = new Vector3();
        this.otherEyeScratch = new Vector3();
        this.directionScratch = new Vector3();
        this.blockCast = new BlockCastStruct();
    }

    @Override
    protected void get() {

        // Internal
        this.combatManager = get(CombatManager.class);
        this.progressionManager = get(ProgressionManager.class);
        this.raycastManager = get(RaycastManager.class);
        this.npcSteerBranch = get(NpcSteerBranch.class);
    }

    // Perceive \\

    void perceive(NpcInstance resident, float deltaTime) {

        resident.advanceSenses(deltaTime);

        if (resident.hasFoe() && !isStanding(resident, resident.getFoe()))
            resident.setFoe(null);

        if (resident.hasFoe() && isBeyondLeash(resident))
            giveUp(resident);

        if (resident.isPerceptionDue()) {
            resident.restPerception(drawPerceptionSeconds());
            look(resident);
        }

        settleMind(resident);
    }

    // The foe kept while it stays hostile and in sight, else the nearest hostile being seen — none while walking home
    private void look(NpcInstance resident) {

        EntityInstance foe = resident.getFoe();
        float sight = resident.getNpcHandle().getTemperament().getSightBlocks();
        float keep = sight * EngineSetting.NPC_SIGHT_KEEP_SCALE;

        if (foe != null && isHostile(resident, foe) && resolveDistanceSquared(resident, foe) <= keep * keep)
            return;

        resident.setFoe(resident.getMind() == NpcMind.RETURN ? null : findFoe(resident, sight));
    }

    private EntityInstance findFoe(NpcInstance resident, float sight) {

        EntityInstance entity = resident.getEntityInstance();
        ObjectArrayList<EntityInstance> combatants = combatManager.getCombatants();
        EntityInstance nearest = null;
        float nearestDistance = sight * sight;
        boolean nearestProvoker = false;

        for (int i = 0; i < combatants.size(); i++) {

            EntityInstance other = combatants.get(i);

            if (other == entity || other.getWorldHandle() != entity.getWorldHandle() || isDefeated(other))
                continue;

            float distance = resolveDistanceSquared(resident, other);
            boolean provoker = resident.isProvokedBy(other);

            if (distance > sight * sight || (nearestProvoker && !provoker))
                continue;

            if (distance >= nearestDistance && provoker == nearestProvoker)
                continue;

            if (!isHostile(resident, other) || (!provoker && !isSeen(entity, other)))
                continue;

            nearest = other;
            nearestDistance = distance;
            nearestProvoker = provoker;
        }

        return nearest;
    }

    // True while the resident is provoked by the other, or thinks no better of it than its hostility
    private boolean isHostile(NpcInstance resident, EntityInstance other) {
        return resident.isProvokedBy(other)
                || progressionManager.getOpinion(resident.getEntityInstance(), other)
                        <= resident.getNpcHandle().getTemperament().getHostility();
    }

    // True when a ray from the resident's eye to the other's meets no solid block on the way
    private boolean isSeen(EntityInstance entity, EntityInstance other) {

        Vector3 eye = entity.getEyePosition(eyeScratch);
        Vector3 otherEye = other.getEyePosition(otherEyeScratch);
        Vector3 offset = npcSteerBranch.resolveOffset(
                entity,
                other.getWorldPositionStruct().getChunkCoordinate(),
                otherEye.x,
                otherEye.y,
                otherEye.z,
                offsetScratch);
        float rise = otherEye.y - eye.y;
        float distance = (float) Math.sqrt(offset.x * offset.x + rise * rise + offset.z * offset.z);

        if (distance <= EngineSetting.DIVISION_EPSILON)
            return true;

        directionScratch.set(offset.x / distance, rise / distance, offset.z / distance);
        raycastManager.castSolidBlock(
                entity.getWorldPositionStruct().getChunkCoordinate(),
                eye,
                directionScratch,
                distance,
                blockCast);

        return !blockCast.isHit() || blockCast.getDistance() >= distance;
    }

    // Standing \\

    // True while the being is still in the world, in the resident's own, and not yet brought down
    private boolean isStanding(NpcInstance resident, EntityInstance other) {
        return other.getWorldHandle() == resident.getEntityInstance().getWorldHandle()
                && combatManager.isCombatant(other)
                && !isDefeated(other);
    }

    private boolean isDefeated(EntityInstance entity) {
        return entity.getStatisticsHandle().isDefeated() || entity.getStatisticsHandle().getHealth() <= 0f;
    }

    // Leash \\

    private boolean isBeyondLeash(NpcInstance resident) {

        Vector3 home = resident.getHomePosition();
        Vector3 offset = npcSteerBranch.resolveOffset(
                resident.getEntityInstance(),
                resident.getHomeChunkCoordinate(),
                home.x,
                home.y,
                home.z,
                offsetScratch);
        float leash = resident.getNpcHandle().getTemperament().getLeashBlocks();

        return offset.x * offset.x + offset.z * offset.z > leash * leash;
    }

    private void giveUp(NpcInstance resident) {
        resident.forgetProvocation();
        resident.setFoe(null);
        resident.setMind(NpcMind.RETURN);
    }

    // Mind \\

    // A foe is fought, or fled once health runs low; a fight ended walks the resident home, and home calms it
    private void settleMind(NpcInstance resident) {

        if (!resident.hasFoe()) {

            if (resident.getMind() == NpcMind.FIGHT || resident.getMind() == NpcMind.FLEE)
                resident.setMind(NpcMind.RETURN);

            return;
        }

        NpcTemperamentStruct temperament = resident.getNpcHandle().getTemperament();
        float health = resident.getEntityInstance().getStatisticsHandle().getHealth();

        resident.setMind(health <= temperament.getFleeHealth() ? NpcMind.FLEE : NpcMind.FIGHT);
    }

    // Provocation \\

    // A blow from a source gives the struck resident cause to fight it, unless it came from a resident it still
    // thinks well of whose quarrel lies with another
    void provoke(NpcInstance resident, EntityInstance source, NpcInstance sourceResident) {

        if (sourceResident != null
                && sourceResident.getFoe() != resident.getEntityInstance()
                && !isHostile(resident, source))
            return;

        take(resident, source);
    }

    // A blow seen struck on a being the resident likes no less than the source — whoever strikes first is to
    // blame — sours it on the source and gives it cause to fight it
    void defend(NpcInstance resident, EntityInstance source, EntityInstance victim, float share) {

        EntityInstance entity = resident.getEntityInstance();

        if (entity == source || entity == victim || !resident.getNpcHandle().getTemperament().isDefends())
            return;

        float alarm = EngineSetting.NPC_ALARM_BLOCKS;

        if (resolveDistanceSquared(resident, victim) > alarm * alarm)
            return;

        if (progressionManager.getOpinion(entity, victim) < progressionManager.getOpinion(entity, source))
            return;

        progressionManager.shiftAffinity(entity, source, -share * EngineSetting.AFFINITY_PER_WOUND
                * EngineSetting.NPC_DEFEND_AFFINITY_SHARE);
        take(resident, source);
    }

    // The source becomes the provoker, and the foe too unless the resident already fights one that provoked it
    private void take(NpcInstance resident, EntityInstance source) {

        boolean engaged = resident.hasFoe() && resident.isProvokedBy(resident.getFoe());

        resident.provoke(source, EngineSetting.NPC_PROVOKED_SECONDS);

        if (engaged)
            return;

        resident.setFoe(source);
        settleMind(resident);
    }

    // Utility \\

    private float resolveDistanceSquared(NpcInstance resident, EntityInstance other) {

        Vector3 position = other.getWorldPositionStruct().getPosition();
        Vector3 size = other.getSize();
        Vector3 offset = npcSteerBranch.resolveOffset(
                resident.getEntityInstance(),
                other.getWorldPositionStruct().getChunkCoordinate(),
                position.x + size.x * 0.5f,
                position.y,
                position.z + size.z * 0.5f,
                offsetScratch);

        return offset.x * offset.x + offset.z * offset.z;
    }

    private float drawPerceptionSeconds() {
        return EngineSetting.NPC_PERCEPTION_SECONDS * (EngineSetting.NPC_PERCEPTION_JITTER_MIN
                + ThreadLocalRandom.current().nextFloat()
                        * (EngineSetting.NPC_PERCEPTION_JITTER_MAX - EngineSetting.NPC_PERCEPTION_JITTER_MIN));
    }
}
