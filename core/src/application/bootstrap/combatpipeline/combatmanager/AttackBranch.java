package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.combatpipeline.weapon.AttackStruct;
import application.bootstrap.combatpipeline.weapon.WeaponHandle;
import application.bootstrap.combatpipeline.weaponmanager.WeaponManager;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityActionHandle;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.soundpipeline.soundcuemanager.SoundCueManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class AttackBranch extends BranchPackage {

    /*
     * Runs weapon attacks for CombatManager. attack() starts the attack the
     * entity's weapon makes when asked for a side — that side, or the one it
     * falls back to when the weapon lacks it — playing the clip the weapon
     * has for the entity's rig, its blow landing as its windup ends,
     * quickened by the entity's attack speed. While one plays, control() reads
     * the entity's input: raising the guard before the blow is committed
     * feints, dropping the attack for the guard, and an attack asked for
     * meanwhile waits in the queue until the recovery is far enough along to
     * cut in, so attacks flow one into the next without waiting each one out.
     * strike() lands the blow through StrikeBranch with the weapon the attack
     * began with. deflect() throws an attacker whose blow a guard turned aside
     * into recoil, and interrupt() staggers an attacker struck before its own
     * blow could land.
     */

    // Internal
    private WeaponManager weaponManager;
    private CombatManager combatManager;
    private StrikeBranch strikeBranch;
    private GuardBranch guardBranch;
    private SoundCueManager soundCueManager;

    // Cues
    private int swingCueID;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.weaponManager = get(WeaponManager.class);
        this.combatManager = get(CombatManager.class);
        this.strikeBranch = get(StrikeBranch.class);
        this.guardBranch = get(GuardBranch.class);
        this.soundCueManager = get(SoundCueManager.class);
    }

    @Override
    protected void awake() {

        // Cues
        this.swingCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SWING);
    }

    // Attack \\

    void attack(EntityInstance entity, CombatDirection direction) {

        short weaponID = combatManager.resolveWeapon(entity);
        WeaponHandle weaponHandle = weaponManager.getWeaponHandleFromWeaponID(weaponID);
        CombatDirection side = weaponHandle.resolveAttackSide(direction);
        AttackStruct attack = weaponHandle.getAttack(side);
        RigHandle rigHandle = entity.getEntityData().getRigHandle();

        entity.getEntityActionHandle().beginMove(
                EntityAction.ATTACK,
                side,
                weaponID,
                weaponHandle.getAttackClip(rigHandle, side),
                attack.getDuration() / combatManager.resolveAttackSpeed(entity),
                attack.getImpact(),
                weaponHandle.getAttackStrike(rigHandle, side));

        soundCueManager.playCueAt(swingCueID, entity.getWorldHandle(), entity.getWorldPositionStruct());
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
        CombatDirection side = actionHandle.getDirection();
        AttackStruct attack = weaponManager.getWeaponHandleFromWeaponID(actionHandle.getWeaponID()).getAttack(side);

        strikeBranch.strike(entity, attack, side);
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
