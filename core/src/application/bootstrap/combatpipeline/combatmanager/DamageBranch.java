package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityInstance;
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
     * blow catches part of it with whatever it is held up — anything blocks,
     * and heavier or armored things block more — and softens the shove.
     * Armor then turns part of the rest aside — each point of it counts for
     * less the more there is — and what gets through comes off the entity's
     * health and shoves it along the blow's horizontal direction. An entity
     * brought to no health is defeated; for now defeat cuts its action short
     * and restores it to full health where it stands.
     */

    // Internal
    private CombatManager combatManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.combatManager = get(CombatManager.class);
    }

    // Damage \\

    void apply(EntityInstance target, float amount, Vector3 direction) {

        if (amount <= 0f)
            return;

        StatisticsHandle statisticsHandle = target.getStatisticsHandle();
        float passed = 1f - resolveGuard(target, direction);
        float armor = Math.max(0f, target.getStat(ItemStat.ARMOR));
        float dealt = amount * passed
                * EngineSetting.ARMOR_MITIGATION_SCALE / (EngineSetting.ARMOR_MITIGATION_SCALE + armor);
        float health = Math.min(statisticsHandle.getHealth(), target.getMaxHealth()) - dealt;

        knockBack(target, direction, passed);

        if (health > 0f) {
            statisticsHandle.setHealth(health);
            return;
        }

        defeat(target);
    }

    // Block \\

    // The share of a blow a raised guard catches — none unless the guard is up and faces where the blow came from
    private float resolveGuard(EntityInstance target, Vector3 direction) {

        if (target.getEntityActionHandle().getAction() != EntityAction.BLOCK
                || !target.getEntityActionHandle().isRaised())
            return 0f;

        ItemInstance guardItem = combatManager.resolveGuardItem(target);

        if (guardItem == null || !isFacing(target, direction))
            return 0f;

        float guard = EngineSetting.BLOCK_MITIGATION_BASE
                + guardItem.getTotalWeight() * EngineSetting.BLOCK_MITIGATION_PER_WEIGHT
                + guardItem.getItemDefinitionHandle().getStat(ItemStat.ARMOR) * EngineSetting.BLOCK_MITIGATION_PER_ARMOR;

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
        target.getStatisticsHandle().setHealth(target.getMaxHealth());
    }
}
