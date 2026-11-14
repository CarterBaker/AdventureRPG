package editor.bootstrap.toolpipeline.tool;

import engine.root.StructPackage;

public class ToolActionStruct extends StructPackage {

    /*
     * One action a canvas lists in the Tools panel: the name its row shows
     * and the key its window runs it by, the same key the window's own button
     * and shortcut for it carry, so all three do exactly the same thing.
     */

    // Action
    private final String label;
    private final String key;

    // Constructor \\

    public ToolActionStruct(String label, String key) {

        // Action
        this.label = label;
        this.key = key;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public String getKey() {
        return key;
    }
}
