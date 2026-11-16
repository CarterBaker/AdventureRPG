package editor.bootstrap.propertiespipeline.properties;

import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface MapsProvider {

    /*
     * Names what the shared Maps tab lists while this window is the one
     * worked in last among those whose image carries maps: its title, the
     * revision that moves whenever the list may have changed, the rows
     * themselves — every map the image takes, the one picked, its settings,
     * Generate and Clear — the row templates of the tool's menu, whose
     * buttons route back to the tool's branch, and the steps its number
     * rows' sliders ask for. The Image Editor and the character tools list
     * their maps through it alike.
     */

    int getMapsRevision();

    String getMapsTitle();

    void buildMapsRows(ObjectArrayList<CharacterViewRowStruct> rows);

    CharacterViewTemplateStruct getMapsTemplates();

    void stepMapsRow(String key, int steps);
}
