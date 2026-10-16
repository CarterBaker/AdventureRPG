package application.bootstrap.combatpipeline.combatmanager;

import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.npcmanager.NpcManager;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.progressionpipeline.damagetype.DamageTypeHandle;
import application.bootstrap.progressionpipeline.damagetypemanager.DamageTypeManager;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.util.PowerUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

class DamageBranch extends BranchPackage {

    /*
     * Applies a hit to an entity for CombatManager. A blow's amount is
     * measured at its source's level and its type decides how it lands. A
     * raised guard facing a blow whose type can be guarded catches part of it
     * — its moveset's share, more with whatever it is held up with, heavier or
     * better-armored things adding more — and softens the shove. A blow from
     * no side in particular, as a thrown item lands, meets any such guard; a
     * weapon's blow from a side meets only a guard held toward that side's
     * mirror, and one caught that way throws its attacker into recoil. A blow
     * that gets past every guard staggers a target still winding up its own
     * attack, and a guard that catches part of one trains and wears whatever
     * it was held up with. The amount then becomes a share of the target's
     * health: raised by the growth of every level the source stands above the
     * target and lowered by every level below, and, for a type armor meets,
     * turned partly aside by the target's armor — read against the source's
     * level too, so a much lower foe can barely break it. A source may land it
     * critically. The share wears the target's armor, sours the target on its
     * source and alarms the NPCs about it through NpcManager, shoves it along
     * the blow's horizontal direction, less the more
     * poise it has, and comes off its health through ProgressionManager,
     * bleeding when its type bleeds. An entity brought to no health is
     * defeated, which cuts its action short and earns its source the kill: an
     * NPC falls through NpcManager, scattering what it carried and its loot,
     * and anything else is marked defeated for its own context to settle.
     */

