package editor.bootstrap.infopipeline.infoschema;

import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoSchemaData extends DataPackage {

    /*
     * Immutable description of one kind of ARPG content: the hierarchy tab it
     * appears under, the asset directory its files live in, and the fields an
     * entry allows. With an entries key every file holds many named entries in
     * that array, and new entries land in the default file when none is
     * chosen; without one every file is a single entry. Entries of an array
     * layout are referenced by their file and name, or by their name alone
     * when the game registers them so, and required names are the entries the
     * game cannot start without. Unique keys name the top-level fields
     * no two entries may share, and the schemas and resource directories its
     * fields reference are what its entries depend on.
     */

    // Identity
    private final String schemaName;
    private final String tabName;
    private final int order;

    // Files
    private final String directory;
    private final String entriesKey;
    private final String nameField;
    private final String defaultFile;
    private final boolean referencedByName;
    private final ObjectArrayList<String> requiredNames;

    // Fields
    private final InfoFieldStruct rootField;
    private final ObjectArrayList<String> uniqueKeys;

    // Dependencies
    private final ObjectArrayList<String> referencedSchemaNames;
    private final ObjectArrayList<String> referencedDirectories;

    // Constructor \\

    public InfoSchemaData(
            String schemaName,
            String tabName,
            int order,
            String directory,
            String entriesKey,
            String nameField,
            String defaultFile,
            boolean referencedByName,
            ObjectArrayList<String> requiredNames,
            InfoFieldStruct rootField,
            ObjectArrayList<String> uniqueKeys,
            ObjectArrayList<String> referencedSchemaNames,
            ObjectArrayList<String> referencedDirectories) {

        // Identity
        this.schemaName = schemaName;
        this.tabName = tabName;
        this.order = order;

        // Files
        this.directory = directory;
        this.entriesKey = entriesKey;
        this.nameField = nameField;
        this.defaultFile = defaultFile;
        this.referencedByName = referencedByName;
        this.requiredNames = requiredNames;

        // Fields
        this.rootField = rootField;
        this.uniqueKeys = uniqueKeys;

        // Dependencies
        this.referencedSchemaNames = referencedSchemaNames;
        this.referencedDirectories = referencedDirectories;
    }

    // Accessible \\

    public String getSchemaName() {
        return schemaName;
    }

    public String getTabName() {
        return tabName;
    }

    public int getOrder() {
        return order;
    }

    public String getDirectory() {
        return directory;
    }

    public String getEntriesKey() {
        return entriesKey;
    }

    public String getNameField() {
        return nameField;
    }

    public String getDefaultFile() {
        return defaultFile;
    }

    public boolean isReferencedByName() {
        return referencedByName;
    }

    public ObjectArrayList<String> getRequiredNames() {
        return requiredNames;
    }

    public InfoFieldStruct getRootField() {
        return rootField;
    }

    public ObjectArrayList<String> getUniqueKeys() {
        return uniqueKeys;
    }

    public ObjectArrayList<String> getReferencedSchemaNames() {
        return referencedSchemaNames;
    }

    public ObjectArrayList<String> getReferencedDirectories() {
        return referencedDirectories;
    }

    public boolean isArrayLayout() {
        return entriesKey != null;
    }
}
