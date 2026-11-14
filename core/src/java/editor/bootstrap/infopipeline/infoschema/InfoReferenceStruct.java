package editor.bootstrap.infopipeline.infoschema;

import editor.bootstrap.infopipeline.util.InfoReferenceKind;
import editor.bootstrap.infopipeline.util.InfoResourceNaming;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoReferenceStruct extends StructPackage {

    /*
     * What one string field must name for the game to load it. A schema
     * reference names an entry of one schema, or of the schema a sibling
     * key's value picks; a resource reference names a file of an asset
     * directory; a local reference names a value found along a path through
     * the same entry. Allowed values are accepted as they are, such as an
     * empty string the game reads as none.
     */

    // Kind
    private final InfoReferenceKind kind;

    // Schema
    private final String schemaName;
    private final String keyField;
    private final Object2ObjectOpenHashMap<String, String> keyValue2SchemaName;

    // Resource
    private final String directory;
    private final InfoResourceNaming naming;

    // Local
    private final ObjectArrayList<InfoLocalStepStruct> localSteps;

    // Accepted
    private final ObjectArrayList<String> allowedValues;

    // Constructor \\

    public InfoReferenceStruct(
            InfoReferenceKind kind,
            String schemaName,
            String keyField,
            Object2ObjectOpenHashMap<String, String> keyValue2SchemaName,
            String directory,
            InfoResourceNaming naming,
            ObjectArrayList<InfoLocalStepStruct> localSteps,
            ObjectArrayList<String> allowedValues) {

        // Kind
        this.kind = kind;

        // Schema
        this.schemaName = schemaName;
        this.keyField = keyField;
        this.keyValue2SchemaName = keyValue2SchemaName;

        // Resource
        this.directory = directory;
        this.naming = naming;

        // Local
        this.localSteps = localSteps;

        // Accepted
        this.allowedValues = allowedValues;
    }

    // Schema \\

    public boolean isKeyed() {
        return keyField != null;
    }

    // The schema a sibling key's value picks, null when it picks none
    public String resolveSchemaName(String keyValue) {

        for (Object2ObjectOpenHashMap.Entry<String, String> entry : keyValue2SchemaName.object2ObjectEntrySet())
            if (entry.getKey().equalsIgnoreCase(keyValue))
                return entry.getValue();

        return null;
    }

    public boolean isAllowed(String value) {
        return allowedValues.contains(value);
    }

    // Accessible \\

    public InfoReferenceKind getKind() {
        return kind;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getKeyField() {
        return keyField;
    }

    public Object2ObjectOpenHashMap<String, String> getKeyValue2SchemaName() {
        return keyValue2SchemaName;
    }

    public String getDirectory() {
        return directory;
    }

    public InfoResourceNaming getNaming() {
        return naming;
    }

    public ObjectArrayList<InfoLocalStepStruct> getLocalSteps() {
        return localSteps;
    }

    public ObjectArrayList<String> getAllowedValues() {
        return allowedValues;
    }
}
