package editor.bootstrap.propertiespipeline.properties;

import editor.bootstrap.propertiespipeline.util.PaletteContent;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface MapsProvider extends PaletteProvider {

    /*
     * Names what the shared Palette tab lists under the colour wheel while
     * this window is the one worked in last among those that have a palette
     * (the tab's title names the window, through ActiveWindowManager): the
     * revision that
     * moves whenever the list may have changed, the rows themselves — every
     * map the image takes, the one picked, its settings, Generate and
     * Clear — the row templates of the tool's menu, whose
     * buttons route back to the tool's branch, and the steps its number
     * rows' sliders ask for. The Image Editor and the character tools list
     * their maps through it alike.
     */

    int getMapsRevision();

    void buildMapsRows(ObjectArrayList<CharacterViewRowStruct> rows);

    CharacterViewTemplateStruct getMapsTemplates();

    void stepMapsRow(String key, int steps);

    // A window whose image carries maps offers the colour wheel over them
    @Override
    default PaletteContent getPaletteContent() {
        return PaletteContent.IMAGE;
    }
}
