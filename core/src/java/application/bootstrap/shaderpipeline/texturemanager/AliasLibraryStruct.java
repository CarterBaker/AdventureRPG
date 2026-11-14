package application.bootstrap.shaderpipeline.texturemanager;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AliasLibraryStruct extends StructPackage {

    /*
     * Every alias definition and the names that select each, matched ignoring
     * case: a file's suffix after its tile name names one. Loaded by
     * TextureBakeUtility from the alias directory, by the bake tool and by
     * the editor alike, and read-only once loaded.
     */

    // Aliases
    private final ObjectArrayList<AliasStruct> aliases;
    private final Object2IntOpenHashMap<String> aliasName2AliasIndex;

    // Constructor \\

    public AliasLibraryStruct() {
        this.aliases = new ObjectArrayList<>();
        this.aliasName2AliasIndex = new Object2IntOpenHashMap<>();
        this.aliasName2AliasIndex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
    }

    // Management \\

    void addAlias(AliasStruct alias, ObjectArrayList<String> names) {

        int index = aliases.size();
        aliases.add(alias);
        aliasName2AliasIndex.put(alias.getAliasType().toLowerCase(), index);

        for (int i = 0; i < names.size(); i++)
            aliasName2AliasIndex.put(names.get(i).toLowerCase(), index);
    }

    // Accessible \\

    public int getAliasCount() {
        return aliases.size();
    }

    public AliasStruct getAlias(int index) {
        return aliases.get(index);
    }

    // The alias a name selects, null when none does
    public AliasStruct findAlias(String name) {

        int index = aliasName2AliasIndex.getInt(name.toLowerCase());
        return index != EngineSetting.INDEX_NOT_FOUND ? aliases.get(index) : null;
    }
}
