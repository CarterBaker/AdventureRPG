package editor.runtime.animationeditor;

public class AnimationEditorSetting {

    /*
     * Constants used only by AnimationEditorContext — its toolbar menu and
     * the held shortcut its pose drags snap with. Its panels' rows and its
     * timeline are the docker windows'.
     */

    // Menus
    public static final String MENU_TOOLBAR = "editor/AnimationEditor/Toolbar";
    public static final int ENTRY_STATUS = 0;

    // Pose — the held shortcut a pose drag snaps with, named in shortcuts/AnimationEditor
    public static final String SHORTCUT_SNAP = "AnimationEditor/Snap";
}
