package editor.bootstrap.infopipeline.infodocument;

import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoDocumentInstance extends InstancePackage {

    /*
     * One content file held in the editor: the schema it follows, its name
     * relative to the schema's directory, and the ARPG tree being edited. Edits stay
     * in memory until saved; dirty marks changes not yet written and onDisk
     * whether the file exists at all. In an array layout the named entries
     * are the objects of the schema's entries array, found by their name field.
     */

    // Identity
    private InfoSchemaHandle schema;
    private String definitionName;

    // Arpg
    private ArpgObjectStruct root;

    // State
    private boolean dirty;
    private boolean onDisk;

    // Constructor \\

    public void constructor(InfoSchemaHandle schema, String definitionName, ArpgObjectStruct root, boolean onDisk) {

        // Identity
        this.schema = schema;
        this.definitionName = definitionName;

        // Arpg
        this.root = root;

        // State
        this.dirty = !onDisk;
        this.onDisk = onDisk;
    }

    // Management \\

    public void markEdited() {
        this.dirty = true;
    }

    public void markSaved() {

        this.dirty = false;
        this.onDisk = true;
    }

    public void replaceRoot(ArpgObjectStruct root) {

        this.root = root;
        this.dirty = false;
    }

    // Entries \\

    public ArpgArrayStruct getEntryArray() {

        ArpgElementStruct entries = root.get(schema.getEntriesKey());
        return entries != null && entries.isArray() ? entries.getAsArray() : new ArpgArrayStruct();
    }

    public ArpgArrayStruct requireEntryArray() {

        ArpgElementStruct entries = root.get(schema.getEntriesKey());

        if (entries == null || !entries.isArray())
            root.add(schema.getEntriesKey(), new ArpgArrayStruct());

        return root.getAsArray(schema.getEntriesKey());
    }

    public int findEntryIndex(String entryName) {

        ArpgArrayStruct entries = getEntryArray();

        for (int i = 0; i < entries.size(); i++)
            if (entryName.equals(getEntryName(entries.get(i))))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    public ArpgObjectStruct findEntry(String entryName) {

        int entryIndex = findEntryIndex(entryName);

        return entryIndex != EngineSetting.INDEX_NOT_FOUND
                ? getEntryArray().get(entryIndex).getAsObject()
                : null;
    }

    public ObjectArrayList<String> getEntryNames() {

        ArpgArrayStruct entries = getEntryArray();
        ObjectArrayList<String> entryNames = new ObjectArrayList<>(entries.size());

        for (int i = 0; i < entries.size(); i++) {

            String entryName = getEntryName(entries.get(i));

            if (entryName != null)
                entryNames.add(entryName);
        }

        return entryNames;
    }

    private String getEntryName(ArpgElementStruct entry) {

        if (!entry.isObject())
            return null;

        ArpgElementStruct name = entry.getAsObject().get(schema.getNameField());

        return name != null && name.isValue() ? name.getAsString() : null;
    }

    // Accessible \\

    public InfoSchemaHandle getSchema() {
        return schema;
    }

    public String getDefinitionName() {
        return definitionName;
    }

    public String getFolderName() {

        int separator = definitionName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR);
        return separator != EngineSetting.INDEX_NOT_FOUND ? definitionName.substring(0, separator) : "";
    }

    public String getFileName() {

        int separator = definitionName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR);
        return separator != EngineSetting.INDEX_NOT_FOUND ? definitionName.substring(separator + 1) : definitionName;
    }

    public ArpgObjectStruct getRoot() {
        return root;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isOnDisk() {
        return onDisk;
    }
}
