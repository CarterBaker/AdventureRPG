package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.placementmanager.PlacementManager;
import application.bootstrap.itempipeline.itemdefinition.ItemStat;
import application.bootstrap.worldpipeline.tree.TreeCastStruct;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class SwingBranch extends BranchPackage {

    /*
     * Lands a swing for CombatManager at the moment it strikes. Anything that
     * can be held swings, and empty hands punch. The swing reaches from the
     * entity's eye along where it faces and lands on whatever it meets first:
     * an entity takes its damage — the entity's own damage, gear included,
     * plus the held item's heft; a tree takes the hit through TreeManager,
     * which knocks leaves off and decides whether the held tool cuts its
     * wood; and a block takes it through PlacementManager, which decides
     * whether the held tool can break it.
     */

    // Internal
    private CombatManager combatManager;
    private PlacementManager placementManager;
    private TreeManager treeManager;

    // Scratch
    private Vector3 eyeScratch;
    private Vector3 direction;
    private TreeCastStruct treeCast;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.eyeScratch = new Vector3();
        this.direction = new Vector3();
        this.treeCast = new TreeCastStruct();
    }

    @Override
    protected void get() {

        // Internal
        this.combatManager = get(CombatManager.class);
        this.placementManager = get(PlacementManager.class);
        this.treeManager = get(TreeManager.class);
    }

    // Strike \\

    void strike(EntityInstance entity) {

        Vector3 eye = entity.getEyePosition(eyeScratch);
        long chunkCoordinate = entity.getWorldPositionStruct().getChunkCoordinate();
        float reach = entity.getStatisticsHandle().getReach() * EngineSetting.REACH_SCALE;

        direction.set(entity.getEntityInputHandle().getFacingDirection()).normalize();

        EntityInstance target = combatManager.findTarget(entity, chunkCoordinate, eye, direction, reach);
        float blockDistance = placementManager.findBlockDistance(entity, eye, direction);

        treeManager.castTree(entity.getWorldHandle(), chunkCoordinate, eye, direction, reach, treeCast);

        float treeDistance = treeCast.isHit() ? treeCast.getDistance() : Float.MAX_VALUE;

        if (target != null && combatManager.getTargetDistance() < Math.min(blockDistance, treeDistance)) {
            combatManager.damage(target, resolveDamage(entity), direction);
            return;
        }

        if (treeDistance < blockDistance) {
            treeManager.strikeTree(entity, treeCast, direction);
            return;
        }

        placementManager.strikeBlock(entity, eye, direction);
    }

    private float resolveDamage(EntityInstance entity) {
        return entity.getStat(ItemStat.DAMAGE)
                + combatManager.resolveHeldWeight(entity) * EngineSetting.SWING_DAMAGE_PER_WEIGHT;
    }
}
