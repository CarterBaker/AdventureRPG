package editor.bootstrap.architecturebuilderpipeline.architecturebuildermanager;

import java.util.function.Consumer;

import editor.bootstrap.architecturebuilderpipeline.architecturebuilder.ArchitectureSlotKind;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class ArchitectureSlotBranch extends BranchPackage {

    /*
     * Reads, fills and empties the slots of an architecture straight in its
     * Info entry. Every edit is the Info Panel's own kind, made through
     * InfoManager.editEntry(), so the file is marked changed, the Properties
     * tab shows the new value at once and Save and Revert work on it as on
     * any content. A block slot lives under its group's map in "blocks" and a
     * furniture slot under its table's name in "furniture"; emptying the last
     * slot of a group or a kind takes the emptied map away with it, so the
     * file only ever holds what the architecture fills.
     */

    // Internal
    private InfoManager infoManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    // Read \\

    // What an architecture fills a slot with, null while it leaves the slot empty or has no entry
    String read(String architectureName, ArchitectureSlotKind kind, String slotName) {

        InfoEntryStruct entry = findEntry(architectureName);

        if (entry == null)
            return null;

        ArpgObjectStruct holder = findHolder(entry.getArpg(), kind, slotName);
        String key = toKey(kind, slotName);

        if (holder == null || !holder.has(key))
            return null;

        ArpgElementStruct value = holder.get(key);

        return value.isValue() ? value.getAsString() : null;
    }

    // Every slot name the architecture's entry fills of one kind, handed to a visitor in file order
    void collect(String architectureName, ArchitectureSlotKind kind, Consumer<String> visitor) {

        InfoEntryStruct entry = findEntry(architectureName);

        if (entry == null || !ArpgUtility.hasObject(entry.getArpg(), kind.field))
            return;

        ArpgObjectStruct kindArpg = entry.getArpg().getAsObject(kind.field);

        for (String key : kindArpg.keySet()) {

            if (!kind.nested) {
                visitor.accept(key);
                continue;
            }

            if (!ArpgUtility.hasObject(kindArpg, key))
                continue;

            for (String place : kindArpg.getAsObject(key).keySet())
                visitor.accept(key + EngineSetting.ARCHITECTURE_SLOT_SEPARATOR + place);
        }
    }

    private ArpgObjectStruct findHolder(ArpgObjectStruct architectureArpg, ArchitectureSlotKind kind, String slotName) {

        if (!ArpgUtility.hasObject(architectureArpg, kind.field))
            return null;

        ArpgObjectStruct kindArpg = architectureArpg.getAsObject(kind.field);

        if (!kind.nested)
            return kindArpg;

        String group = toGroup(slotName);

        return ArpgUtility.hasObject(kindArpg, group) ? kindArpg.getAsObject(group) : null;
    }

    // Write \\

    // A slot filled with a block or an item — false when the architecture has no entry to edit
    boolean fill(String architectureName, ArchitectureSlotKind kind, String slotName, String value) {

        if (findEntry(architectureName) == null)
            return false;

        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_ARCHITECTURES, architectureName, architectureName,
                architectureArpg -> resolveHolder(architectureArpg, kind, slotName)
                        .addProperty(toKey(kind, slotName), value));

        return true;
    }

    // A slot emptied, so every structure keeps what it was authored with there — false when nothing fills it
    boolean empty(String architectureName, ArchitectureSlotKind kind, String slotName) {

        if (read(architectureName, kind, slotName) == null)
            return false;

        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_ARCHITECTURES, architectureName, architectureName,
                architectureArpg -> remove(architectureArpg, kind, slotName));

        return true;
    }

    private void remove(ArpgObjectStruct architectureArpg, ArchitectureSlotKind kind, String slotName) {

        ArpgObjectStruct kindArpg = architectureArpg.getAsObject(kind.field);
        ArpgObjectStruct holder = findHolder(architectureArpg, kind, slotName);

        holder.remove(toKey(kind, slotName));

        if (kind.nested && holder.size() == 0)
            kindArpg.remove(toGroup(slotName));

        if (kindArpg.size() == 0)
            architectureArpg.remove(kind.field);
    }

    // The map a slot is kept in, made where the entry has none yet
    private ArpgObjectStruct resolveHolder(
            ArpgObjectStruct architectureArpg,
            ArchitectureSlotKind kind,
            String slotName) {

        ArpgObjectStruct kindArpg = resolveObject(architectureArpg, kind.field);

        return kind.nested ? resolveObject(kindArpg, toGroup(slotName)) : kindArpg;
    }

    private ArpgObjectStruct resolveObject(ArpgObjectStruct parent, String key) {

        if (!ArpgUtility.hasObject(parent, key))
            parent.add(key, new ArpgObjectStruct());

        return parent.getAsObject(key);
    }

    // Utility \\

    private InfoEntryStruct findEntry(String architectureName) {

        if (architectureName == null
                || !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_ARCHITECTURES, architectureName))
            return null;

        return infoManager.findEntry(EditorSetting.INFO_SCHEMA_ARCHITECTURES, architectureName, architectureName);
    }

    // The key a slot is kept under in its map: its place within its group, or its full table name
    private String toKey(ArchitectureSlotKind kind, String slotName) {
        return kind.nested
                ? slotName.substring(slotName.indexOf(EngineSetting.ARCHITECTURE_SLOT_SEPARATOR) + 1)
                : slotName;
    }

    private String toGroup(String slotName) {
        return slotName.substring(0, slotName.lastIndexOf(EngineSetting.ARCHITECTURE_SLOT_SEPARATOR));
    }
}
