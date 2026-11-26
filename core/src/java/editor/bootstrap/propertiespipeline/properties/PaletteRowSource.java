package editor.bootstrap.propertiespipeline.properties;

import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface PaletteRowSource extends PaletteProvider {

    /*
     * A window whose palette is a list of rows: what its tools pick from (the
     * parts, bones, gear, nodes or maps of what it works on, the one picked
     * marked), listed in the shared Palette tab under the colour wheel when
     * the window paints and alone otherwise, while the Properties tab holds
     * the settings of what is picked. It names the revision that moves
     * whenever the list may have changed, the rows themselves, the row
     * templates of the tool's menu, whose buttons route back to the tool's
     * branch, and takes the steps its number rows' sliders ask for.
     */

    int getPaletteRevision();

    void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows);

    CharacterViewTemplateStruct getPaletteTemplates();

    void stepPaletteRow(String key, int steps);
}
