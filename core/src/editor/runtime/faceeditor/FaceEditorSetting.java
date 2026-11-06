package editor.runtime.faceeditor;

public class FaceEditorSetting {

    /*
     * Constants used only by FaceEditorContext — its toolbar menu and the
     * held shortcut its bone drags snap with. Its panels' rows are the docker
     * windows'.
     */

    // Menus
    public static final String MENU_TOOLBAR = "editor/FaceEditor/Toolbar";
    public static final int ENTRY_STATUS = 0;

    // Gesture — the held shortcut a bone drag snaps with, named in shortcuts/FaceEditor
    public static final String SHORTCUT_SNAP = "FaceEditor/Snap";
}
