package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.combatpipeline.moveset.AttackStruct;
import application.bootstrap.combatpipeline.movesetmanager.MovesetManager;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityActionHandle;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class AttackBranch extends BranchPackage {

    /*
     * Runs moveset attacks for CombatManager. attack() starts the attack the
     * entity's moveset makes from a side, its blow landing as its windup ends,
     * quickened by the entity's attack speed. While one plays, control() reads
     * the entity's input: raising the guard before the blow is committed
     * feints, dropping the attack for the guard, and an attack asked for
     * meanwhile waits in the queue until the recovery is far enough along to
     * cut in, so attacks flow one into the next without waiting each one out.
     * strike() lands the blow through SwingBranch with the moveset the attack
     * began with. deflect() throws an attacker whose blow a guard turned aside
     * into recoil, and interrupt() staggers an attacker struck before its own
     * blow could land.
     */

    // Internal
    private MovesetManager movesetManager;
    private CombatManager combatManager;
    private SwingBranch swingBranch;
    private GuardBranch guardBranch;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.movesetManager = get(MovesetManager.class);
        this.combatManager = get(CombatManager.class);
        this.swingBranch = get(SwingBranch.class);
        this.guardBranch = get(GuardBranch.class);
    }

    // Attack \\

    // False when the entity holds something that is no weapon — that swings plainly instead
    boolean attack(EntityInstance entity, CombatDirection direction) {

        short movesetID = combatManager.resolveAttackMoveset(entity);

        if (movesetID == EngineSetting.MOVESET_NONE)
            return false;

        AttackStruct attack = movesetManager.getMovesetHandleFromMovesetID(movesetID).getAttack(direction);

        entity.getEntityActionHandle().beginMove(
                EntityAction.ATTACK,
                direction,
                movesetID,
                attack.getClip(),
                attack.getDuration() / combatManager.resolveAttackSpeed(entity),
                attack.getImpact(),
                attack.getStrike());
        return true;
    }

    // Control \\

    void control(EntityInstance entity, EntityInputHandle input) {

        EntityActionHandle actionHandle = entity.getEntityActionHandle();

        if (input.isBlockAction() && isFeintable(actionHandle)) {
            actionHandle.release();
            guardBranch.raise(entity, input.getGuardDirection());
            return;
        }

        queue(entity, input);

        if (actionHandle.isQueued() && actionHandle.getRecoveryProgress() >= EngineSetting.ATTACK_CHAIN_FRACTION)
            attack(entity, actionHandle.getQueuedDirection());
    }

    // An attack asked for while the hands are busy waits for them
    void queue(EntityInstance entity, EntityInputHandle input) {

        if (input.isAttackAction())
            entity.getEntityActionHandle().queue(input.getAttackDirection());
    }

    // A windup can be dropped until the last moment before its blow lands
    private boolean isFeintable(EntityActionHandle actionHandle) {
        return actionHandle.isWindingUp() && actionHandle.getTimeToImpact() > EngineSetting.ATTACK_COMMIT_SECONDS;
    }

    // Strike \\

    void strike(EntityInstance entity) {

        EntityActionHandle actionHandle = entity.getEntityActionHandle();
        CombatDirection direction = actionHandle.getDirection();
        AttackStruct attack = movesetManager.getMovesetHandleFromMovesetID(actionHandle.getMovesetID())
                .getAttack(direction);

        swingBranch.strike(entity, attack, direction);
    }

    // Reaction \\

    void deflect(EntityInstance attacker) {
        attacker.getEntityActionHandle().begin(
                EntityAction.RECOIL,
                EngineSetting.RECOIL_SECONDS,
                EngineSetting.REACTION_IMPACT);
    }

    void interrupt(EntityInstance target) {

        EntityActionHandle actionHandle = target.getEntityActionHandle();

        if (actionHandle.getAction() != EntityAction.ATTACK || !actionHandle.isWindingUp())
            return;

        actionHandle.begin(EntityAction.STAGGER, EngineSetting.STAGGER_SECONDS, EngineSetting.REACTION_IMPACT);
    }
}
