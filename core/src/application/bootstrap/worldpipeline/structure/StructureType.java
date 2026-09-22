package application.bootstrap.worldpipeline.structure;

public enum StructureType {

    /*
     * What a structure definition is and how StructureManager resolves it
     * into blocks. STRUCTURE is a single hand-authored block template.
     * SETTLEMENT and DUNGEON are both layouts — a generated street or
     * corridor network with child structures placed along its sides and in
     * the space between — and differ only in their defaults: a dungeon is
     * expected underground with tunnelled corridors, a settlement on the
     * surface with streets that follow the terrain. ROAD is never placed on
     * its own; it describes the blocks a path is built from and is referenced
     * by the road network between points of interest and by layouts for
     * their internal streets. placementPriority breaks overlaps between two
     * candidate placements — the higher priority keeps its spot.
     */

    ROAD(0),
    STRUCTURE(1),
    DUNGEON(2),
    SETTLEMENT(3);

    public static final StructureType[] VALUES = values();
    public static final int LENGTH = VALUES.length;

    public final int placementPriority;

    StructureType(int placementPriority) {
        this.placementPriority = placementPriority;
    }

    public boolean isLayout() {
        return this == SETTLEMENT || this == DUNGEON;
    }

    public boolean isPlaceable() {
        return this != ROAD;
    }
}
