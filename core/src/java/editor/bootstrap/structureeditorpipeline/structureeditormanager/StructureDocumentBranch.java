package editor.bootstrap.structureeditorpipeline.structureeditormanager;

import editor.bootstrap.structureeditorpipeline.structureeditor.StructureEditorPartStruct;
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
     * Reads a structure's entry into the layers the Structure Editor paints
     * and writes them back, the one place the editor speaks the structure
     * format. The structure's own "layers", "blocks", "furnishings" and
     * "spawns" read as its first layer, then every one of its "parts" as a
     * layer above it in order. Every block a layer lays is one cell, keyed by
     * its position from the corner, holding the block entry it is laid with
     * (block, slot, shape, facing, spin, covering, coverage, content); a
     * later entry overwrites an earlier one at the same cell, as
     * StructureBuilder reads them, and one laid below zero is dropped, as no
     * layer row can lay it. Written back, every layer becomes a part, lowest
     * first, sharing one palette keyed by single characters, every entry the
     * structure already keyed keeping its key, its blocks as layer rows from
     * the corner; the structure's own layers, blocks, furnishings and spawns
     * are folded into its parts. Entries are never changed in place: an edit
     * lays a new one.
     */

    // Read \\

    // Every layer a structure entry holds, lowest first, and its origin
    void read(ArpgObjectStruct structureArpg, ObjectArrayList<StructureEditorPartStruct> parts, int[] origin) {

        parts.clear();

        int[] authored = readVector(structureArpg, "origin");

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
            origin[axis] = authored != null ? authored[axis] : 0;

        Object2ObjectOpenHashMap<String, ArpgObjectStruct> key2Entry = readPalette(structureArpg);

        if (isHoldingContent(structureArpg) || !ArpgUtility.hasArray(structureArpg, "parts"))
            parts.add(readPart(structureArpg, EditorSetting.STRUCTURE_EDITOR_FIRST_LAYER_NAME, key2Entry));

        if (!ArpgUtility.hasArray(structureArpg, "parts"))
            return;

        for (ArpgElementStruct part : structureArpg.getAsArray("parts"))
            parts.add(readPart(
                    part.getAsObject(),
                    ArpgUtility.getString(part.getAsObject(), "name", EditorSetting.STRUCTURE_EDITOR_LAYER_NAME),
                    key2Entry));
    }

    private boolean isHoldingContent(ArpgObjectStruct arpg) {
        return ArpgUtility.hasArray(arpg, "layers") || ArpgUtility.hasArray(arpg, "blocks")
                || ArpgUtility.hasArray(arpg, "furnishings") || ArpgUtility.hasArray(arpg, "spawns");
    }

    private StructureEditorPartStruct readPart(
            ArpgObjectStruct partArpg,
            String name,
            Object2ObjectOpenHashMap<String, ArpgObjectStruct> key2Entry) {

        StructureEditorPartStruct part = new StructureEditorPartStruct(name);
        Long2ObjectOpenHashMap<ArpgObjectStruct> cells = part.getCells();

        if (ArpgUtility.hasArray(partArpg, "layers"))
            for (ArpgElementStruct layer : partArpg.getAsArray("layers"))
                readLayer(layer.getAsObject(), key2Entry, cells);

        if (ArpgUtility.hasArray(partArpg, "blocks"))
            for (ArpgElementStruct block : partArpg.getAsArray("blocks"))
                readBlock(block.getAsObject(), key2Entry, cells);

        readEntries(partArpg, "furnishings", part.getFurnishings());
        readEntries(partArpg, "spawns", part.getSpawns());

        return part;
    }

    private void readEntries(ArpgObjectStruct partArpg, String key, ObjectArrayList<ArpgObjectStruct> out) {

        if (!ArpgUtility.hasArray(partArpg, key))
            return;

        for (ArpgElementStruct entry : partArpg.getAsArray(key))
            if (entry.isObject())
                out.add(entry.getAsObject().deepCopy());
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

        for (int y = Math.max(0, baseY); y < baseY + height; y++)
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

    // Every layer and the origin written into a structure entry as one shared palette and a part to each layer —
    // false, the entry untouched, when the layers lay more distinct block entries than palette keys exist
    boolean write(ArpgObjectStruct structureArpg, ObjectArrayList<StructureEditorPartStruct> parts, int[] origin) {

        Object2ObjectOpenHashMap<String, String> text2Key = new Object2ObjectOpenHashMap<>();
        ArpgObjectStruct paletteArpg = new ArpgObjectStruct();

        if (!keyEntries(structureArpg, parts, text2Key, paletteArpg))
            return false;

        ArpgArrayStruct partsArpg = new ArpgArrayStruct();

        for (int i = 0; i < parts.size(); i++)
            partsArpg.add(writePart(parts.get(i), text2Key));

        writeOrigin(structureArpg, origin);
        structureArpg.add("palette", paletteArpg);
        structureArpg.remove("layers");
        structureArpg.remove("blocks");
        structureArpg.remove("furnishings");
        structureArpg.remove("spawns");
        structureArpg.add("parts", partsArpg);

        return true;
    }

    private ArpgObjectStruct writePart(
            StructureEditorPartStruct part,
            Object2ObjectOpenHashMap<String, String> text2Key) {

        ArpgObjectStruct partArpg = new ArpgObjectStruct();

        partArpg.addProperty("name", part.getName());
        partArpg.add("layers", writeLayers(part.getCells(), text2Key));
        writeEntries(partArpg, "furnishings", part.getFurnishings());
        writeEntries(partArpg, "spawns", part.getSpawns());

        return partArpg;
    }

    private void writeEntries(ArpgObjectStruct partArpg, String key, ObjectArrayList<ArpgObjectStruct> entries) {

        if (entries.isEmpty())
            return;

        ArpgArrayStruct entriesArpg = new ArpgArrayStruct();

        for (int i = 0; i < entries.size(); i++)
            entriesArpg.add(entries.get(i).deepCopy());

        partArpg.add(key, entriesArpg);
    }

    // Every distinct entry the layers lay given a key, keeping each key the entry already gave it
    private boolean keyEntries(
            ArpgObjectStruct structureArpg,
            ObjectArrayList<StructureEditorPartStruct> parts,
            Object2ObjectOpenHashMap<String, String> text2Key,
            ArpgObjectStruct paletteArpg) {

        Object2ObjectOpenHashMap<String, String> text2OldKey = new Object2ObjectOpenHashMap<>();
        Object2ObjectOpenHashMap<String, ArpgObjectStruct> key2Entry = new Object2ObjectOpenHashMap<>();
        ObjectArrayList<ArpgObjectStruct> fresh = new ObjectArrayList<>();
        ObjectArrayList<String> usedKeys = new ObjectArrayList<>();

        for (Object2ObjectOpenHashMap.Entry<String, ArpgObjectStruct> keyed : readPalette(structureArpg)
                .object2ObjectEntrySet())
            text2OldKey.put(ArpgUtility.formatCompactText(keyed.getValue()), keyed.getKey());

        for (int i = 0; i < parts.size(); i++)
            for (Long2ObjectMap.Entry<ArpgObjectStruct> cell : parts.get(i).getCells().long2ObjectEntrySet()) {

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

    // One layer row set for every height holding a cell, its rows along z from the corner and each row's characters
    // along x
    private ArpgArrayStruct writeLayers(
            Long2ObjectOpenHashMap<ArpgObjectStruct> cells,
            Object2ObjectOpenHashMap<String, String> text2Key) {

        ArpgArrayStruct layers = new ArpgArrayStruct();

        if (cells.isEmpty())
            return layers;

        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;

        for (long cell : cells.keySet()) {
            maxX = Math.max(maxX, Coordinate3Long.unpackX(cell));
            maxY = Math.max(maxY, Coordinate3Long.unpackY(cell));
            maxZ = Math.max(maxZ, Coordinate3Long.unpackZ(cell));
        }

        StringBuilder row = new StringBuilder();

        for (int y = 0; y <= maxY; y++) {

            ArpgArrayStruct rows = new ArpgArrayStruct();
            int lastRow = 0;

            for (int z = 0; z <= maxZ; z++) {

                row.setLength(0);

                for (int x = 0; x <= maxX; x++) {

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

            if (lastRow == 0)
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
