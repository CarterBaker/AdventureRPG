package application.bootstrap.combatpipeline.combat;

import application.bootstrap.combatpipeline.projectile.ProjectileInstance;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class BlowStruct extends StructPackage {

    /*
     * One blow as it lands, swung or thrown alike — the scratch every
     * deliverer fills and hands to CombatManager.land(), never allocated per
     * blow. It carries who struck it, the item it was struck with, null for
     * a body's own weapon, and the projectile that carried it, null for one
     * struck by hand; the mass driven into the target and the velocity it
     * arrives with, whose product is its momentum; the damage it opens with
     * before its momentum adds to it, how much its attack scales the total,
     * the type it lands as and the side it comes from, null for no side in
     * particular. Its momentum decides both how hard it wounds and how much
     * work it does on a block or a tree, and a blow that breaks through
     * spends the share of its momentum the break took.
     */

    // Source
    private EntityInstance source;
    private ItemInstance item;
    private ProjectileInstance projectile;

    // Motion
    private float mass;
    private final Vector3 velocity;
    private final Vector3 direction;
    private float momentum;
    private float spent;

    // Effect
    private float base;
    private float scale;
    private short damageTypeID;
    private CombatDirection side;

    // Constructor \\

    public BlowStruct() {

        // Motion
        this.velocity = new Vector3();
        this.direction = new Vector3();
    }

    // Management \\

    // Starts a blow by a source with an item, or with its own weapon when the item is null, carried by a projectile
    // or struck by hand when that is null
    public void set(
            EntityInstance source,
            ItemInstance item,
            ProjectileInstance projectile,
            float mass,
            Vector3 velocity,
            float base,
            float scale,
            short damageTypeID,
            CombatDirection side) {

        float speed = velocity.length();

        // Source
        this.source = source;
        this.item = item;
        this.projectile = projectile;

        // Motion
        this.mass = mass;
        this.velocity.set(velocity);
        this.momentum = mass * speed;
        this.spent = 0f;

        if (speed > 0f)
            this.direction.set(velocity).divide(speed);
        else
            this.direction.set(0f, 0f, 0f);

        // Effect
        this.base = base;
        this.scale = scale;
        this.damageTypeID = damageTypeID;
        this.side = side;
    }

    // Spends work out of what the blow carries, never more than it has left
    public void spend(float work) {
        spent = Math.min(1f, spent + work / Math.max(EngineSetting.DIVISION_EPSILON, getWork()));
    }

    // Accessible \\

    public EntityInstance getSource() {
        return source;
    }

    public ItemInstance getItem() {
        return item;
    }

    public ProjectileInstance getProjectile() {
        return projectile;
    }

    // True for a blow carried by a projectile, false for one struck by hand
    public boolean isThrown() {
        return projectile != null;
    }

    public float getMass() {
        return mass;
    }

    public Vector3 getVelocity() {
        return velocity;
    }

    public Vector3 getDirection() {
        return direction;
    }

    public float getMomentum() {
        return momentum;
    }

    // The work the blow does on a block or a tree, in points of durability
    public float getWork() {
        return momentum / EngineSetting.BLOW_MOMENTUM_PER_DURABILITY;
    }

    // The share of its momentum it still carries, from 0 to 1
    public float getRemaining() {
        return 1f - spent;
    }

    // What it opens with plus what its momentum drives in, never less than the least a blow does, scaled by its attack
    public float getDamage() {
        return Math.max(EngineSetting.BLOW_DAMAGE_MIN, base + momentum * EngineSetting.BLOW_DAMAGE_PER_MOMENTUM)
                * scale;
    }

    public short getDamageTypeID() {
        return damageTypeID;
    }

    public CombatDirection getSide() {
        return side;
    }
}
