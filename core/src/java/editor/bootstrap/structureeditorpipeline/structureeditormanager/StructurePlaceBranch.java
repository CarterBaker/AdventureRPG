package editor.bootstrap.structureeditorpipeline.structureeditormanager;

import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Coordinate3Long;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StructurePlaceBranch extends BranchPackage {

    /*
     * Makes, finds and takes away the furniture and NPCs a layer places, as
     * the structure format holds them: a furnishing names its table, the
     * "corner" of its item's model grid in sub-voxels from the origin, its
     * "spin" in quarter turns and the "loot" a container drawn there is
     * found filled with; a spawn names its "npc" and the block it stands in
     * as an "offset" from the origin. Both are placed by the block they stand
     * in, measured from the structure's corner, so the editor finds them by
     * the cell the pointer strikes. Entries are never changed in place.
     */

    // Make \\

    // A furnishing standing in a cell, turned and carrying a loot table where one is chosen
    ArpgObjectStruct createFurnishing(String table, String lootTable, int spin, long cell, int[] origin) {

        ArpgObjectStruct entry = new ArpgObjectStruct();
        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        entry.addProperty("furnishing", table);
        entry.add("corner", toVector(
                (Coordinate3Long.unpackX(cell) - origin[0]) * resolution,
                (Coordinate3Long.unpackY(cell) - origin[1]) * resolution,
                (Coordinate3Long.unpackZ(cell) - origin[2]) * resolution));

        if (spin != 0)
            entry.addProperty("spin", (long) spin);

        if (!lootTable.isEmpty())
            entry.addProperty("loot", lootTable);

        return entry;
    }

    // An NPC living in a cell
    ArpgObjectStruct createSpawn(String npc, long cell, int[] origin) {

        ArpgObjectStruct entry = new ArpgObjectStruct();

        entry.addProperty("npc", npc);
        entry.add("offset", toVector(
                Coordinate3Long.unpackX(cell) - origin[0],
                Coordinate3Long.unpackY(cell) - origin[1],
                Coordinate3Long.unpackZ(cell) - origin[2]));

        return entry;
    }

    private ArpgArrayStruct toVector(int x, int y, int z) {

        ArpgArrayStruct vector = new ArpgArrayStruct();
        vector.add((long) x);
        vector.add((long) y);
        vector.add((long) z);

        return vector;
    }

    // Find \\

    // The block an entry stands in, from the structure's corner: a furnishing by its corner, a spawn by its offset;
    // Long.MIN_VALUE for one standing below the corner or naming no place
    long resolveCell(ArpgObjectStruct entry, int[] origin) {

        boolean furnishing = entry.has("corner");
        String key = furnishing ? "corner" : "offset";

        if (!ArpgUtility.hasArray(entry, key) || entry.getAsArray(key).size() != EngineSetting.AXIS_COUNT)
            return Long.MIN_VALUE;

        ArpgArrayStruct vector = entry.getAsArray(key);
        int scale = furnishing ? EngineSetting.SUB_VOXEL_RESOLUTION : 1;
        int x = Math.floorDiv(vector.get(0).getAsInt(), scale) + origin[0];
        int y = Math.floorDiv(vector.get(1).getAsInt(), scale) + origin[1];
        int z = Math.floorDiv(vector.get(2).getAsInt(), scale) + origin[2];

        return x < 0 || y < 0 || z < 0 ? Long.MIN_VALUE : Coordinate3Long.pack(x, y, z);
    }

    // The first entry of a list standing in a cell, null when none does
    ArpgObjectStruct find(ObjectArrayList<ArpgObjectStruct> entries, long cell, int[] origin) {

        for (int i = 0; i < entries.size(); i++)
            if (resolveCell(entries.get(i), origin) == cell)
                return entries.get(i);

        return null;
    }

    // Every entry of a list standing in a cell taken away — false when none stood there
    boolean remove(ObjectArrayList<ArpgObjectStruct> entries, long cell, int[] origin) {

        boolean removed = false;

        for (int i = entries.size() - 1; i >= 0; i--)
            if (resolveCell(entries.get(i), origin) == cell) {
                entries.remove(i);
                removed = true;
            }

        return removed;
    }
}
