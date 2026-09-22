package application.bootstrap.worldpipeline.structure;

import engine.root.StructPackage;

public class StructureTemplatePieceStruct extends StructPackage {

    /*
     * One template stamped at one spot: the world block its rotated
     * footprint's minimum corner lands on, the world Y its bottom layer sits
     * at, and its quarter-turn rotation. Rotation is clockwise looking down,
     * so the template's front (its z = 0 face, looking toward -Z) looks
     * toward -Z, +X, +Z, -X for rotations 0 through 3. originX/originZ are
     * not wrapped — they stay beside the placement that produced them, and
     * the stamper resolves them against a chunk with a wrapped delta.
     */

    private final StructureTemplateData templateData;
    private final long originX;
    private final long originZ;
    private final int originY;
    private final int rotation;

    public StructureTemplatePieceStruct(
            StructureTemplateData templateData,
            long originX, long originZ, int originY,
            int rotation) {

        this.templateData = templateData;
        this.originX = originX;
        this.originZ = originZ;
        this.originY = originY;
        this.rotation = rotation & 3;
    }

    // Rotation \\

    public static int footprintX(StructureTemplateData template, int rotation) {
        return (rotation & 1) == 0 ? template.getSizeX() : template.getSizeZ();
    }

    public static int footprintZ(StructureTemplateData template, int rotation) {
        return (rotation & 1) == 0 ? template.getSizeZ() : template.getSizeX();
    }

    /*
     * Template-local X of the cell that lands on footprint cell (rx, rz).
     * The inverse of rotating the template clockwise a quarter turn per
     * step, so a footprint walk reads each template cell exactly once.
     */
    public static int templateX(StructureTemplateData template, int rotation, int rx, int rz) {
        switch (rotation & 3) {
            case 1:
                return rz;
            case 2:
                return template.getSizeX() - 1 - rx;
            case 3:
                return template.getSizeX() - 1 - rz;
            default:
                return rx;
        }
    }

    public static int templateZ(StructureTemplateData template, int rotation, int rx, int rz) {
        switch (rotation & 3) {
            case 1:
                return template.getSizeZ() - 1 - rx;
            case 2:
                return template.getSizeZ() - 1 - rz;
            case 3:
                return rx;
            default:
                return rz;
        }
    }

    /*
     * Footprint-local position of template cell (tx, tz) — the forward
     * rotation, used to find where the anchor lands.
     */
    public static int footprintXOf(StructureTemplateData template, int rotation, int tx, int tz) {
        switch (rotation & 3) {
            case 1:
                return template.getSizeZ() - 1 - tz;
            case 2:
                return template.getSizeX() - 1 - tx;
            case 3:
                return tz;
            default:
                return tx;
        }
    }

    public static int footprintZOf(StructureTemplateData template, int rotation, int tx, int tz) {
        switch (rotation & 3) {
            case 1:
                return tx;
            case 2:
                return template.getSizeZ() - 1 - tz;
            case 3:
                return template.getSizeX() - 1 - tx;
            default:
                return tz;
        }
    }

    // Front direction for a rotation, as a unit step in X and Z.
    public static int frontX(int rotation) {
        switch (rotation & 3) {
            case 1:
                return 1;
            case 3:
                return -1;
            default:
                return 0;
        }
    }

    public static int frontZ(int rotation) {
        switch (rotation & 3) {
            case 0:
                return -1;
            case 2:
                return 1;
            default:
                return 0;
        }
    }

    // Rotation whose front looks along (dx, dz), one of the four axes.
    public static int rotationFacing(int dx, int dz) {
        if (dx > 0)
            return 1;
        if (dx < 0)
            return 3;
        if (dz > 0)
            return 2;
        return 0;
    }

    // Accessible \\

    public StructureTemplateData getTemplateData() {
        return templateData;
    }

    public long getOriginX() {
        return originX;
    }

    public long getOriginZ() {
        return originZ;
    }

    public int getOriginY() {
        return originY;
    }

    public int getRotation() {
        return rotation;
    }

    public int getFootprintX() {
        return footprintX(templateData, rotation);
    }

    public int getFootprintZ() {
        return footprintZ(templateData, rotation);
    }
}
