package editor.bootstrap.infopipeline.infoschema;

import engine.root.DataPackage;

public class InfoSchemaData extends DataPackage {

    /*
     * Immutable description of one kind of JSON content: the hierarchy tab it
     * appears under, the asset directory its files live in, and the fields an
     * entry allows. With an entries key every file holds many named entries in
     * that array, and new entries land in the default file when none is
     * chosen; without one every file is a single entry.
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

    // Fields
    private final InfoFieldStruct rootField;

    // Constructor \\

    public InfoSchemaData(
            String schemaName,
            String tabName,
            int order,
            String directory,
            String entriesKey,
            String nameField,
            String defaultFile,
            InfoFieldStruct rootField) {

        // Identity
        this.schemaName = schemaName;
        this.tabName = tabName;
        this.order = order;

        // Files
        this.directory = directory;
        this.entriesKey = entriesKey;
        this.nameField = nameField;
        this.defaultFile = defaultFile;

        // Fields
        this.rootField = rootField;
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

    public InfoFieldStruct getRootField() {
        return rootField;
    }

    public boolean isArrayLayout() {
        return entriesKey != null;
    }
}
