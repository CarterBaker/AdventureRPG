package application.bootstrap.itempipeline.itemdefinition;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class ItemActionStruct extends StructPackage {

    /*
     * One thing that can be done to an item where it stands. It is set off by
     * its trigger while the main hand holds the item it names, or anything at
     * all when it names none, and only on the parts it names when it names
     * any. It may use up one of the held item, and it always turns the item
     * into the item it names in the same place. It may also fire an item from
     * a muzzle, given in model sub-voxels, along an aim in the model's frame
     * at a speed in blocks a second.
     */

    // Trigger
    private final ItemActionTrigger trigger;
    private final String heldItemName;
    private final boolean consumesHeld;
    private final ItemShapeStruct partShape;

    // Result
    private final String becomesItemName;

    // Fire
    private final String fireItemName;
    private final Vector3 muzzle;
    private final Vector3 aim;
    private final float fireSpeed;

    // Constructor \\

    public ItemActionStruct(
            ItemActionTrigger trigger,
            String heldItemName,
            boolean consumesHeld,
            ItemShapeStruct partShape,
            String becomesItemName,
            String fireItemName,
            Vector3 muzzle,
            Vector3 aim,
            float fireSpeed) {

        // Trigger
        this.trigger = trigger;
        this.heldItemName = heldItemName;
        this.consumesHeld = consumesHeld;
        this.partShape = partShape;

        // Result
        this.becomesItemName = becomesItemName;

        // Fire
        this.fireItemName = fireItemName;
        this.muzzle = muzzle;
        this.aim = aim;
        this.fireSpeed = fireSpeed;
    }

    // Match \\

    // Whether the action answers this trigger with this item held, struck on this cell of the item's model grid
    public boolean matches(ItemActionTrigger trigger, String heldName, int gridX, int gridY, int gridZ) {
        return this.trigger == trigger
                && (heldItemName.equals(EngineSetting.ITEM_ACTION_HELD_ANY) || heldItemName.equals(heldName))
                && (partShape == null || partShape.claimsGridCell(gridX, gridY, gridZ));
    }

    // Accessible \\

    public ItemActionTrigger getTrigger() {
        return trigger;
    }

    public String getHeldItemName() {
        return heldItemName;
    }

    public boolean isConsumingHeld() {
        return consumesHeld;
    }

    public String getBecomesItemName() {
        return becomesItemName;
    }

    public boolean fires() {
        return !fireItemName.equals(EngineSetting.ITEM_ACTION_FIRE_NONE);
    }

    public String getFireItemName() {
        return fireItemName;
    }

    public Vector3 getMuzzle() {
        return muzzle;
    }

    public Vector3 getAim() {
        return aim;
    }

    public float getFireSpeed() {
        return fireSpeed;
    }
}
