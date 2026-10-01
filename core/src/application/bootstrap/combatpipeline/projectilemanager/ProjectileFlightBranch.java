package application.bootstrap.combatpipeline.projectilemanager;

import application.bootstrap.combatpipeline.combatmanager.CombatManager;
import application.bootstrap.combatpipeline.projectile.ProjectileInstance;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemStat;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.physicspipeline.util.SubBlockSampleUtility;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;

class ProjectileFlightBranch extends BranchPackage {

    /*
     * Flies a projectile one fixed step with real physics: gravity, air drag —
     * far heavier in liquid — and a tumble about its spin axis. Its path is
     * swept against solid sub-blocks and against every entity but its thrower;
     * the first entity in reach takes the item's thrown damage, once, and
     * knocks it back. A block face reflects it with restitution and friction,
     * and a slow enough fall onto the top of a block brings it to rest there.
     * A projectile still airborne past the flight limit rests where it is. Its
     * chunk must stay loaded; while it is not, the projectile waits.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private RaycastManager raycastManager;
    private CombatManager combatManager;

    // Scratch
    private BlockCastStruct castStruct;
    private Vector3 direction;
    private Quaternion spinScratch;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.castStruct = new BlockCastStruct();
        this.direction = new Vector3();
        this.spinScratch = new Quaternion();
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.raycastManager = get(RaycastManager.class);
        this.combatManager = get(CombatManager.class);
    }

    // Step \\

    void step(ProjectileInstance projectile, float timeStep) {

        WorldPositionStruct worldPosition = projectile.getWorldPositionStruct();

        if (worldStreamManager.getChunkInstance(worldPosition.getChunkCoordinate()) == null)
            return;

        projectile.advanceFlightTime(timeStep);

        applyForces(projectile, timeStep);
        spin(projectile, timeStep);
        travel(projectile, timeStep);

        WorldPositionUtility.settleChunk(projectile.getWorldHandle(), worldPosition);

        if (!projectile.isResting() && projectile.getFlightTime() >= EngineSetting.PROJECTILE_MAX_FLIGHT_SECONDS)
            projectile.rest(worldPosition.getPosition().y);
    }

    // Forces \\

    private void applyForces(ProjectileInstance projectile, float timeStep) {

        Vector3 velocity = projectile.getVelocity();
        float drag = isInLiquid(projectile) ? EngineSetting.PROJECTILE_LIQUID_DRAG : EngineSetting.PROJECTILE_AIR_DRAG;

        velocity.y -= EngineSetting.GRAVITY_FORCE * timeStep;
        velocity.multiply((float) Math.exp(-drag * timeStep));
    }

    private boolean isInLiquid(ProjectileInstance projectile) {

        WorldPositionStruct worldPosition = projectile.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        BlockHandle block = SubBlockSampleUtility.getSubBlockAt(
                worldStreamManager,
                blockManager,
                worldPosition.getChunkCoordinate(),
                SubBlockSampleUtility.toSub(position.x),
                SubBlockSampleUtility.toSub(position.y),
                SubBlockSampleUtility.toSub(position.z));

        return block != null && block.getGeometry() == DynamicGeometryType.LIQUID;
    }

    // World-axis spin applied ahead of the current orientation
    private void spin(ProjectileInstance projectile, float timeStep) {

        if (projectile.getSpinRate() == 0f)
            return;

        Vector3 axis = projectile.getSpinAxis();
        Quaternion orientation = projectile.getOrientation();

        spinScratch.setFromAxisAngle(axis.x, axis.y, axis.z, projectile.getSpinRate() * timeStep);
        orientation.set(spinScratch.multiply(orientation)).normalize();
    }

    // Travel \\

    private void travel(ProjectileInstance projectile, float timeStep) {

        WorldPositionStruct worldPosition = projectile.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        Vector3 velocity = projectile.getVelocity();
        float speed = velocity.length();

        if (speed <= 0f)
            return;

        float distance = speed * timeStep;
        direction.set(velocity).divide(speed);

        raycastManager.castSolidBlock(
                worldPosition.getChunkCoordinate(),
                position,
                direction,
                distance,
                castStruct);

        float reach = castStruct.isHit() ? castStruct.getDistance() : distance;

        if (!projectile.hasStruck())
            strikeEntity(projectile, reach, speed);

        if (!castStruct.isHit()) {
            position.add(direction.x * distance, direction.y * distance, direction.z * distance);
            return;
        }

        Direction3Vector face = castStruct.getHitFace();
        float offset = EngineSetting.PROJECTILE_SURFACE_OFFSET;

        position.add(
                direction.x * reach + face.x * offset,
                direction.y * reach + face.y * offset,
                direction.z * reach + face.z * offset);

        bounce(projectile, face, position.y - face.y * offset);
    }

    // Reflects off a face with restitution and friction — or rests, when it drops slowly onto a block's top
    private void bounce(ProjectileInstance projectile, Direction3Vector face, float faceY) {

        Vector3 velocity = projectile.getVelocity();
        float normalSpeed = velocity.x * face.x + velocity.y * face.y + velocity.z * face.z;

        if (normalSpeed >= 0f)
            return;

        if (face == Direction3Vector.UP && -normalSpeed < EngineSetting.PROJECTILE_LAND_SPEED) {
            projectile.rest(faceY);
            return;
        }

        float tangentX = velocity.x - normalSpeed * face.x;
        float tangentY = velocity.y - normalSpeed * face.y;
        float tangentZ = velocity.z - normalSpeed * face.z;
        float friction = EngineSetting.PROJECTILE_BOUNCE_FRICTION;
        float rebound = -normalSpeed * EngineSetting.PROJECTILE_BOUNCE_RESTITUTION;

        velocity.set(
                tangentX * friction + face.x * rebound,
                tangentY * friction + face.y * rebound,
                tangentZ * friction + face.z * rebound);

        projectile.setSpinRate(projectile.getSpinRate() * EngineSetting.PROJECTILE_BOUNCE_SPIN_DAMPING);
    }

    // Strike \\

    private void strikeEntity(ProjectileInstance projectile, float reach, float speed) {

        WorldPositionStruct worldPosition = projectile.getWorldPositionStruct();
        EntityInstance target = combatManager.findTarget(
                projectile.getThrower(),
                worldPosition.getChunkCoordinate(),
                worldPosition.getPosition(),
                direction,
                reach);

        if (target == null)
            return;

        combatManager.damage(target, resolveDamage(projectile, speed), direction);
        projectile.markStruck();

        Vector3 velocity = projectile.getVelocity();
        velocity.set(
                -velocity.x * EngineSetting.PROJECTILE_STRIKE_REBOUND,
                velocity.y,
                -velocity.z * EngineSetting.PROJECTILE_STRIKE_REBOUND);
    }

    // The item's own damage plus the momentum it carries
    private float resolveDamage(ProjectileInstance projectile, float speed) {

        ItemInstance itemInstance = projectile.getItemInstance();
        float momentumDamage = itemInstance.getTotalWeight() * speed * EngineSetting.THROWN_DAMAGE_PER_MOMENTUM;

        return Math.max(
                EngineSetting.THROWN_DAMAGE_MIN,
                itemInstance.getItemDefinitionHandle().getStat(ItemStat.DAMAGE) + momentumDamage);
    }
}
