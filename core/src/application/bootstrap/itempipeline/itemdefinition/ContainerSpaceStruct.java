package application.bootstrap.itempipeline.itemdefinition;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3Int;

public class ContainerSpaceStruct extends StructPackage {

    /*
     * The space a container item holds, measured in sub-voxels. A space with
     * an offset lies inside the item's own model grid at that cell, so a
     * chest's contents rest inside the chest itself. A space without one is a
     * pocket: it is bigger than the item looks, so it is only ever shown in
     * the inventory's menus, as its own open box of walls textured with the
     * pocket texture.
     */

    // Size
    private final Vector3Int size;

    // Placement — the space's first cell inside the item's model grid, null for a pocket
    private final Vector3Int offset;

    // Pocket
    private final String pocketTextureName;

    // Constructor \\

    public ContainerSpaceStruct(Vector3Int size, Vector3Int offset, String pocketTextureName) {

        // Size
        this.size = size;

        // Placement
        this.offset = offset;

        // Pocket
        this.pocketTextureName = pocketTextureName;
    }

    // Accessible \\

    public Vector3Int getSize() {
        return size;
    }

    public boolean isPocket() {
        return offset == null;
    }

    public Vector3Int getOffset() {
        return offset;
    }

    public String getPocketTextureName() {
        return pocketTextureName;
    }
}
