package editor.bootstrap.structureeditorpipeline.structureeditormanager;

import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Coordinate3Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StructureDocumentBranch extends BranchPackage {

    /*
     * Reads a structure's entry into the cells the Structure Editor paints
     * and writes them back, the one place the editor speaks the structure
     * format. Every block the "palette", "layers" and "blocks" lay is one
     * cell, keyed by its position as authored before the origin is taken
     * away, holding the block entry it is laid with (block, slot, shape,
     * facing, spin, covering, coverage, content); a later entry overwrites an
     * earlier one at the same cell, as StructureBuilder reads them, and a
     * block entry laid below zero is dropped, as no layer can lay it. Written
     * back, the cells become one palette keyed by single characters, every
     * entry the structure already keyed keeping its key, layers row by row
     * from the corner and the origin given; "blocks" is folded into the
     * layers. Cell entries are never changed in place: an edit lays a new
     * one.
     */

    // Read \\

    // Every cell a structure entry lays, its earlier cells cleared, and its origin
    void read(ArpgObjectStruct structureArpg, Long2ObjectOpenHashMap<ArpgObjectStruct> cells, int[] origin) {

        cells.clear();

        int[] authored = readVector(structureArpg, "origin");

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
            origin[axis] = authored != null ? authored[axis] : 0;

        Object2ObjectOpenHashMap<String, ArpgObjectStruct> key2Entry = readPalette(structureArpg);

        if (ArpgUtility.hasArray(structureArpg, "layers"))
            for (ArpgElementStruct layer : structureArpg.getAsArray("layers"))
                readLayer(layer.getAsObject(), key2Entry, cells);

        if (ArpgUtility.hasArray(structureArpg, "blocks"))
            for (ArpgElementStruct block : structureArpg.getAsArray("blocks"))
                readBlock(block.getAsObject(), key2Entry, cells);
    }

    private Object2ObjectOpenHashMap<String, ArpgObjectStruct> readPalette(ArpgObjectStruct structureArpg) {

        Object2ObjectOpenHashMap<String, ArpgObjectStruct> key2Entry = new Object2ObjectOpenHashMap<>();

        if (!ArpgUtility.hasObject(structureArpg, "palette"))
            return key2Entry;

        ArpgObjectStruct paletteArpg = structureArpg.getAsObject("palette");

        for (String key : paletteArpg.keySet())
            if (ArpgUtility.hasObject(paletteArpg, key))
                key2Entry.put(key, paletteArpg.getAsObject(key).deepCopy());

        return key2Entry;
    }

    private void readLayer(
            ArpgObjectStruct layerArpg,
            Object2ObjectOpenHashMap<String, ArpgObjectStruct> key2Entry,
            Long2ObjectOpenHashMap<ArpgObjectStruct> cells) {

        if (!ArpgUtility.hasArray(layerArpg, "rows"))
            return;

        int baseY = ArpgUtility.getInt(layerArpg, "y", 0);
        int height = ArpgUtility.getInt(layerArpg, "height", 1);
        ArpgArrayStruct rows = layerArpg.getAsArray("rows");

        for (int y = baseY; y < baseY + height; y++)
            for (int z = 0; z < rows.size(); z++)
                readRow(rows.get(z).getAsString(), y, z, key2Entry, cells);
    }

    private void readRow(
            String row,
            int y,
            int z,
            Object2ObjectOpenHashMap<String, ArpgObjectStruct> key2Entry,
            Long2ObjectOpenHashMap<ArpgObjectStruct> cells) {

        for (int x = 0; x < row.length(); x++) {

            ArpgObjectStruct entry = key2Entry.get(String.valueOf(row.charAt(x)));

            if (entry != null)
                cells.put(Coordinate3Long.pack(x, y, z), entry);
        }
    }

    // A block entry laid at one position or through an inclusive box, by palette key or named inline
    private void readBlock(
            ArpgObjectStruct blockArpg,
            Object2ObjectOpenHashMap<String, ArpgObjectStruct> key2Entry,
            Long2ObjectOpenHashMap<ArpgObjectStruct> cells) {

        ArpgObjectStruct entry = blockArpg.has("palette")
                ? key2Entry.get(blockArpg.get("palette").getAsString())
                : toCellEntry(blockArpg);

        if (entry == null)
            return;

        int[] from = readVector(blockArpg, blockArpg.has("position") ? "position" : "from");
        int[] to = readVector(blockArpg, blockArpg.has("position") ? "position" : "to");

        if (from == null || to == null || Math.min(from[0], to[0]) < 0 || Math.min(from[1], to[1]) < 0
                || Math.min(from[2], to[2]) < 0)
            return;

        for (int y = Math.min(from[1], to[1]); y <= Math.max(from[1], to[1]); y++)
            for (int z = Math.min(from[2], to[2]); z <= Math.max(from[2], to[2]); z++)
                for (int x = Math.min(from[0], to[0]); x <= Math.max(from[0], to[0]); x++)
                    cells.put(Coordinate3Long.pack(x, y, z), entry);
    }

    // An inline block entry without where it is laid, so it reads as a palette entry does
    private ArpgObjectStruct toCellEntry(ArpgObjectStruct blockArpg) {

        ArpgObjectStruct entry = blockArpg.deepCopy();

        entry.remove("position");
        entry.remove("from");
        entry.remove("to");

        return entry;
    }

    private int[] readVector(ArpgObjectStruct arpg, String key) {

        if (!ArpgUtility.hasArray(arpg, key) || arpg.getAsArray(key).size() != EngineSetting.AXIS_COUNT)
            return null;

        ArpgArrayStruct vector = arpg.getAsArray(key);

        return new int[] { vector.get(0).getAsInt(), vector.get(1).getAsInt(), vector.get(2).getAsInt() };
    }

    // Write \\

    // The cells and origin written into a structure entry as one palette, its layers and its origin — false, the
    // entry untouched, when they hold more distinct block entries than palette keys exist
    boolean write(ArpgObjectStruct structureArpg, Long2ObjectOpenHashMap<ArpgObjectStruct> cells, int[] origin) {

        Object2ObjectOpenHashMap<String, String> text2Key = new Object2ObjectOpenHashMap<>();
        ArpgObjectStruct paletteArpg = new ArpgObjectStruct();

        if (!keyEntries(structureArpg, cells, text2Key, paletteArpg))
            return false;

        writeOrigin(structureArpg, origin);
        structureArpg.add("palette", paletteArpg);
        structureArpg.add("layers", writeLayers(cells, text2Key, resolveBounds(cells)));
        structureArpg.remove("blocks");

        return true;
    }

    // Every distinct entry the cells lay given a key, keeping each key the entry already gave it
    private boolean keyEntries(
            ArpgObjectStruct structureArpg,
            Long2ObjectOpenHashMap<ArpgObjectStruct> cells,
            Object2ObjectOpenHashMap<String, String> text2Key,
            ArpgObjectStruct paletteArpg) {

        Object2ObjectOpenHashMap<String, String> text2OldKey = new Object2ObjectOpenHashMap<>();
        Object2ObjectOpenHashMap<String, ArpgObjectStruct> key2Entry = new Object2ObjectOpenHashMap<>();
        ObjectArrayList<ArpgObjectStruct> fresh = new ObjectArrayList<>();
        ObjectArrayList<String> usedKeys = new ObjectArrayList<>();

        for (Object2ObjectOpenHashMap.Entry<String, ArpgObjectStruct> keyed : readPalette(structureArpg)
                .object2ObjectEntrySet())
            text2OldKey.put(ArpgUtility.formatCompactText(keyed.getValue()), keyed.getKey());

        for (Long2ObjectMap.Entry<ArpgObjectStruct> cell : cells.long2ObjectEntrySet()) {

            String text = ArpgUtility.formatCompactText(cell.getValue());

            if (text2Key.containsKey(text))
                continue;

            String oldKey = text2OldKey.get(text);

            if (oldKey == null) {
                text2Key.put(text, null);
                fresh.add(cell.getValue());
                continue;
            }

            text2Key.put(text, oldKey);
            key2Entry.put(oldKey, cell.getValue());
            usedKeys.add(oldKey);
        }

        String pool = EditorSetting.STRUCTURE_EDITOR_PALETTE_KEYS;
        int next = 0;

        for (int i = 0; i < fresh.size(); i++) {

            while (next < pool.length() && usedKeys.contains(String.valueOf(pool.charAt(next))))
                next++;

            if (next >= pool.length())
                return false;

            String key = String.valueOf(pool.charAt(next++));
            usedKeys.add(key);
            key2Entry.put(key, fresh.get(i));
            text2Key.put(ArpgUtility.formatCompactText(fresh.get(i)), key);
        }

        usedKeys.sort(null);

        for (int i = 0; i < usedKeys.size(); i++)
            paletteArpg.add(usedKeys.get(i), key2Entry.get(usedKeys.get(i)).deepCopy());

        return true;
    }

    // Minimum and maximum x, y and z of every cell
    private int[] resolveBounds(Long2ObjectOpenHashMap<ArpgObjectStruct> cells) {

        int[] bounds = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };

        for (long position : cells.keySet()) {

            bounds[0] = Math.min(bounds[0], Coordinate3Long.unpackX(position));
            bounds[1] = Math.min(bounds[1], Coordinate3Long.unpackY(position));
            bounds[2] = Math.min(bounds[2], Coordinate3Long.unpackZ(position));
            bounds[3] = Math.max(bounds[3], Coordinate3Long.unpackX(position));
            bounds[4] = Math.max(bounds[4], Coordinate3Long.unpackY(position));
            bounds[5] = Math.max(bounds[5], Coordinate3Long.unpackZ(position));
        }

        return bounds;
    }

    // The origin written whenever it lies off the corner or the entry already names one
    private void writeOrigin(ArpgObjectStruct structureArpg, int[] origin) {

        if (!structureArpg.has("origin") && origin[0] == 0 && origin[1] == 0 && origin[2] == 0)
            return;

        ArpgArrayStruct originArpg = new ArpgArrayStruct();
        originArpg.add((long) origin[0]);
        originArpg.add((long) origin[1]);
        originArpg.add((long) origin[2]);
        structureArpg.add("origin", originArpg);
    }

    // One layer for every height holding a cell, its rows along z and each row's characters along x
    private ArpgArrayStruct writeLayers(
            Long2ObjectOpenHashMap<ArpgObjectStruct> cells,
            Object2ObjectOpenHashMap<String, String> text2Key,
            int[] bounds) {

        ArpgArrayStruct layers = new ArpgArrayStruct();

        if (cells.isEmpty())
            return layers;

        StringBuilder row = new StringBuilder();

        for (int y = bounds[1]; y <= bounds[4]; y++) {

            ArpgArrayStruct rows = new ArpgArrayStruct();
            int lastRow = EngineSetting.INDEX_NOT_FOUND;

            for (int z = 0; z <= bounds[5]; z++) {

                row.setLength(0);

                for (int x = 0; x <= bounds[3]; x++) {

                    ArpgObjectStruct entry = cells.get(Coordinate3Long.pack(x, y, z));
                    row.append(entry == null
                            ? EngineSetting.STRUCTURE_LAYER_SKIP_CHARACTER
                            : text2Key.get(ArpgUtility.formatCompactText(entry)).charAt(0));
                }

                String trimmed = row.toString().stripTrailing();

                rows.add(trimmed);

                if (!trimmed.isEmpty())
                    lastRow = rows.size();
            }

            if (lastRow == EngineSetting.INDEX_NOT_FOUND)
                continue;

            while (rows.size() > lastRow)
                rows.remove(rows.size() - 1);

            ArpgObjectStruct layer = new ArpgObjectStruct();
            layer.addProperty("y", (long) y);
            layer.add("rows", rows);
            layers.add(layer);
        }

        return layers;
    }
}
