package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.npcmanager.NpcManager;
import application.bootstrap.entitypipeline.statistics.StatisticsHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemStat;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

class DamageBranch extends BranchPackage {

    /*
     * Applies a hit to an entity for CombatManager. A raised guard facing the
     * blow catches part of it — its moveset's share, more with whatever it is
     * held up with, heavier or armored things adding more — and softens the
     * shove. A blow from no side in particular, as a thrown item lands, meets
     * any such guard; a weapon's blow from a side meets only a guard held
     * toward that side's mirror, and one caught that way throws its attacker
     * into recoil. A blow that gets past every guard staggers a target still
     * winding up its own attack. Armor then turns part of the rest aside —
     * each point of it counts for less the more there is — and what gets
     * through comes off the entity's health and shoves it along the blow's
     * horizontal direction. An entity brought to no health is defeated, which
     * cuts its action short: an NPC falls through NpcManager, scattering what
     * it carried and its loot, while anything else is for now restored to
     * full health where it stands.
     */

    // Internal
    private CombatManager combatManager;
    private AttackBranch attackBranch;
    private GuardBranch guardBranch;
    private NpcManager npcManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.combatManager = get(CombatManager.class);
        this.attackBranch = get(AttackBranch.class);
        this.guardBranch = get(GuardBranch.class);
        this.npcManager = get(NpcManager.class);
    }

    // Damage \\

    void apply(EntityInstance target, float amount, Vector3 direction) {

        if (amount <= 0f)
            return;

        land(target, amount, direction, isGuarding(target, direction) ? resolveGuard(target) : 0f);
    }

    void strike(
            EntityInstance source,
            EntityInstance target,
            float amount,
            Vector3 direction,
            CombatDirection side) {

        boolean caught = isGuarding(target, direction) && guardBranch.catches(target, side);

        if (caught)
            attackBranch.deflect(source);

        if (amount <= 0f)
            return;

        land(target, amount, direction, caught ? resolveGuard(target) : 0f);
    }

    private void land(EntityInstance target, float amount, Vector3 direction, float guarded) {

        StatisticsHandle statisticsHandle = target.getStatisticsHandle();
        float passed = 1f - guarded;
        float armor = Math.max(0f, target.getStat(ItemStat.ARMOR));
        float dealt = amount * passed
                * EngineSetting.ARMOR_MITIGATION_SCALE / (EngineSetting.ARMOR_MITIGATION_SCALE + armor);
        float health = Math.min(statisticsHandle.getHealth(), target.getMaxHealth()) - dealt;

        knockBack(target, direction, passed);

        if (guarded <= 0f)
            attackBranch.interrupt(target);

        if (health > 0f) {
            statisticsHandle.setHealth(health);
            return;
        }

        defeat(target);
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
                    + guardItem.getItemDefinitionHandle().getStat(ItemStat.ARMOR)
                            * EngineSetting.BLOCK_MITIGATION_PER_ARMOR;

        return Math.min(EngineSetting.BLOCK_MITIGATION_MAX, guard);
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
        float speed = EngineSetting.KNOCKBACK_SPEED * passed / length;

        velocity.x += direction.x * speed;
        velocity.y += direction.z * speed;
    }

    // Defeat \\

    private void defeat(EntityInstance target) {

        target.getEntityActionHandle().release();

        if (npcManager.defeat(target))
            return;

        target.getStatisticsHandle().setHealth(target.getMaxHealth());
    }
}
