package application.bootstrap.progressionpipeline.progressionmanager;

import java.util.Arrays;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.progressionpipeline.stat.StatHandle;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.tiermanager.TierManager;
import application.bootstrap.progressionpipeline.util.PowerUtility;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class GearBranch extends BranchPackage {

    /*
     * Gear for ProgressionManager. forge() gives a new piece of gear its
     * level, rolls its tier and draws its bonuses — distinct stats picked by
     * their bonus weights, each worth part to all of its stat's bonus value —
     * every draw salted from the seed given, so the same seed always forges
     * the same piece. A gear stat is its definition's value plus its bonuses,
     * times its tier's multiplier and its condition's factor; a scaled stat is
     * then read against the wearer's level, worth the growth of every level
     * the gear stands above them and losing it for every level below, which is
     * how outgrown gear falls away. Condition counts in whole percents, and at
     * zero the gear still keeps the condition floor's share of its stats.
     * canWield() asks the being's inventory whether it is high enough to draw
     * on a piece: worn slots refuse what it fails, and hands carry it but draw
     * nothing from it.
     */

    // Internal
    private StatManager statManager;
    private TierManager tierManager;

    // Scratch
    private float[] weightScratch;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.statManager = get(StatManager.class);
        this.tierManager = get(TierManager.class);
    }

    @Override
    protected void awake() {

        // Scratch
        this.weightScratch = new float[statManager.getStatIDCount()];
    }

    // Forge \\

    void forge(ItemInstance itemInstance, int level, float luck, float bias, long seed) {

        if (!itemInstance.getItemDefinitionHandle().isGear())
            return;

        short tierID = tierManager.rollTier(seed, luck, bias);
        int bonusCount = tierManager.getTierHandleFromTierID(tierID).getBonusCount();
        short[] bonusStatIDs = new short[bonusCount];
        float[] bonusValues = new float[bonusCount];
        int rolled = 0;

        prepareBonusWeights();

        for (int i = 0; i < bonusCount; i++) {

            long bonusSeed = BiomeFieldUtility.hashCell(seed ^ EngineSetting.GEAR_BONUS_SALT, i, tierID);
            short statID = pickBonusStat(BiomeFieldUtility.hash01(bonusSeed));

            if (statID == EngineSetting.REGISTRY_RESERVED_ID)
                break;

            float roll = BiomeFieldUtility.hash01(bonusSeed ^ EngineSetting.GEAR_BONUS_VALUE_SALT);
            float share = EngineSetting.GEAR_BONUS_ROLL_MIN + (1f - EngineSetting.GEAR_BONUS_ROLL_MIN) * roll;

            bonusStatIDs[rolled] = statID;
            bonusValues[rolled] = statManager.getStatHandleFromStatID(statID).getBonusValue() * share;
            weightScratch[statID] = 0f;
            rolled++;
        }

        if (rolled == 0)
            itemInstance.forge(
                    Math.max(EngineSetting.LEVEL_START, level), tierID,
                    EngineSetting.GEAR_BONUS_IDS_NONE, EngineSetting.GEAR_BONUS_VALUES_NONE);
        else
            itemInstance.forge(
                    Math.max(EngineSetting.LEVEL_START, level), tierID,
                    Arrays.copyOf(bonusStatIDs, rolled), Arrays.copyOf(bonusValues, rolled));
    }

    private void prepareBonusWeights() {
        for (int statID = EngineSetting.REGISTRY_RESERVED_ID + 1; statID < weightScratch.length; statID++)
            weightScratch[statID] = statManager.getStatHandleFromStatID((short) statID).getBonusWeight();
    }

    // The stat a unit roll lands on by the remaining weights, RESERVED_ID when no weight is left
    private short pickBonusStat(float roll) {

        float total = 0f;

        for (int statID = EngineSetting.REGISTRY_RESERVED_ID + 1; statID < weightScratch.length; statID++)
            total += weightScratch[statID];

        if (total <= 0f)
            return EngineSetting.REGISTRY_RESERVED_ID;

        float pick = roll * total;

        for (int statID = EngineSetting.REGISTRY_RESERVED_ID + 1; statID < weightScratch.length; statID++) {

            pick -= weightScratch[statID];

            if (pick < 0f && weightScratch[statID] > 0f)
                return (short) statID;
        }

        for (int statID = weightScratch.length - 1; statID > EngineSetting.REGISTRY_RESERVED_ID; statID--)
            if (weightScratch[statID] > 0f)
                return (short) statID;

        return EngineSetting.REGISTRY_RESERVED_ID;
    }

    // Stats \\

    // Every stat the wielded gear of a being lends it, added into the totals given, each read at the being's level
    void accumulate(EntityInstance entity, float[] totals) {

        InventoryHandle inventoryHandle = entity.getInventoryHandle();
        int level = entity.getLevel();

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {

            ItemInstance itemInstance = inventoryHandle.getItem(equipmentSlot);

            if (itemInstance == null || !canWield(entity, itemInstance))
                continue;

            ValueMapStruct stats = itemInstance.getItemDefinitionHandle().getStats();

            for (int i = 0; i < stats.size(); i++)
                totals[stats.getID(i)] += scale(itemInstance, stats.getID(i), stats.getValue(i), level);

            for (int i = 0; i < itemInstance.getBonusCount(); i++)
                totals[itemInstance.getBonusStatID(i)] += scale(
                        itemInstance, itemInstance.getBonusStatID(i), itemInstance.getBonusValue(i), level);
        }
    }

    // One stat of one piece of gear read at a level — relative to that level when the stat is scaled
    float resolveItemStat(ItemInstance itemInstance, short statID, int level) {

        float value = itemInstance.getItemDefinitionHandle().getStat(statID);

        for (int i = 0; i < itemInstance.getBonusCount(); i++)
            if (itemInstance.getBonusStatID(i) == statID)
                value += itemInstance.getBonusValue(i);

        return scale(itemInstance, statID, value, level);
    }

    private float scale(ItemInstance itemInstance, short statID, float value, int level) {

        if (value == 0f)
            return 0f;

        StatHandle statHandle = statManager.getStatHandleFromStatID(statID);
        float scaled = value * resolveTierMultiplier(itemInstance) * resolveConditionFactor(itemInstance);

        return statHandle.isScaled() ? scaled * PowerUtility.toFactor(itemInstance.getLevel() - level) : scaled;
    }

    private float resolveTierMultiplier(ItemInstance itemInstance) {

        short tierID = itemInstance.getTierID();

        return tierID == EngineSetting.TIER_NONE ? 1f : tierManager.getTierHandleFromTierID(tierID).getMultiplier();
    }

    float resolveConditionFactor(ItemInstance itemInstance) {
        return EngineSetting.CONDITION_FLOOR
                + (1f - EngineSetting.CONDITION_FLOOR) * toConditionPercent(itemInstance.getCondition())
                        / EngineSetting.PERCENT_MAX;
    }

    // Wield \\

    boolean canWield(EntityInstance entity, ItemInstance itemInstance) {
        return !entity.getInventoryHandle().isAboveWieldLevel(itemInstance);
    }

    // Wear \\

    // Wears a piece of gear down by an amount of its condition — its owner's stats move once a whole percent goes
    void wear(EntityInstance owner, ItemInstance itemInstance, float amount) {

        if (amount <= 0f || !itemInstance.getItemDefinitionHandle().isGear())
            return;

        int before = toConditionPercent(itemInstance.getCondition());

        itemInstance.setCondition(itemInstance.getCondition() - amount);

        if (owner != null && toConditionPercent(itemInstance.getCondition()) != before)
            owner.getInventoryHandle().markChanged();
    }

    // Wears everything a being wears that turns blows aside, by an amount of its condition
    void wearArmor(EntityInstance entity, float amount) {

        InventoryHandle inventoryHandle = entity.getInventoryHandle();
        short armorStatID = statManager.getArmorStatID();

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {

            ItemInstance itemInstance = inventoryHandle.getItem(equipmentSlot);

            if (itemInstance != null && itemInstance.getItemDefinitionHandle().getStat(armorStatID) != 0f)
                wear(entity, itemInstance, amount);
        }
    }

    private int toConditionPercent(float condition) {
        return (int) Math.floor(condition * EngineSetting.PERCENT_MAX);
    }
}
