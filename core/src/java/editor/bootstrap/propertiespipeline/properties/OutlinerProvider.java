package editor.bootstrap.propertiespipeline.properties;

import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface OutlinerProvider {

    /*
     * Names what the shared Character Outliner lists while this window is the
     * one worked in last, which may change as the window changes layer: its
     * title, the revision that moves whenever the list may have changed, the
     * rows themselves, the row templates of the tool's menu, whose buttons
     * route back to the tool's branch, and the steps its number rows'
     * sliders ask for.
     */

    int getOutlinerRevision();

    String getOutlinerTitle();

    void buildOutlinerRows(ObjectArrayList<CharacterViewRowStruct> rows);

    CharacterViewTemplateStruct getOutlinerTemplates();

    void stepOutlinerRow(String key, int steps);
}
