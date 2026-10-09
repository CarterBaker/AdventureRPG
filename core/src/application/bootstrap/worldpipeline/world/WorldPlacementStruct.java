package application.bootstrap.worldpipeline.world;

import engine.root.StructPackage;

public class WorldPlacementStruct extends StructPackage {

    /*
     * One hand-picked thing a world stands up at an exact block column,
     * persisted by name in the world's companion file: a settlement or a
     * structure, the architecture a settlement is built in, empty to take the
     * one its biome picks, and the clockwise quarter turns a structure takes.
     * A hand-placed settlement replaces whatever would have grown in its
     * cell, and ignores every rule a grown one keeps.
     */

    // Identity
    private final WorldPlacementKind kind;
    private final String name;
    private final String architectureName;

    // Position
    private final long worldX;
    private final long worldZ;
    private final int quarterTurns;

    // Constructor \\

    public WorldPlacementStruct(
            WorldPlacementKind kind,
            String name,
            String architectureName,
            long worldX,
            long worldZ,
            int quarterTurns) {

        // Identity
        this.kind = kind;
        this.name = name;
        this.architectureName = architectureName;

        // Position
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.quarterTurns = quarterTurns;
    }

    // Accessible \\

    public WorldPlacementKind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public String getArchitectureName() {
        return architectureName;
    }

    public boolean hasArchitecture() {
        return !architectureName.isEmpty();
    }

    public long getWorldX() {
        return worldX;
    }

    public long getWorldZ() {
        return worldZ;
    }

    public int getQuarterTurns() {
        return quarterTurns;
    }
}
