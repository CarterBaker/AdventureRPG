package application.bootstrap.furnishingpipeline.util;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
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
     * sub-voxels, its "spin" in quarter turns about the vertical, and the
     * "chance" it is furnished at all. Every table is resolved here, so an
     * owner naming an unknown table fails at boot.
     */

    // Slots \\

    public static ObjectArrayList<FurnishingSlotStruct> parseSlots(
            ArpgObjectStruct ownerArpg,
            String key,
            String ownerName,
            FurnishingManager furnishingManager) {

        ObjectArrayList<FurnishingSlotStruct> slots = new ObjectArrayList<>();

        if (!ArpgUtility.hasArray(ownerArpg, key))
            return slots;

        ArpgArrayStruct slotsArpg = ownerArpg.getAsArray(key);

        for (int i = 0; i < slotsArpg.size(); i++)
            slots.add(parseSlot(slotsArpg.get(i).getAsObject(), ownerName, furnishingManager));

        return slots;
    }

    private static FurnishingSlotStruct parseSlot(
            ArpgObjectStruct slotArpg,
            String ownerName,
            FurnishingManager furnishingManager) {

        String furnishingName = ArpgUtility.validateString(slotArpg, "furnishing");
        ArpgArrayStruct cornerArpg = ArpgUtility.validateArray(slotArpg, "corner", EngineSetting.AXIS_COUNT);
        int spin = ArpgUtility.getInt(slotArpg, "spin", 0);
        float chance = ArpgUtility.getFloat(slotArpg, "chance", 1f);

        if (spin < 0 || spin >= EngineSetting.ENCODED_FACE_SPIN_COUNT)
            throwException("\"" + ownerName + "\" furnishes from '" + furnishingName + "' turned " + spin
                    + " quarter turns, outside 0 to " + (EngineSetting.ENCODED_FACE_SPIN_COUNT - 1) + ".");

        if (chance < 0f || chance > 1f)
            throwException("\"" + ownerName + "\" furnishes from '" + furnishingName
                    + "' with a chance outside 0 to 1.");

        return new FurnishingSlotStruct(
                furnishingName,
                furnishingManager.getFurnishingIDFromFurnishingName(furnishingName),
                cornerArpg.get(0).getAsInt(),
                cornerArpg.get(1).getAsInt(),
                cornerArpg.get(2).getAsInt(),
                EngineSetting.DEFAULT_BLOCK_ORIENTATION + spin,
                chance);
    }
}
