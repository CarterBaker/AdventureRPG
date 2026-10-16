package application.bootstrap.itempipeline.item;

import java.util.Arrays;

import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import engine.root.EngineSetting;
import engine.root.InstancePackage;

public class ItemInstance extends InstancePackage {

    /*
     * One real item — worn, held, lying in a container, or standing in the
     * world. Handed out by ItemManager. A container item carries its own
     * ContainerInstance, so a backpack or chest keeps what it holds wherever
     * it goes, and its weight includes everything inside it. A stackable item
     * stands for a whole stack of its kind, up to its stack size; absorb() is
     * the one path that moves count from one stack into another, and a stack
     * absorbed down to nothing is spent and dropped by whoever held it; only
     * items alike in their gear stack. A piece of gear also carries its own
     * level, tier, condition and rolled bonuses, set when it is forged through
     * ProgressionManager; everything else stays at level one, the lowest tier
     * and new. Condition wears from one toward zero, and at zero the gear is
     * old, never broken.
     */

    // Internal
    private ItemDefinitionHandle itemDefinitionHandle;

    // Stack
    private int stackCount;

    // Storage — created only for container items
    private ContainerInstance containerInstance;

    // Gear
    private int level;
    private short tierID;
    private float condition;
    private short[] bonusStatIDs;
    private float[] bonusValues;

    // Constructor \\

    public void constructor(ItemDefinitionHandle itemDefinitionHandle) {

        // Internal
        this.itemDefinitionHandle = itemDefinitionHandle;

        // Stack
        this.stackCount = 1;

        // Gear
        this.level = EngineSetting.LEVEL_START;
        this.tierID = EngineSetting.TIER_NONE;
        this.condition = EngineSetting.CONDITION_NEW;
        this.bonusStatIDs = EngineSetting.GEAR_BONUS_IDS_NONE;
        this.bonusValues = EngineSetting.GEAR_BONUS_VALUES_NONE;

        // Storage
        if (!itemDefinitionHandle.isContainer())
            return;

        this.containerInstance = create(ContainerInstance.class);
        this.containerInstance.constructor(itemDefinitionHandle.getContainerSpace().getSize());
    }

    // Stack \\

    public boolean stacksWith(ItemInstance itemInstance) {
        return itemInstance != this
                && itemDefinitionHandle.isStackable()
                && itemInstance.getItemDefinitionHandle() == itemDefinitionHandle
                && isSameGear(itemInstance);
    }

    // Moves as much of another stack of the same item into this one as fits, returning how many moved
    public int absorb(ItemInstance itemInstance) {

        if (!stacksWith(itemInstance))
            throwException("Item '" + itemInstance.getItemDefinitionHandle().getItemName()
                    + "' cannot stack onto '" + itemDefinitionHandle.getItemName() + "'.");

        int moved = Math.min(getStackRoom(), itemInstance.stackCount);

        stackCount += moved;
        itemInstance.stackCount -= moved;

        return moved;
    }

    public void setStackCount(int stackCount) {

        if (stackCount < 1 || stackCount > itemDefinitionHandle.getStackSize())
            throwException("Item '" + itemDefinitionHandle.getItemName() + "' cannot stack " + stackCount
                    + " — its stack holds 1 to " + itemDefinitionHandle.getStackSize() + ".");

        this.stackCount = stackCount;
    }

    public int getStackCount() {
        return stackCount;
    }

    public int getStackRoom() {
        return itemDefinitionHandle.getStackSize() - stackCount;
    }

    public boolean isSpent() {
        return stackCount == 0;
    }

    // Gear \\

    // Gives this item another's level, tier, condition and bonuses, as a stack split off it carries them
    public void copyGear(ItemInstance itemInstance) {
        forge(itemInstance.level, itemInstance.tierID, itemInstance.bonusStatIDs, itemInstance.bonusValues);
        this.condition = itemInstance.condition;
    }

    private boolean isSameGear(ItemInstance itemInstance) {
        return level == itemInstance.level
                && tierID == itemInstance.tierID
                && condition == itemInstance.condition
                && Arrays.equals(bonusStatIDs, itemInstance.bonusStatIDs)
                && Arrays.equals(bonusValues, itemInstance.bonusValues);
    }

    public void forge(int level, short tierID, short[] bonusStatIDs, float[] bonusValues) {
        this.level = level;
        this.tierID = tierID;
        this.bonusStatIDs = bonusStatIDs;
        this.bonusValues = bonusValues;
    }

    public int getLevel() {
        return level;
    }

    public short getTierID() {
        return tierID;
    }

    public float getCondition() {
        return condition;
    }

    public void setCondition(float condition) {
        this.condition = Math.max(EngineSetting.CONDITION_WORN_OUT, Math.min(EngineSetting.CONDITION_NEW, condition));
    }

    public int getBonusCount() {
        return bonusStatIDs.length;
    }

    public short getBonusStatID(int index) {
        return bonusStatIDs[index];
    }

    public float getBonusValue(int index) {
        return bonusValues[index];
    }

    // Accessible \\

    public ItemDefinitionHandle getItemDefinitionHandle() {
        return itemDefinitionHandle;
    }

    public boolean hasContainer() {
        return containerInstance != null;
    }

    public ContainerInstance getContainerInstance() {
        return containerInstance;
    }

    public float getTotalWeight() {
        return itemDefinitionHandle.getWeight() * stackCount
                + (containerInstance != null ? containerInstance.getContentWeight() : 0f);
    }
}
