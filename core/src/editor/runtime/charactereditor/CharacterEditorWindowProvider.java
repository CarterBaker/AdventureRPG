package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;

public interface CharacterEditorWindowProvider {

    /*
     * Names what one character viewport tab is for: the mode it opens in,
     * the modes it keeps to — Appearance, Build and Equipment each in a tab
     * of its own, the modes that take tools in the Character Mesh Editor —
     * and the toolbar it shows, so the systems every such tab shares work
     * its way.
     */

    CharacterEditorMode getStartingMode();

    boolean holdsMode(CharacterEditorMode mode);

    String getToolbarMenu();
}
