package application.bootstrap.worldpipeline.biome;

import engine.root.StructPackage;

public class BiomeCoveringStruct extends StructPackage {

    /*
     * A covering a biome lays over one kind of ground as it generates: the
     * covering, the level it grows to, and how many levels below that a
     * column may fall short, drawn per column so the covering thins and
     * thickens across the land instead of lying as one flat sheet.
     */

    private final String coveringName;
    private final int level;
    private final int variance;

    public BiomeCoveringStruct(String coveringName, int level, int variance) {
        this.coveringName = coveringName;
        this.level = level;
        this.variance = variance;
    }

    // Accessible \\

    public String getCoveringName() {
        return coveringName;
    }

    public int getLevel() {
        return level;
    }

    public int getVariance() {
        return variance;
    }
}
