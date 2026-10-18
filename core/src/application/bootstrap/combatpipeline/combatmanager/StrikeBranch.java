package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.combatpipeline.combat.BlowOutcome;
import application.bootstrap.combatpipeline.combat.BlowStruct;
import application.bootstrap.combatpipeline.weapon.AttackStruct;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.placementmanager.PlacementManager;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.worldpipeline.tree.TreeCastStruct;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class StrikeBranch extends BranchPackage {

    /*
     * Lands a weapon's attack for CombatManager at the moment it strikes —
     * a sword, a pickaxe, a block or bare hands alike. The blow drives the
     * mass of what is wielded, or of the entity's own weapon, at the speed
     * the entity's work power drives it, so a heavier thing hits and works
     * harder; it opens with the entity's own damage stat, gear and skill
     * included, and the attack scales it and names its type. It reaches from
     * the entity's eye along where it faces and lands through
     * CombatManager.land() on whatever it meets first: the attack's blade
     * sweeps an arc as it comes in — across for a blow from either side or
     * straight ahead, up and down for one from above or below — so it meets
     * any entity within the arc, while a tree or a block is struck straight
     * ahead and gives only to the right tool. Landing on an entity practices
     * the skill of whatever was swung and wears it, and a stroke that tells
     * on a tree or a block does the same for the tool.
     */

    // Internal
    private CombatManager combatManager;
    private PlacementManager placementManager;
    private TreeManager treeManager;
    private ProgressionManager progressionManager;
    private StatManager statManager;

    // Scratch
    private BlowStruct blow;
    private Vector3 eyeScratch;
    private Vector3 direction;
    private Vector3 velocity;
    private Vector3 sweepAxis;
    private Vector3 sweepDirection;
    private TreeCastStruct treeCast;
    private BlockCastStruct blockCast;

    // Sweep
    private float sweepDistance;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.blow = new BlowStruct();
        this.eyeScratch = new Vector3();
        this.direction = new Vector3();
        this.velocity = new Vector3();
        this.sweepAxis = new Vector3();
        this.sweepDirection = new Vector3();
        this.treeCast = new TreeCastStruct();
        this.blockCast = new BlockCastStruct();
    }

    @Override
    protected void get() {

        // Internal
        this.combatManager = get(CombatManager.class);
        this.placementManager = get(PlacementManager.class);
        this.treeManager = get(TreeManager.class);
        this.progressionManager = get(ProgressionManager.class);
        this.statManager = get(StatManager.class);
    }

    // Strike \\

    // An attack from a side, landing on the entity in its arc or the tree or block straight ahead, whichever is nearest
    void strike(EntityInstance entity, AttackStruct attack, CombatDirection side) {

        Vector3 eye = entity.getEyePosition(eyeScratch);
        long chunkCoordinate = entity.getWorldPositionStruct().getChunkCoordinate();
        float reach = combatManager.resolvePlainReach(entity) * attack.getReach();

        direction.set(entity.getEntityInputHandle().getFacingDirection()).normalize();
        fill(entity, attack, side);

        EntityInstance target = sweep(entity, chunkCoordinate, eye, reach, attack.getArc(), side);
        float blockDistance = placementManager.castBlock(entity, eye, direction, blockCast)
                ? blockCast.getDistance()
                : Float.MAX_VALUE;

        treeManager.castTree(entity.getWorldHandle(), chunkCoordinate, eye, direction, reach, treeCast);

        float treeDistance = treeCast.isHit() ? treeCast.getDistance() : Float.MAX_VALUE;

        if (target != null && sweepDistance < Math.min(blockDistance, treeDistance)) {
            combatManager.land(blow, target);
            practice(entity, EngineSetting.SKILL_PRACTICE_PER_STRIKE, EngineSetting.WEAPON_WEAR_PER_STRIKE);
            return;
        }

        BlowOutcome outcome = treeDistance < blockDistance
                ? combatManager.land(blow, treeCast)
                : combatManager.land(blow, blockCast);

        if (outcome != BlowOutcome.TURNED)
            practice(entity, EngineSetting.SKILL_PRACTICE_PER_WORK, EngineSetting.TOOL_WEAR_PER_WORK);
    }

    // The blow the entity drives along its facing — what it wields swung as hard as its work power swings that mass
    private void fill(EntityInstance entity, AttackStruct attack, CombatDirection side) {

        float mass = combatManager.resolveBlowMass(entity);
        float speed = combatManager.resolveDriveSpeed(
                mass,
                EngineSetting.SWING_IMPULSE,
                entity.getStat(statManager.getWorkPowerStatID()));

        velocity.set(direction).multiply(speed);
        blow.set(
                entity,
                combatManager.resolveWielded(entity),
                null,
                mass,
                velocity,
                entity.getStat(statManager.getDamageStatID()),
                attack.getDamage(),
                attack.getDamageTypeID(),
                side);
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

    // Practice \\

    // A blow that told trains whatever was swung and wears what was wielded
    private void practice(EntityInstance entity, float practice, float wear) {

        ItemInstance wielded = blow.getItem();

        progressionManager.practiceHeldSkill(entity, practice);

        if (wielded != null)
            progressionManager.wearGear(entity, wielded, wear);
    }
}
