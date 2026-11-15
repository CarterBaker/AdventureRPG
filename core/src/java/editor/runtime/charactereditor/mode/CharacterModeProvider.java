package editor.runtime.charactereditor.mode;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;

public interface CharacterModeProvider {

    /*
     * A window that works the character in one mode: every character tool
     * tab, and the UV Editor, which works its UVs in the UV mode. Its
     * CharacterEditorModeSystem reads the mode here and hands it, with the
     * region it works on, to the editor while the window is active.
     */

    CharacterEditorMode getMode();
}
