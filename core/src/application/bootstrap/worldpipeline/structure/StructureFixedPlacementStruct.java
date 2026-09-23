package application.bootstrap.worldpipeline.structure;

import engine.root.StructPackage;

public class StructureFixedPlacementStruct extends StructPackage {

    /*
     * One hand-placed copy at an exact world column. Without a Y the origin
     * snaps onto the ground plus the structure's Y offset. Rules are ignored
     * unless enforceRules is set.
     */

    private final long worldX;
    private final long worldZ;
    private final int worldY;
    private final boolean hasWorldY;
    private final int quarterTurns;
    private final boolean enforceRules;

    public StructureFixedPlacementStruct(
            long worldX,
            long worldZ,
            int worldY,
            boolean hasWorldY,
            int quarterTurns,
            boolean enforceRules) {

        this.worldX = worldX;
        this.worldZ = worldZ;
        this.worldY = worldY;
        this.hasWorldY = hasWorldY;
        this.quarterTurns = quarterTurns;
        this.enforceRules = enforceRules;
    }

    public long getWorldX() {
        return worldX;
    }

    public long getWorldZ() {
        return worldZ;
    }

    public int getWorldY() {
        return worldY;
    }

    public boolean hasWorldY() {
        return hasWorldY;
    }

    public int getQuarterTurns() {
        return quarterTurns;
    }

    public boolean isEnforceRules() {
        return enforceRules;
    }
}
