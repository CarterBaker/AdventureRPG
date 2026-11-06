package editor.runtime.characterview.panel;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface CharacterViewRowSource extends CharacterViewStepTarget {

    /*
     * Supplies the rows one character tool's docker window shows: its title,
     * the revision that moves whenever what it lists may have changed, the
     * rows themselves, and the row templates of the tool's menu, whose
     * buttons route back to the tool's branch; and takes the steps its number
     * rows' sliders ask for.
     */

    int getRowRevision();

    String getRowTitle();

    void buildRows(ObjectArrayList<CharacterViewRowStruct> rows);

    CharacterViewTemplateStruct getRowTemplates();
}
