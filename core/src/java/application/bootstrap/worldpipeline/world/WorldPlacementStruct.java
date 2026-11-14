package application.bootstrap.worldpipeline.world;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class WorldPlacementStruct extends StructPackage {

    /*
     * One hand-picked thing a world stands up at an exact block column,
     * persisted by name in the world's companion file: a settlement or a
     * structure, the architecture a settlement is built in, empty to take the
     * one its biome picks, the numbered layout a settlement is planned from,
     * 0 to take the one its cell would grow, and the clockwise quarter turns
     * a structure takes. A camera is named only to tell it apart, and
     * carries its view instead: how many blocks above the ground of its
     * column it looks out from, and its yaw and pitch in degrees, yaw turning
     * from +z toward +x and pitch raising the view. A hand-placed settlement
     * replaces whatever would have grown in its cell, and ignores every rule
     * a grown one keeps.
     */

    // Identity
    private final WorldPlacementKind kind;
    private final String name;
    private final String architectureName;
    private final int layout;

    // Position
    private final long worldX;
    private final long worldZ;
    private final int quarterTurns;

    // View
    private final float height;
    private final float yaw;
    private final float pitch;

    // Constructor \\

    public WorldPlacementStruct(
            WorldPlacementKind kind,
            String name,
            String architectureName,
            int layout,
            long worldX,
            long worldZ,
            int quarterTurns) {
        this(kind, name, architectureName, layout, worldX, worldZ, quarterTurns, 0f, 0f, 0f);
    }

    public WorldPlacementStruct(
            WorldPlacementKind kind,
            String name,
            String architectureName,
            int layout,
            long worldX,
            long worldZ,
            int quarterTurns,
            float height,
            float yaw,
            float pitch) {

        // Identity
        this.kind = kind;
        this.name = name;
        this.architectureName = architectureName;
        this.layout = layout;

        // Position
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.quarterTurns = quarterTurns;

        // View
        this.height = height;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    // Copy \\

    // The same placement stood on another block column, everything else kept
    public WorldPlacementStruct moveTo(long movedX, long movedZ) {
        return new WorldPlacementStruct(
                kind, name, architectureName, layout, movedX, movedZ, quarterTurns, height, yaw, pitch);
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

    public int getLayout() {
        return layout;
    }

    public boolean hasLayout() {
        return layout != EngineSetting.WORLD_PLACEMENT_LAYOUT_CELL;
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

    public float getHeight() {
        return height;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
