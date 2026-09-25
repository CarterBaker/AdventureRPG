package editor.bootstrap.infopipeline.infoentry;

import com.google.gson.JsonObject;

import engine.root.StructPackage;

public class InfoEntryStruct extends StructPackage {

    /*
     * One named entry of an info schema as other editor systems see it: the
     * file it lives in, its name, and its live JSON. In a file layout the file
     * is the entry, so its name is the definition name.
     */

    // Identity
    private final String schemaName;
    private final String definitionName;
    private final String entryName;

    // Json
    private final JsonObject json;

    // Constructor \\

    public InfoEntryStruct(String schemaName, String definitionName, String entryName, JsonObject json) {

        // Identity
        this.schemaName = schemaName;
        this.definitionName = definitionName;
        this.entryName = entryName;

        // Json
        this.json = json;
    }

    // Accessible \\

    public String getSchemaName() {
        return schemaName;
    }

    public String getDefinitionName() {
        return definitionName;
    }

    public String getEntryName() {
        return entryName;
    }

    public JsonObject getJson() {
        return json;
    }
}
