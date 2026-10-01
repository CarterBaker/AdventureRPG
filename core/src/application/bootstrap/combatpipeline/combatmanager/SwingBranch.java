package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.placementmanager.PlacementManager;
import application.bootstrap.itempipeline.itemdefinition.ItemStat;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class SwingBranch extends BranchPackage {

    /*
     * Lands a swing for CombatManager at the moment it strikes. Anything that
     * can be held swings, and empty hands punch. The swing reaches from the
     * entity's eye along where it faces: the nearest entity in reach takes its
     * damage — the entity's own damage, gear included, plus the held item's
     * heft — unless a block stands closer, in which case the block takes the
     * hit through PlacementManager, which decides whether the held tool can
     * break it.
     */

    // Internal
    private CombatManager combatManager;
    private PlacementManager placementManager;

    // Scratch
    private Vector3 eyeScratch;
    private Vector3 direction;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.eyeScratch = new Vector3();
        this.direction = new Vector3();
    }

    @Override
    protected void get() {

        // Internal
        this.combatManager = get(CombatManager.class);
        this.placementManager = get(PlacementManager.class);
    }

    // Strike \\

    void strike(EntityInstance entity) {

        Vector3 eye = entity.getEyePosition(eyeScratch);
        direction.set(entity.getEntityInputHandle().getFacingDirection()).normalize();

        EntityInstance target = combatManager.findTarget(
                entity,
                entity.getWorldPositionStruct().getChunkCoordinate(),
                eye,
                direction,
                entity.getStatisticsHandle().getReach() * EngineSetting.REACH_SCALE);

        if (target != null && combatManager.getTargetDistance() < placementManager.findBlockDistance(
                entity, eye, direction)) {
            combatManager.damage(target, resolveDamage(entity), direction);
            return;
        }

        placementManager.strikeBlock(entity, eye, direction);
    }

    private float resolveDamage(EntityInstance entity) {
        return entity.getStat(ItemStat.DAMAGE)
                + combatManager.resolveHeldWeight(entity) * EngineSetting.SWING_DAMAGE_PER_WEIGHT;
    }
}
