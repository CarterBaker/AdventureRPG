package editor.bootstrap.worldeditorpipeline.worldplacement;

import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import engine.root.StructPackage;

public class WorldPlacementEntryStruct extends StructPackage {

    /*
     * One thing the world editor can place by hand: whether it is a
     * settlement or a single structure, its registry name, and the name shown
     * for it.
     */

    // Identity
    private final WorldPlacementKind kind;
    private final String name;
    private final String displayName;

    // Constructor \\

    public WorldPlacementEntryStruct(WorldPlacementKind kind, String name, String displayName) {

        // Identity
        this.kind = kind;
        this.name = name;
        this.displayName = displayName;
    }

    // Accessible \\

    public WorldPlacementKind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }
}
