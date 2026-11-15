package editor.bootstrap.filemenupipeline.util;

public enum FileMenuAction {

    /*
     * Every operation the editor's File and Edit menus offer, each run on
     * the window worked in last exactly as that tool's own key or button for
     * it runs it. The File menu holds the first seven, the two model actions
     * and Generate Maps, the Edit menu Undo and Redo. The ordinal indexes each tool's
     * table and the menu's labels, so the order is fixed and new actions are
     * added at the end.
     */

    NEW,
    NEW_FILE,
    DUPLICATE,
    SAVE,
    SAVE_ALL,
    REVERT,
    DELETE,
    UNDO,
    REDO,
    IMPORT_MODEL,
    EXPORT_MODEL,
    GENERATE_MAPS;

    // Values
    public static final FileMenuAction[] VALUES = values();
}
