package editor.bootstrap.infopipeline.infolink;

import editor.bootstrap.infopipeline.util.InfoReferenceKind;
import engine.root.StructPackage;

public class InfoLinkStruct extends StructPackage {

    /*
     * One name a content file depends on: whether it names a schema's entry
     * or a resource file, the schema or directory it names it in, the name
     * itself, and the schema, file, entry and row path it is written at.
     */

    // Target
    private final InfoReferenceKind kind;
    private final String targetName;
    private final String name;

    // Location
    private final String sourceSchemaName;
    private final String definitionName;
    private final String entryName;
    private final String path;

    // Constructor \\

    public InfoLinkStruct(
            InfoReferenceKind kind,
            String targetName,
            String name,
            String sourceSchemaName,
            String definitionName,
            String entryName,
            String path) {

        // Target
        this.kind = kind;
        this.targetName = targetName;
        this.name = name;

        // Location
        this.sourceSchemaName = sourceSchemaName;
        this.definitionName = definitionName;
        this.entryName = entryName;
        this.path = path;
    }

    // Accessible \\

    public InfoReferenceKind getKind() {
        return kind;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getName() {
        return name;
    }

    public String getSourceSchemaName() {
        return sourceSchemaName;
    }

    public String getDefinitionName() {
        return definitionName;
    }

    public String getEntryName() {
        return entryName;
    }

    public String getPath() {
        return path;
    }
}
