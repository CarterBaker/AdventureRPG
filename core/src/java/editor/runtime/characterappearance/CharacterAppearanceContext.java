package editor.runtime.characterappearance;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.charactereditor.CharacterLookContext;

public class CharacterAppearanceContext extends CharacterLookContext {

    /*
     * Editor tab for a character's appearance: its default features, the
     * skin and hair colors and palettes it wears and the tone picked from
     * each, on the live preview. Everything a Character Editor tab does, and
     * every value it shows in the Properties tab, is CharacterLookContext's.
     */

    // Window \\

    @Override
    public CharacterEditorMode getStartingMode() {
        return CharacterEditorMode.APPEARANCE;
    }
}
