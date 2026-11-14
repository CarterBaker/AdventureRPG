package editor.bootstrap.toolpipeline.tool;

import engine.root.StructPackage;

public class ToolActionGroupStruct extends StructPackage {

    /*
     * A titled run of the actions a canvas lists in the Tools panel under its
     * tools, such as a mesh's selection or geometry operations.
     */

    // Group
    private final String title;
    private final ToolActionStruct[] actions;

    // Constructor \\

    public ToolActionGroupStruct(String title, ToolActionStruct... actions) {

        // Group
        this.title = title;
        this.actions = actions;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }

    public ToolActionStruct[] getActions() {
        return actions;
    }
}