    // Internal
    private CombatManager combatManager;
    private AttackBranch attackBranch;
    private GuardBranch guardBranch;
    private NpcManager npcManager;
    private ProgressionManager progressionManager;
    private StatManager statManager;
    private DamageTypeManager damageTypeManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.combatManager = get(CombatManager.class);
        this.attackBranch = get(AttackBranch.class);
        this.guardBranch = get(GuardBranch.class);
        this.npcManager = get(NpcManager.class);
        this.progressionManager = get(ProgressionManager.class);
        this.statManager = get(StatManager.class);
        this.damageTypeManager = get(DamageTypeManager.class);
    }

    // Damage \\

    void apply(EntityInstance source, EntityInstance target, float amount, short damageTypeID, Vector3 direction) {

        DamageTypeHandle damageType = damageTypeManager.getDamageTypeHandleFromDamageTypeID(damageTypeID);

        if (amount <= 0f || target.getStatisticsHandle().isDefeated())
            return;

        boolean guarded = damageType.isGuardable() && isGuarding(target, direction);

        land(source, target, amount, damageType, direction, guarded ? resolveGuard(target) : 0f);
    }

    void strike(
            EntityInstance source,
            EntityInstance target,
            float amount,
            short damageTypeID,
            Vector3 direction,
            CombatDirection side) {

        DamageTypeHandle damageType = damageTypeManager.getDamageTypeHandleFromDamageTypeID(damageTypeID);

        if (target.getStatisticsHandle().isDefeated())
            return;

        boolean caught = damageType.isGuardable()
                && isGuarding(target, direction)
                && guardBranch.catches(target, side);

        if (caught)
            attackBranch.deflect(source);

        if (amount <= 0f)
            return;

        land(source, target, amount, damageType, direction, caught ? resolveGuard(target) : 0f);
    }

    private void land(
            EntityInstance source,
            EntityInstance target,
            float amount,
            DamageTypeHandle damageType,
            Vector3 direction,
            float guarded) {

        int sourceLevel = source != null ? source.getLevel() : target.getLevel();
        float passed = 1f - guarded;
        float share = amount * PowerUtility.toFactor(sourceLevel - target.getLevel())
                / Math.max(EngineSetting.DIVISION_EPSILON, target.getStat(statManager.getHealthStatID()))
                * passed;

        if (damageType.isArmored()) {
            share *= resolveMitigation(target, sourceLevel);
            progressionManager.wearArmor(target, share * EngineSetting.ARMOR_WEAR_PER_WOUND);
        }

        if (source != null && isCritical(source))
            share *= EngineSetting.CRIT_DAMAGE_MULTIPLIER;

        knockBack(target, direction, passed);

        if (guarded > 0f)
            practiceGuard(target);
        else
            attackBranch.interrupt(target);

        if (source != null) {
            progressionManager.shiftAffinity(target, source, -share * EngineSetting.AFFINITY_PER_WOUND);
            npcManager.alarm(source, target, share);
        }

        if (progressionManager.wound(target, share, damageType.isBleeding()))
            defeat(source, target);
    }

    // The share of a blow that gets past the target's armor — its armor read against the source's level, each point
    // counting for less the more there is
    private float resolveMitigation(EntityInstance target, int sourceLevel) {

        float armor = Math.max(0f, target.getStat(statManager.getArmorStatID()))
                * PowerUtility.toFactor(target.getLevel() - sourceLevel);

        return EngineSetting.ARMOR_MITIGATION_SCALE / (EngineSetting.ARMOR_MITIGATION_SCALE + armor);
    }

    private boolean isCritical(EntityInstance source) {
        return ThreadLocalRandom.current().nextFloat() < source.getStat(statManager.getCritChanceStatID());
    }

    // Block \\

    // True when the target's guard is up and faces where the blow came from
    private boolean isGuarding(EntityInstance target, Vector3 direction) {

        if (target.getEntityActionHandle().getAction() != EntityAction.BLOCK
                || !target.getEntityActionHandle().isRaised())
            return false;

        return isFacing(target, direction);
    }

    // The share of a blow a raised guard catches — its moveset's own, plus the heft and armor it is held up with
    private float resolveGuard(EntityInstance target) {

        ItemInstance guardItem = combatManager.resolveGuardItem(target);
        float guard = guardBranch.resolveCatch(target);

        if (guardItem != null)
            guard += guardItem.getTotalWeight() * EngineSetting.BLOCK_MITIGATION_PER_WEIGHT
                    + progressionManager.getItemStat(guardItem, statManager.getArmorStatID(), target.getLevel())
                            * EngineSetting.BLOCK_MITIGATION_PER_ARMOR;

        return Math.min(EngineSetting.BLOCK_MITIGATION_MAX, guard);
    }

    // A guard that caught a blow trains what it was held up with, bare hands their own skill, and wears it
    private void practiceGuard(EntityInstance target) {

        ItemInstance guardItem = combatManager.resolveGuardItem(target);

        if (guardItem == null) {
            progressionManager.practiceHeldSkill(target, EngineSetting.SKILL_PRACTICE_PER_GUARD);
            return;
        }

        progressionManager.practiceSkill(
                target, guardItem.getItemDefinitionHandle().getSkillID(), EngineSetting.SKILL_PRACTICE_PER_GUARD);
        progressionManager.wearGear(target, guardItem, EngineSetting.WEAPON_WEAR_PER_STRIKE);
    }

    // True when a blow travelling along a direction meets the target from the front
    private boolean isFacing(EntityInstance target, Vector3 direction) {

        Vector3 facing = target.getEntityInputHandle().getFacingDirection();
        float facingLength = (float) Math.sqrt(facing.x * facing.x + facing.z * facing.z);
        float blowLength = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        if (facingLength <= 0f || blowLength <= 0f)
            return true;

        float alignment = -(facing.x * direction.x + facing.z * direction.z) / (facingLength * blowLength);

        return alignment >= EngineSetting.BLOCK_ARC_COSINE;
    }

    // Knock Back \\

    private void knockBack(EntityInstance target, Vector3 direction, float passed) {

        float length = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        if (length <= 0f)
            return;

        Vector2 velocity = target.getEntityStateHandle().getHorizontalVelocity();
        float poise = Math.max(EngineSetting.DIVISION_EPSILON, target.getStat(statManager.getPoiseStatID()));
        float speed = EngineSetting.KNOCKBACK_SPEED * passed / (poise * length);

        velocity.x += direction.x * speed;
        velocity.y += direction.z * speed;
    }

    // Defeat \\

    private void defeat(EntityInstance source, EntityInstance target) {

        target.getEntityActionHandle().release();

        if (source != null)
            progressionManager.recordKill(source, target);

        if (npcManager.defeat(target, source))
            return;

        target.getStatisticsHandle().setDefeated(true);
    }
}
