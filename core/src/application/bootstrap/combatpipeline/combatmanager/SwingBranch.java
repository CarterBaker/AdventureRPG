package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.combatpipeline.moveset.AttackStruct;
import application.bootstrap.entitypipeline.entity.CombatDirection;
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
     * Lands a blow for CombatManager at the moment it strikes. Anything that
     * can be held swings, and empty hands punch. The blow reaches from the
     * entity's eye along where it faces and lands on whatever it meets first:
     * an entity takes its damage — the entity's own damage, gear included,
     * plus the held item's heft; a tree takes the hit through TreeManager,
     * which knocks leaves off and decides whether the held tool cuts its
     * wood; and a block takes it through PlacementManager, which decides
     * whether the held tool can break it. A moveset's attack scales the
     * damage and reach by its own, and its blade sweeps an arc as it comes
     * in — across for a blow from either side or straight ahead, up and down
     * for one from above or below — so it meets any entity within the arc;
     * the world is struck straight ahead.
     */

    // Internal
    private CombatManager combatManager;
    private PlacementManager placementManager;
    private TreeManager treeManager;

    // Scratch
    private Vector3 eyeScratch;
    private Vector3 direction;
    private Vector3 sweepAxis;
    private Vector3 sweepDirection;
    private TreeCastStruct treeCast;

    // Sweep
    private float sweepDistance;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.eyeScratch = new Vector3();
        this.direction = new Vector3();
        this.sweepAxis = new Vector3();
        this.sweepDirection = new Vector3();
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

    // A plain swing, straight ahead at the entity's own reach
    void strike(EntityInstance entity) {

        EntityInstance target = reach(
                entity,
                EngineSetting.DEFAULT_ATTACK_REACH,
                EngineSetting.DEFAULT_ATTACK_ARC,
                CombatDirection.NEUTRAL);

        if (target != null)
            combatManager.damage(target, resolveDamage(entity), direction);
    }

    // A moveset's attack from a side
    void strike(EntityInstance entity, AttackStruct attack, CombatDirection side) {

        EntityInstance target = reach(entity, attack.getReach(), attack.getArc(), side);

        if (target != null)
            combatManager.strike(entity, target, resolveDamage(entity) * attack.getDamage(), direction, side);
    }

    // The entity the blow meets before anything in the world, null when the world takes it instead
    private EntityInstance reach(EntityInstance entity, float reachScale, float arc, CombatDirection side) {

        Vector3 eye = entity.getEyePosition(eyeScratch);
        long chunkCoordinate = entity.getWorldPositionStruct().getChunkCoordinate();
        float reach = entity.getStatisticsHandle().getReach() * EngineSetting.REACH_SCALE * reachScale;

        direction.set(entity.getEntityInputHandle().getFacingDirection()).normalize();

        EntityInstance target = sweep(entity, chunkCoordinate, eye, reach, arc, side);
        float blockDistance = placementManager.findBlockDistance(entity, eye, direction);

        treeManager.castTree(entity.getWorldHandle(), chunkCoordinate, eye, direction, reach, treeCast);

        float treeDistance = treeCast.isHit() ? treeCast.getDistance() : Float.MAX_VALUE;

        if (target != null && sweepDistance < Math.min(blockDistance, treeDistance))
            return target;

        if (treeDistance < blockDistance) {
            treeManager.strikeTree(entity, treeCast, direction);
            return null;
        }

        placementManager.strikeBlock(entity, eye, direction);
        return null;
    }

    // Sweep \\

    // The nearest entity any ray across the arc meets, leaving how far it met it in sweepDistance
    private EntityInstance sweep(
            EntityInstance entity,
            long chunkCoordinate,
            Vector3 eye,
            float reach,
            float arc,
            CombatDirection side) {

        sweepDistance = reach;

        if (arc <= 0f)
            return sweepRay(entity, chunkCoordinate, eye, direction, null);

        resolveSweepAxis(side);

        EntityInstance nearest = null;
        float step = (float) Math.toRadians(arc) / (EngineSetting.ATTACK_ARC_RAYS - 1);
        float start = -(float) Math.toRadians(arc) * 0.5f;

        for (int i = 0; i < EngineSetting.ATTACK_ARC_RAYS; i++) {
            rotateAboutSweepAxis(start + step * i);
            nearest = sweepRay(entity, chunkCoordinate, eye, sweepDirection, nearest);
        }

        return nearest;
    }

    private EntityInstance sweepRay(
            EntityInstance entity,
            long chunkCoordinate,
            Vector3 eye,
            Vector3 rayDirection,
            EntityInstance nearest) {

        EntityInstance target = combatManager.findTarget(entity, chunkCoordinate, eye, rayDirection, sweepDistance);

        if (target == null)
            return nearest;

        sweepDistance = combatManager.getTargetDistance();
        return target;
    }

    // A blow from above or below sweeps about the facing's sideways axis, any other about the vertical
    private void resolveSweepAxis(CombatDirection side) {

        if (side != CombatDirection.UP && side != CombatDirection.DOWN) {
            sweepAxis.set(0f, 1f, 0f);
            return;
        }

        float length = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        if (length <= 0f) {
            sweepAxis.set(1f, 0f, 0f);
            return;
        }

        sweepAxis.set(direction.z / length, 0f, -direction.x / length);
    }

    // The facing turned by an angle about the unit sweep axis, by Rodrigues' rotation
    private void rotateAboutSweepAxis(float radians) {

        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float dot = sweepAxis.x * direction.x + sweepAxis.y * direction.y + sweepAxis.z * direction.z;
        float crossX = sweepAxis.y * direction.z - sweepAxis.z * direction.y;
        float crossY = sweepAxis.z * direction.x - sweepAxis.x * direction.z;
        float crossZ = sweepAxis.x * direction.y - sweepAxis.y * direction.x;
        float keep = dot * (1f - cos);

        sweepDirection.set(
                direction.x * cos + crossX * sin + sweepAxis.x * keep,
                direction.y * cos + crossY * sin + sweepAxis.y * keep,
                direction.z * cos + crossZ * sin + sweepAxis.z * keep);
    }

    // Damage \\

    private float resolveDamage(EntityInstance entity) {
        return entity.getStat(ItemStat.DAMAGE)
                + combatManager.resolveHeldWeight(entity) * EngineSetting.SWING_DAMAGE_PER_WEIGHT;
    }
}
