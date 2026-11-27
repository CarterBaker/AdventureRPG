package editor.bootstrap.propertiespipeline.util;

import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class PaletteRowUtility extends EngineUtility {

    /*
     * The one way a tool lists content in the Palette tab: every entry of a
     * schema as a row named by the entry, its key the entry's name behind
     * the prefix the tool's branch reads it back by, the entry chosen marked.
     */

    // Rows \\

    public static void addEntryRows(
            InfoManager infoManager,
            ObjectArrayList<CharacterViewRowStruct> rows,
            String schemaName,
            String prefix,
            String chosen) {

        ObjectArrayList<InfoEntryStruct> entries = infoManager.getEntries(schemaName);

        for (int i = 0; i < entries.size(); i++) {

            String name = entries.get(i).getEntryName();
            rows.add(CharacterViewRowStruct.item(prefix + name, name, name.equals(chosen)));
        }
    }
}
