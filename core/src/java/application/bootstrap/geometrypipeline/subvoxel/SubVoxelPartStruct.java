package application.bootstrap.geometrypipeline.subvoxel;

import engine.root.StructPackage;

public class SubVoxelPartStruct extends StructPackage {

    /*
     * One named part of a sub-voxel model. Every cell assigned to the part
     * renders with the part's texture.
     */

    // Identity
    private String partName;

    // Render
    private String textureName;

    // Constructor \\

    public SubVoxelPartStruct(String partName, String textureName) {

        // Identity
        this.partName = partName;

        // Render
        this.textureName = textureName;
    }

    // Accessible \\

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public String getTextureName() {
        return textureName;
    }

    public void setTextureName(String textureName) {
        this.textureName = textureName;
    }
}
