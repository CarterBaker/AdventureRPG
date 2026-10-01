package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.combatpipeline.projectilemanager.ProjectileManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.entitypipeline.placementmanager.PlacementManager;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemStat;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

class ThrowBranch extends BranchPackage {

    /*
     * Lets a held item fly for CombatManager at the moment a throw releases.
     * A stack throws one at a time, split off through the inventory. The
     * throw is an impulse: the item leaves at a speed set by the thrower's
     * strength over the item's mass plus the arm's own, adds the thrower's
     * running speed, turns end over end about the throw's horizontal axis —
     * slower the heavier it is — and is handed to ProjectileManager just in
     * front of the eye, short of any block in the way.
     */

    // Internal
    private PlacementManager placementManager;
    private ProjectileManager projectileManager;

    // Scratch
    private Vector3 eyeScratch;
    private Vector3 direction;
    private Vector3 velocity;
    private Vector3 spinAxis;
    private Quaternion orientation;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.eyeScratch = new Vector3();
        this.direction = new Vector3();
        this.velocity = new Vector3();
        this.spinAxis = new Vector3();
        this.orientation = new Quaternion();
    }

    @Override
    protected void get() {

        // Internal
        this.placementManager = get(PlacementManager.class);
        this.projectileManager = get(ProjectileManager.class);
    }

    // Release \\

    void release(EntityInstance entity) {

        InventoryHandle inventoryHandle = entity.getInventoryHandle();

        if (!inventoryHandle.hasMainHand())
            return;

        ItemInstance itemInstance = inventoryHandle.takeOne(EquipmentSlot.MAIN_HAND);
        Vector3 eye = entity.getEyePosition(eyeScratch);
        direction.set(entity.getEntityInputHandle().getFacingDirection()).normalize();

        resolveVelocity(entity, itemInstance);
        resolveSpinAxis();
        orientation.setFromAxisAngle(0f, 1f, 0f, (float) Math.atan2(direction.x, direction.z));

        float clearance = placementManager.findBlockDistance(entity, eye, direction)
                - EngineSetting.THROW_RELEASE_MARGIN;
        float releaseDistance = Math.max(0f, Math.min(EngineSetting.THROW_RELEASE_DISTANCE, clearance));

        eye.add(direction.x * releaseDistance, direction.y * releaseDistance, direction.z * releaseDistance);

        projectileManager.launch(
                entity,
                itemInstance,
                eye,
                velocity,
                spinAxis,
                EngineSetting.THROW_SPIN_RATE / (1f + itemInstance.getTotalWeight()),
                orientation);
    }

    // Impulse over mass, plus the thrower's own run
    private void resolveVelocity(EntityInstance entity, ItemInstance itemInstance) {

        float strength = Math.max(
                EngineSetting.THROW_STRENGTH_MIN_FACTOR,
                1f + (entity.getStat(ItemStat.STRENGTH) - EngineSetting.DEFAULT_ATTRIBUTE_VALUE)
                        * EngineSetting.THROW_STRENGTH_SCALE);
        float speed = EngineSetting.THROW_IMPULSE * strength
                / (itemInstance.getTotalWeight() + EngineSetting.THROW_ARM_MASS);
        Vector2 run = entity.getEntityStateHandle().getHorizontalVelocity();

        velocity.set(direction.x * speed + run.x, direction.y * speed, direction.z * speed + run.y);
    }

    // The throw's horizontal axis, so the item turns end over end away from the thrower
    private void resolveSpinAxis() {

        float length = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        if (length <= 0f) {
            spinAxis.set(1f, 0f, 0f);
            return;
        }

        spinAxis.set(direction.z / length, 0f, -direction.x / length);
    }
}
