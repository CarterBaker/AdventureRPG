package editor.bootstrap.sceneassetspipeline.sceneassets;

import engine.root.StructPackage;

public class SceneAssetRowStruct extends StructPackage {

    /*
     * One row of the Scene Assets tab: what it names and what it says beside
     * that, and, when it names content the editor edits, the schema, file
     * and entry the Info Panel opens it by; a row naming no content has none.
     */

    // Row
    private final String label;
    private final String value;

    // Content
    private final String schemaName;
    private final String definitionName;
    private final String entryName;

    // Constructor \\

    public SceneAssetRowStruct(
            String label,
            String value,
            String schemaName,
            String definitionName,
            String entryName) {
        this.label = label;
        this.value = value;
        this.schemaName = schemaName;
        this.definitionName = definitionName;
        this.entryName = entryName;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    // True when the row names content the Info Panel can open
    public boolean isContent() {
        return schemaName != null;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getDefinitionName() {
        return definitionName;
    }

    public String getEntryName() {
        return entryName;
    }
}
