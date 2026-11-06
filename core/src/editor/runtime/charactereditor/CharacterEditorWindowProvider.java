package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;

public interface CharacterEditorWindowProvider {

    /*
     * Names what one character viewport tab is for: the mode it opens in,
     * which also decides the modes it keeps to — those that take tools in
     * the Character Mesh Editor, the rest in the Character Editor — and the
     * toolbar it shows, so the systems both tabs share work their own way.
     */

    CharacterEditorMode getStartingMode();

    String getToolbarMenu();
}
