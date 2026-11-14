package editor.bootstrap.infopipeline.infodocument;

import editor.bootstrap.infopipeline.infoissue.InfoIssueStruct;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class InfoDocumentInstance extends InstancePackage {

    /*
     * One content file held in the editor: the schema it follows, its name
     * relative to the schema's directory, and the ARPG tree being edited. Edits stay
     * in memory until saved; dirty marks changes not yet written and onDisk
     * whether the file exists at all. In an array layout the named entries
     * are the objects of the schema's entries array, found by their name field.
     * The entry names the file holds on disk are kept beside the live ones, so
     * a save can tell which names it would take away, and every edit moves the
     * edit revision its cached problems were found at. The values it holds
     * under its schema's unique keys are kept as one signature, so only an
     * edit that moves one of them makes every file of the schema look again.
     */

    // Identity
    private InfoSchemaHandle schema;
    private String definitionName;

    // Arpg
    private ArpgObjectStruct root;

    // State
    private boolean dirty;
    private boolean onDisk;
    private int editRevision;

    // Names
    private ObjectArrayList<String> liveEntryNames;
    private ObjectOpenHashSet<String> savedEntryNames;

    // Unique
    private String uniqueSignature;

    // Issues
    private ObjectArrayList<InfoIssueStruct> issues;
    private int errorCount;
    private int issueEditRevision;
    private int issueContentRevision;

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

        // Names
        this.liveEntryNames = collectEntryNames();
        this.savedEntryNames = onDisk ? new ObjectOpenHashSet<>(liveEntryNames) : new ObjectOpenHashSet<>();

        // Unique
        this.uniqueSignature = collectUniqueSignature();

        // Issues
        this.issues = new ObjectArrayList<>();
        this.issueEditRevision = EngineSetting.INDEX_NOT_FOUND;
        this.issueContentRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    // Management \\

    public void markEdited() {

        this.dirty = true;
        this.editRevision++;
    }

    public void markSaved() {

        this.dirty = false;
        this.onDisk = true;
        this.savedEntryNames = new ObjectOpenHashSet<>(collectEntryNames());
    }

    public void replaceRoot(ArpgObjectStruct root) {

        this.root = root;
        this.dirty = false;
        this.editRevision++;
        this.savedEntryNames = new ObjectOpenHashSet<>(collectEntryNames());
    }

    // Puts back content a refused save had written into the tree, as edited as it was before
    public void restoreRoot(ArpgObjectStruct root, boolean dirty) {

        this.root = root;
        this.dirty = dirty;
        this.editRevision++;
    }

    // True when the live entry names moved since they were last read
    public boolean refreshEntryNames() {

        ObjectArrayList<String> entryNames = collectEntryNames();

        if (entryNames.equals(liveEntryNames))
            return false;

        this.liveEntryNames = entryNames;
        return true;
    }

    // True when a value under one of the schema's unique keys moved since they were last read
    public boolean refreshUniqueSignature() {

        String signature = collectUniqueSignature();

        if (signature.equals(uniqueSignature))
            return false;

        this.uniqueSignature = signature;
        return true;
    }

    private String collectUniqueSignature() {

        ObjectArrayList<String> uniqueKeys = schema.getUniqueKeys();
        StringBuilder signature = new StringBuilder();

        if (uniqueKeys.isEmpty())
            return "";

        if (!schema.isArrayLayout()) {
            appendUniqueValues(root, uniqueKeys, signature);
            return signature.toString();
        }

        for (ArpgElementStruct entry : getEntryArray())
            if (entry.isObject())
                appendUniqueValues(entry.getAsObject(), uniqueKeys, signature);

        return signature.toString();
    }

    private void appendUniqueValues(ArpgObjectStruct entryArpg, ObjectArrayList<String> uniqueKeys, StringBuilder out) {

        for (int i = 0; i < uniqueKeys.size(); i++) {

            ArpgElementStruct value = entryArpg.get(uniqueKeys.get(i));

            out.append(value != null ? value.toString() : "").append(EditorSetting.HIERARCHY_KEY_SEPARATOR);
        }
    }

    // Issues \\

    public void setIssues(ObjectArrayList<InfoIssueStruct> issues, int issueEditRevision, int issueContentRevision) {

        this.issues = issues;
        this.errorCount = 0;
        this.issueEditRevision = issueEditRevision;

        for (int i = 0; i < issues.size(); i++)
            if (issues.get(i).isError())
                errorCount++;

        this.issueContentRevision = issueContentRevision;
    }

    public boolean hasIssuesAt(int issueEditRevision, int issueContentRevision) {
        return this.issueEditRevision == issueEditRevision && this.issueContentRevision == issueContentRevision;
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
        return collectEntryNames();
    }

    private ObjectArrayList<String> collectEntryNames() {

        if (!schema.isArrayLayout())
            return new ObjectArrayList<>();

        ArpgArrayStruct entries = getEntryArray();
        ObjectArrayList<String> entryNames = new ObjectArrayList<>(entries.size());

        for (int i = 0; i < entries.size(); i++) {

            String entryName = getEntryName(entries.get(i));

            if (entryName != null)
                entryNames.add(entryName);
        }

        return entryNames;
    }

    public String getEntryName(ArpgElementStruct entry) {

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

    public int getEditRevision() {
        return editRevision;
    }

    public ObjectOpenHashSet<String> getSavedEntryNames() {
        return savedEntryNames;
    }

    public ObjectArrayList<InfoIssueStruct> getIssues() {
        return issues;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }
}
