package application.bootstrap.entitypipeline.npcmanager;

import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.combatpipeline.combatmanager.CombatManager;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityActionHandle;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.npc.NpcCombatStruct;
import application.bootstrap.entitypipeline.npc.NpcInstance;
import application.bootstrap.entitypipeline.npc.NpcTemperamentStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class NpcCombatBranch extends BranchPackage {

    /*
     * The fighting mind of an NPC for NpcManager. It writes what the NPC wants
     * into its EntityInputHandle exactly as the player's keys and mouse would
     * — an attack from a side, a guard toward one, a plain swing for anything
     * that is no weapon — and leaves every blow to CombatManager. Beyond reach
     * it chases its foe along a path, running while the gap is wide. Within
     * reach it faces the foe and strafes about it, stepping in when too far
     * for a blow and back when crowded, circling toward a side drawn anew
     * every little while. It watches the foe's hands: an attack it sees coming
     * is answered after its reaction time by a guard raised toward the side
     * it reads the blow from — the attack's mirror, read right at its guard
     * chance — and held a moment past the blow, unless its own attack will
     * land first, which it lets fly instead, since a blow landing on a foe
     * still winding up staggers it. It presses attacks at a pace set by its
     * aggression, each from a side the foe's raised guard does not cover, and
     * at once while the foe reels from a guarded blow or a stagger; an attack
     * may carry into others, each pressed once the last has recovered far
     * enough to chain, or be feinted into a guard just before it commits.
     * Flight runs from the foe for a point NPC_FLEE_BLOCKS beyond, drawing
     * another once there or stuck.
     */

    // Internal
    private CombatManager combatManager;
    private NpcSteerBranch npcSteerBranch;

    // Scratch
    private Vector3 offsetScratch;
    private Vector3 eyeScratch;
    private CombatDirection[] sideScratch;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.offsetScratch = new Vector3();
        this.eyeScratch = new Vector3();
        this.sideScratch = new CombatDirection[CombatDirection.VALUES.length];
    }

    @Override
    protected void get() {

        // Internal
        this.combatManager = get(CombatManager.class);
        this.npcSteerBranch = get(NpcSteerBranch.class);
    }

    // Fight \\

    void fight(NpcInstance resident, float deltaTime) {

        EntityInstance entity = resident.getEntityInstance();
        EntityInstance foe = resident.getFoe();
        NpcCombatStruct combat = resident.getNpcCombatStruct();
        NpcTemperamentStruct temperament = resident.getNpcHandle().getTemperament();
        Vector3 offset = resolveAim(entity, foe);
        float gap = (float) Math.sqrt(offset.x * offset.x + offset.z * offset.z)
                - Math.min(foe.getSize().x, foe.getSize().z) * 0.5f;
        float reach = resolveReach(entity);

        combat.advance(deltaTime);

        if (gap > reach * EngineSetting.NPC_ENGAGE_REACH_SHARE) {
            chase(resident, foe, gap, reach, deltaTime);
            return;
        }

        EntityInputHandle input = entity.getEntityInputHandle();

        input.setFacingDirection(offset.x, offset.y, offset.z);
        input.setStrafe(true);

        footwork(input, combat, gap, reach);

        if (!defend(entity, foe, combat, temperament, gap))
            attack(entity, foe, combat, temperament, gap, reach);
    }

    // From the NPC's eye to the middle of its foe's body — level across, and up or down to the foe's chest
    private Vector3 resolveAim(EntityInstance entity, EntityInstance foe) {

        Vector3 position = foe.getWorldPositionStruct().getPosition();
        Vector3 size = foe.getSize();
        Vector3 offset = npcSteerBranch.resolveOffset(
                entity,
                foe.getWorldPositionStruct().getChunkCoordinate(),
                position.x + size.x * 0.5f,
                position.y + size.y * EngineSetting.NPC_AIM_HEIGHT_SHARE,
                position.z + size.z * 0.5f,
                offsetScratch);

        offset.y += entity.getWorldPositionStruct().getPosition().y - entity.getEyePosition(eyeScratch).y;

        return offset;
    }

    // The shortest reach of a blow from any side, so a blow drawn from whichever side still lands
    private float resolveReach(EntityInstance entity) {

        float reach = Float.MAX_VALUE;

        for (CombatDirection side : CombatDirection.VALUES)
            reach = Math.min(reach, combatManager.getAttackReach(entity, side));

        return reach;
    }

    // Chase \\

    private void chase(NpcInstance resident, EntityInstance foe, float gap, float reach, float deltaTime) {

        Vector3 position = foe.getWorldPositionStruct().getPosition();
        Vector3 size = foe.getSize();
        boolean walking = npcSteerBranch.steer(
                resident,
                foe.getWorldPositionStruct().getChunkCoordinate(),
                position.x + size.x * 0.5f,
                position.y,
                position.z + size.z * 0.5f,
                reach * EngineSetting.NPC_STRIKE_REACH_SHARE,
                deltaTime);

        resident.getEntityInstance().getEntityInputHandle().setSprint(
                walking && gap > EngineSetting.NPC_SPRINT_GAP_BLOCKS);
    }

    // Footwork \\

    // In when too far for a blow, back when crowded, and round the foe toward whichever side was last drawn
    private void footwork(EntityInputHandle input, NpcCombatStruct combat, float gap, float reach) {

        if (gap < reach * EngineSetting.NPC_CROWD_REACH_SHARE)
            input.setBack(true);
        else if (gap > reach * EngineSetting.NPC_STRIKE_REACH_SHARE)
            input.setForward(true);

        if (combat.isStrafeDue())
            combat.strafe(drawStrafeSide(), drawBetween(
                    EngineSetting.NPC_STRAFE_MIN_SECONDS,
                    EngineSetting.NPC_STRAFE_MAX_SECONDS));

        switch (combat.getStrafeSide()) {
            case LEFT -> input.setLeft(true);
            case RIGHT -> input.setRight(true);
            default -> {
            }
        }
    }

    private CombatDirection drawStrafeSide() {

        float draw = ThreadLocalRandom.current().nextFloat();
        float still = EngineSetting.NPC_STRAFE_STILL_CHANCE;

        if (draw < still)
            return CombatDirection.NEUTRAL;

        return draw < still + (1f - still) * 0.5f ? CombatDirection.LEFT : CombatDirection.RIGHT;
    }

    // Defense \\

    // True while the guard is up — raised against an attack seen coming once the reaction has passed, and held a
    // moment past it; it drops the moment the foe reels, to strike
    private boolean defend(
            EntityInstance entity,
            EntityInstance foe,
            NpcCombatStruct combat,
            NpcTemperamentStruct temperament,
            float gap) {

        EntityActionHandle foeAction = foe.getEntityActionHandle();

        if (isReeling(foeAction)) {
            combat.dropGuard();
            return false;
        }

        boolean threatened = foeAction.getAction() == EntityAction.ATTACK
                && foeAction.isWindingUp()
                && gap <= combatManager.getAttackReach(foe, foeAction.getDirection())
                        * EngineSetting.NPC_THREAT_REACH_SCALE;

        if (threatened && foeAction.getSequence() != combat.getWatchedSequence())
            combat.watch(
                    foeAction.getSequence(),
                    drawReaction(temperament),
                    readGuard(temperament, foeAction.getDirection()));

        if (threatened && combat.isReactionDue() && !isTradeWon(entity.getEntityActionHandle(), foeAction))
            combat.linger(EngineSetting.NPC_GUARD_LINGER_SECONDS);

        if (!combat.isLingering())
            return false;

        EntityInputHandle input = entity.getEntityInputHandle();

        input.setBlockAction(true);
        input.setGuardDirection(combat.getGuardDirection());

        return true;
    }

    // The side a guard meets a blow from — the blow's mirror when read right, any other side when not
    private CombatDirection readGuard(NpcTemperamentStruct temperament, CombatDirection attackSide) {

        CombatDirection guard = attackSide.mirror();

        if (ThreadLocalRandom.current().nextFloat() < temperament.getGuardChance())
            return guard;

        return drawSide(guard, null);
    }

    // True when the NPC's own blow lands far enough ahead of the foe's that it is better thrown than guarded
    private boolean isTradeWon(EntityActionHandle ownAction, EntityActionHandle foeAction) {
        return ownAction.getAction() == EntityAction.ATTACK
                && ownAction.isWindingUp()
                && ownAction.getTimeToImpact() + EngineSetting.NPC_TRADE_MARGIN_SECONDS < foeAction.getTimeToImpact();
    }

    private boolean isReeling(EntityActionHandle actionHandle) {
        return actionHandle.getAction() == EntityAction.RECOIL || actionHandle.getAction() == EntityAction.STAGGER;
    }

    // Offense \\

    private void attack(
            EntityInstance entity,
            EntityInstance foe,
            NpcCombatStruct combat,
            NpcTemperamentStruct temperament,
            float gap,
            float reach) {

        EntityActionHandle ownAction = entity.getEntityActionHandle();

        if (ownAction.getAction() == EntityAction.ATTACK) {
            press(entity, foe, combat, gap, reach);
            return;
        }

        boolean opening = isReeling(foe.getEntityActionHandle());

        if (ownAction.isActing() || gap > reach * EngineSetting.NPC_STRIKE_REACH_SHARE)
            return;

        if (combat.isPaused() && !opening)
            return;

        EntityInputHandle input = entity.getEntityInputHandle();
        float pause = drawPause(temperament);

        if (!combatManager.isArmed(entity)) {
            input.setPrimaryAction(true);
            combat.beginExchange(pause, 0, false);
            return;
        }

        input.setAttackAction(true);
        input.setAttackDirection(chooseSide(foe, null));
        combat.beginExchange(
                pause,
                drawCombo(temperament),
                !opening && ThreadLocalRandom.current().nextFloat() < temperament.getFeintChance());
    }

    // An attack under way is feinted into a guard just before it commits, or carried into the next once it has
    // recovered far enough to chain
    private void press(EntityInstance entity, EntityInstance foe, NpcCombatStruct combat, float gap, float reach) {

        EntityActionHandle ownAction = entity.getEntityActionHandle();
        EntityInputHandle input = entity.getEntityInputHandle();

        if (combat.isFeintPlanned()) {

            float timeToImpact = ownAction.getTimeToImpact();

            if (timeToImpact > EngineSetting.ATTACK_COMMIT_SECONDS
                    && timeToImpact <= EngineSetting.NPC_FEINT_LEAD_SECONDS) {
                input.setBlockAction(true);
                input.setGuardDirection(CombatDirection.NEUTRAL);
                combat.feint(EngineSetting.NPC_FEINT_FOLLOW_SECONDS);
            }

            return;
        }

        if (combat.getComboLeft() <= 0 || ownAction.isWindingUp() || ownAction.isQueued())
            return;

        if (ownAction.getRecoveryProgress() < EngineSetting.ATTACK_CHAIN_FRACTION
                || gap > reach * EngineSetting.NPC_STRIKE_REACH_SHARE)
            return;

        input.setAttackAction(true);
        input.setAttackDirection(chooseSide(foe, ownAction.getDirection()));
        combat.spendCombo();
    }

    // A side the foe's raised guard does not cover, other than the one just struck from
    private CombatDirection chooseSide(EntityInstance foe, CombatDirection last) {

        EntityActionHandle foeAction = foe.getEntityActionHandle();
        CombatDirection covered = foeAction.getAction() == EntityAction.BLOCK
                ? foeAction.getDirection().mirror()
                : null;

        return drawSide(covered, last);
    }

    // Any side but the two given, either of which may be null
    private CombatDirection drawSide(CombatDirection first, CombatDirection second) {

        int count = 0;

        for (CombatDirection side : CombatDirection.VALUES)
            if (side != first && side != second)
                sideScratch[count++] = side;

        return sideScratch[ThreadLocalRandom.current().nextInt(count)];
    }

    // Flight \\

    void flee(NpcInstance resident, float deltaTime) {

        NpcCombatStruct combat = resident.getNpcCombatStruct();

        combat.advance(deltaTime);

        if (!combat.isFleeing() || resident.getPathStruct().isStuck())
            beginFlight(resident);

        Vector3 target = combat.getFleeTarget();
        boolean running = npcSteerBranch.steer(
                resident,
                combat.getFleeChunkCoordinate(),
                target.x,
                target.y,
                target.z,
                EngineSetting.NPC_ARRIVE_DISTANCE_BLOCKS,
                deltaTime);

        if (!running) {
            combat.endFlight();
            return;
        }

        resident.getEntityInstance().getEntityInputHandle().setSprint(true);
    }

    // A point straight away from the foe and a little to either side, NPC_FLEE_BLOCKS off, in the NPC's own chunk
    private void beginFlight(NpcInstance resident) {

        EntityInstance entity = resident.getEntityInstance();
        Vector3 away = resolveAim(entity, resident.getFoe());
        double angle = Math.atan2(-away.z, -away.x) + Math.toRadians(drawBetween(
                -EngineSetting.NPC_FLEE_SPREAD_DEGREES,
                EngineSetting.NPC_FLEE_SPREAD_DEGREES));
        Vector3 position = entity.getWorldPositionStruct().getPosition();
        Vector3 size = entity.getSize();
        float distance = EngineSetting.NPC_FLEE_BLOCKS;

        resident.getPathStruct().clear();
        resident.getNpcCombatStruct().beginFlight(
                entity.getWorldPositionStruct().getChunkCoordinate(),
                position.x + size.x * 0.5f + (float) Math.cos(angle) * distance,
                position.y,
                position.z + size.z * 0.5f + (float) Math.sin(angle) * distance);
    }

    // Draws \\

    private float drawReaction(NpcTemperamentStruct temperament) {
        return temperament.getReactionSeconds() * drawBetween(
                EngineSetting.NPC_REACTION_JITTER_MIN,
                EngineSetting.NPC_REACTION_JITTER_MAX);
    }

    // The pause before the next attack, shorter the more aggressive the NPC
    private float drawPause(NpcTemperamentStruct temperament) {
        return drawBetween(EngineSetting.NPC_ATTACK_PAUSE_MIN_SECONDS, EngineSetting.NPC_ATTACK_PAUSE_MAX_SECONDS)
                / temperament.getAggression();
    }

    // How many more attacks an attack carries into, each one more drawn at the combo chance
    private int drawCombo(NpcTemperamentStruct temperament) {

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int combo = 0;

        while (combo < EngineSetting.NPC_COMBO_MAX && random.nextFloat() < temperament.getComboChance())
            combo++;

        return combo;
    }

    private float drawBetween(float min, float max) {
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }
}
