package editor.bootstrap.tabpipeline.util;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import engine.root.StructPackage;

public class DropTargetStruct extends StructPackage {

    /*
     * Resolved drop destination for the current drag frame. Holds the OS window
     * the cursor is over, the BSP leaf under the cursor, and which quadrant of
     * that leaf the cursor occupies. Null leaf means the cursor is over an OS
     * window but outside any valid leaf — treated as a no-drop frame. Null
     * instance means the cursor is outside all OS windows — drop opens a new one.
     */

    // Target
    private WindowInstance window;
    private DockNodeStruct leaf;
    private DropZone zone;

    // Internal \\

    public DropTargetStruct(WindowInstance window, DockNodeStruct leaf, DropZone zone) {

        // Target
        this.window = window;
        this.leaf = leaf;
        this.zone = zone;
    }

    // Accessible \\

    public WindowInstance getWindow() {
        return window;
    }

    public DockNodeStruct getLeaf() {
        return leaf;
    }

    public DropZone getZone() {
        return zone;
    }

    public boolean matches(DropTargetStruct other) {

        if (other == null)
            return false;

        return window == other.window
                && leaf == other.leaf
                && zone == other.zone;
    }
}