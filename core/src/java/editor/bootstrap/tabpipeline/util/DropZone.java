package editor.bootstrap.tabpipeline.util;

public enum DropZone {

    /*
     * Where a dragged tab lands relative to the leaf it is dropped on: split
     * off to one side, or stacked into the leaf's own tabs.
     */

    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    CENTER
}