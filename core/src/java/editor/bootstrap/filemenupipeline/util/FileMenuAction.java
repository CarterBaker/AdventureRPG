package editor.bootstrap.filemenupipeline.util;

public enum FileMenuAction {

    /*
     * Every operation the editor's File and Edit menus offer, each run on
     * the window worked in last exactly as that tool's own key or button for
     * it runs it. The File menu holds the first seven and the two model
     * actions, the Edit menu Undo and Redo. New is the one way to make
     * something, Open picks a workspace file in the system's own dialog. The
     * ordinal indexes each tool's table and the menu's labels, so the order
     * here and in EditorSetting.FILE_MENU_LABELS is one.
     */

    NEW,
    OPEN,
    DUPLICATE,
    SAVE,
    SAVE_ALL,
    REVERT,
    DELETE,
    UNDO,
    REDO,
    IMPORT,
    EXPORT;

    // Values
    public static final FileMenuAction[] VALUES = values();
}
