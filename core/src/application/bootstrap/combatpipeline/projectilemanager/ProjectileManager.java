package application.bootstrap.combatpipeline.projectilemanager;

import application.bootstrap.combatpipeline.projectile.ProjectileInstance;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ProjectileManager extends ManagerPackage {

    /*
     * Owns every thrown item in flight. launch() is the one path an item
     * leaves a hand to fly. Every fixed step ProjectileFlightBranch moves each
     * projectile under gravity, drag and spin, striking entities and bouncing
     * off blocks until it comes to rest, and ProjectileLandingBranch snaps a
     * resting one back into the world — a block piece as a sub-block, anything
     * else as a world item on the nearest sub-voxel position and orientation.
     * One that cannot settle yet waits where it rests and tries again.
     * ProjectileRenderSystem draws them for every window.
     */

    // Branches
    private ProjectileFlightBranch projectileFlightBranch;
    private ProjectileLandingBranch projectileLandingBranch;

    // Projectiles
    private ObjectArrayList<ProjectileInstance> projectiles;

    // Settings
    private float timeStep;

    // Base \\

    @Override
    protected void create() {

        // Branches
        this.projectileFlightBranch = create(ProjectileFlightBranch.class);
        this.projectileLandingBranch = create(ProjectileLandingBranch.class);
        create(ProjectileRenderSystem.class);

        // Projectiles
        this.projectiles = new ObjectArrayList<>();

        // Settings
        this.timeStep = EngineSetting.FIXED_TIME_STEP;
    }

    // Update \\

    @Override
    protected void fixedUpdate() {

        for (int i = projectiles.size() - 1; i >= 0; i--) {

            ProjectileInstance projectile = projectiles.get(i);

            if (!projectile.isResting())
                projectileFlightBranch.step(projectile, timeStep);

            if (projectile.isResting() && projectileLandingBranch.settle(projectile))
                projectiles.remove(i);
        }
    }

    // Management \\

    public void launch(
            EntityInstance thrower,
            ItemInstance itemInstance,
            Vector3 position,
            Vector3 velocity,
            Vector3 spinAxis,
            float spinRate,
            Quaternion orientation) {

        ProjectileInstance projectile = create(ProjectileInstance.class);
        projectile.constructor(
                thrower,
                itemInstance,
                thrower.getWorldHandle(),
                thrower.getWorldPositionStruct().getChunkCoordinate(),
                position,
                velocity,
                spinAxis,
                spinRate,
                orientation);

        projectiles.add(projectile);
    }

    // Accessible \\

    public ObjectArrayList<ProjectileInstance> getProjectiles() {
        return projectiles;
    }
}
