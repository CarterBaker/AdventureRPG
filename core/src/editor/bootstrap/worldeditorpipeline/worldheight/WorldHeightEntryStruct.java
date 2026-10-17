package editor.bootstrap.worldeditorpipeline.worldheight;

import engine.root.StructPackage;

public class WorldHeightEntryStruct extends StructPackage {

    /*
     * One level the height layer can paint: the grey level itself, black the
     * lowest and white the highest, and the name shown for it.
     */

    // Identity
    private final int level;
    private final String displayName;

    // Constructor \\

    public WorldHeightEntryStruct(int level, String displayName) {

        // Identity
        this.level = level;
        this.displayName = displayName;
    }

    // Accessible \\

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }
}
