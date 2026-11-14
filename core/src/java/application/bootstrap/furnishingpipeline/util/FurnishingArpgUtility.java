package application.bootstrap.furnishingpipeline.util;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
import application.bootstrap.lootpipeline.loottablemanager.LootTableManager;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class FurnishingArpgUtility extends EngineUtility {

    /*
     * The one reader of a furnished owner's places, shared by every builder
     * whose content comes furnished. Each place names the "furnishing" table
     * it draws from, the "corner" of the item's model grid in the owner's
     * sub-voxels, its "spin" in quarter turns about the vertical, the
     * "chance" it is furnished at all, and the optional "loot" table a
     * container drawn there is found filled from. Every table is resolved
     * here, so an owner naming an unknown table fails at boot. createSlot()
     * is the one way a place is made, so a place drawn at the drafting table
     * is checked exactly as one read from a file.
     */

    // Slots \\

    public static ObjectArrayList<FurnishingSlotStruct> parseSlots(
            ArpgObjectStruct ownerArpg,
            String key,
            String ownerName,
            FurnishingManager furnishingManager,
            LootTableManager lootTableManager) {

        ObjectArrayList<FurnishingSlotStruct> slots = new ObjectArrayList<>();

        if (!ArpgUtility.hasArray(ownerArpg, key))
            return slots;

        ArpgArrayStruct slotsArpg = ownerArpg.getAsArray(key);

        for (int i = 0; i < slotsArpg.size(); i++)
            slots.add(parseSlot(slotsArpg.get(i).getAsObject(), ownerName, furnishingManager, lootTableManager));

        return slots;
    }

    private static FurnishingSlotStruct parseSlot(
            ArpgObjectStruct slotArpg,
            String ownerName,
            FurnishingManager furnishingManager,
            LootTableManager lootTableManager) {

        ArpgArrayStruct cornerArpg = ArpgUtility.validateArray(slotArpg, "corner", EngineSetting.AXIS_COUNT);

        return createSlot(
                ownerName,
                ArpgUtility.validateString(slotArpg, "furnishing"),
                cornerArpg.get(0).getAsInt(),
                cornerArpg.get(1).getAsInt(),
                cornerArpg.get(2).getAsInt(),
                ArpgUtility.getInt(slotArpg, "spin", 0),
                ArpgUtility.getFloat(slotArpg, "chance", 1f),
                ArpgUtility.getString(slotArpg, "loot", EngineSetting.LOOT_TABLE_NAME_NONE),
                furnishingManager,
                lootTableManager);
    }

    // The one way a furnished place is made, read from a file or drawn at the drafting table
    public static FurnishingSlotStruct createSlot(
            String ownerName,
            String furnishingName,
            int cornerX,
            int cornerY,
            int cornerZ,
            int spin,
            float chance,
            String lootTableName,
            FurnishingManager furnishingManager,
            LootTableManager lootTableManager) {

        if (spin < 0 || spin >= EngineSetting.ENCODED_FACE_SPIN_COUNT)
            throwException("\"" + ownerName + "\" furnishes from '" + furnishingName + "' turned " + spin
                    + " quarter turns, outside 0 to " + (EngineSetting.ENCODED_FACE_SPIN_COUNT - 1) + ".");

        if (chance < 0f || chance > 1f)
            throwException("\"" + ownerName + "\" furnishes from '" + furnishingName
                    + "' with a chance outside 0 to 1.");

        return new FurnishingSlotStruct(
                furnishingName,
                furnishingManager.getFurnishingIDFromFurnishingName(furnishingName),
                cornerX,
                cornerY,
                cornerZ,
                EngineSetting.DEFAULT_BLOCK_ORIENTATION + spin,
                chance,
                lootTableName,
                lootTableName.isEmpty()
                        ? EngineSetting.LOOT_TABLE_NONE
                        : lootTableManager.getLootTableIDFromLootTableName(lootTableName));
    }
}
