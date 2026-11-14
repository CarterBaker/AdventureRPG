package editor.bootstrap.worldeditorpipeline.worldbiome;

import engine.root.StructPackage;

public class WorldBiomeEntryStruct extends StructPackage {

    /*
     * One biome the world image can paint: its registry name, the name shown
     * for it, and the 0xRRGGBB map color that paints it.
     */

    // Identity
    private final String biomeName;
    private final String displayName;

    // Color
    private final int mapColor;

    // Constructor \\

    public WorldBiomeEntryStruct(String biomeName, String displayName, int mapColor) {

        // Identity
        this.biomeName = biomeName;
        this.displayName = displayName;

        // Color
        this.mapColor = mapColor;
    }

    // Accessible \\

    public String getBiomeName() {
        return biomeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMapColor() {
        return mapColor;
    }
}
