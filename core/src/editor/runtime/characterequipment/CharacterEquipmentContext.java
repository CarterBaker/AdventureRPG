package editor.runtime.characterequipment;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.charactereditor.CharacterLookContext;

public class CharacterEquipmentContext extends CharacterLookContext {

    /*
     * Editor tab for the anchors worn and held gear hangs from: each
     * anchor's bone, position, turn and size, on the live preview with every
     * anchor marked and the picked one highlighted. Everything a Character
     * Editor tab does, and every value it shows in the Properties tab, is
     * CharacterLookContext's.
     */

    // Window \\

    @Override
    public CharacterEditorMode getStartingMode() {
        return CharacterEditorMode.EQUIPMENT;
    }
}
