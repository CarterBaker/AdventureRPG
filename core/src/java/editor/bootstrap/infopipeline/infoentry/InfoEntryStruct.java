package editor.bootstrap.infopipeline.infoentry;

import engine.root.StructPackage;
import engine.util.arpg.ArpgObjectStruct;

public class InfoEntryStruct extends StructPackage {

    /*
     * One named entry of an info schema as other editor systems see it: the
     * file it lives in, its name, and its live ARPG tree. In a file layout the file
     * is the entry, so its name is the definition name.
     */

    // Identity
    private final String schemaName;
    private final String definitionName;
    private final String entryName;

    // Arpg
    private final ArpgObjectStruct arpg;

    // Constructor \\

    public InfoEntryStruct(String schemaName, String definitionName, String entryName, ArpgObjectStruct arpg) {

        // Identity
        this.schemaName = schemaName;
        this.definitionName = definitionName;
        this.entryName = entryName;

        // Arpg
        this.arpg = arpg;
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

    public ArpgObjectStruct getArpg() {
        return arpg;
    }
}
