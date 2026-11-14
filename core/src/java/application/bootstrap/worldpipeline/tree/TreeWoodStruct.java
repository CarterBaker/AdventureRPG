package application.bootstrap.worldpipeline.tree;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeWoodStruct extends StructPackage {

    /*
     * A species' wood: the bark texture its surface is drawn with and the
     * heartwood texture a cut lays open, the bark's tint and how many
     * sub-voxels deep it runs, the tool type and tier that cut it, and the
     * logs it splits into once felled, thickest first.
     */

    private final String barkTextureName;
    private final String woodTextureName;
    private final int barkColor;
    private final int barkSubVoxels;
    private final String toolTypeName;
    private final short toolTypeID;
    private final int toolTier;
    private final ObjectArrayList<TreeLogStruct> logs;

    public TreeWoodStruct(
            String barkTextureName,
            String woodTextureName,
            int barkColor,
            int barkSubVoxels,
            String toolTypeName,
            short toolTypeID,
            int toolTier,
            ObjectArrayList<TreeLogStruct> logs) {

        this.barkTextureName = barkTextureName;
        this.woodTextureName = woodTextureName;
        this.barkColor = barkColor;
        this.barkSubVoxels = barkSubVoxels;
        this.toolTypeName = toolTypeName;
        this.toolTypeID = toolTypeID;
        this.toolTier = toolTier;
        this.logs = logs;
    }

    // Accessible \\

    public String getBarkTextureName() {
        return barkTextureName;
    }

    public String getWoodTextureName() {
        return woodTextureName;
    }

    public int getBarkColor() {
        return barkColor;
    }

    public int getBarkSubVoxels() {
        return barkSubVoxels;
    }

    public String getToolTypeName() {
        return toolTypeName;
    }

    public short getToolTypeID() {
        return toolTypeID;
    }

    public int getToolTier() {
        return toolTier;
    }

    public ObjectArrayList<TreeLogStruct> getLogs() {
        return logs;
    }
}
