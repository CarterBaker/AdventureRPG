package editor.bootstrap.itemeditorpipeline.itementry;

import application.bootstrap.itempipeline.util.ItemRegistryUtility;
import engine.root.StructPackage;

public class ItemEntryStruct extends StructPackage {

    /*
     * Where one item lives on disk: the definition file that declares it, its
     * name within that file, and the mesh it draws with. The full item name is
     * built by the same rule the item pipeline registers items under.
     */

    // Identity
    private final String itemName;
    private final String localName;
    private final String definitionName;

    // Mesh
    private final String meshName;

    // Constructor \\

    public ItemEntryStruct(String definitionName, String localName, String meshName) {

        // Identity
        this.itemName = ItemRegistryUtility.toItemName(definitionName, localName);
        this.localName = localName;
        this.definitionName = definitionName;

        // Mesh
        this.meshName = meshName;
    }

    // Accessible \\

    public String getItemName() {
        return itemName;
    }

    public String getLocalName() {
        return localName;
    }

    public String getDefinitionName() {
        return definitionName;
    }

    public String getMeshName() {
        return meshName;
    }
}
