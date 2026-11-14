package application.bootstrap.worldpipeline.structure;

import engine.root.StructPackage;

public class StructureFrequencyStruct extends StructPackage {

    /*
     * How often a structure generates on its own. Each spacing-wide cell rolls
     * chance once and holds at most one anchor, kept separation / 2 clear of
     * every cell edge so two copies never sit closer than the separation.
     */

    private final float chance;
    private final int spacingBlocks;
    private final int separationBlocks;
    private final boolean randomRotation;

    public StructureFrequencyStruct(
            float chance,
            int spacingBlocks,
            int separationBlocks,
            boolean randomRotation) {

        this.chance = chance;
        this.spacingBlocks = spacingBlocks;
        this.separationBlocks = separationBlocks;
        this.randomRotation = randomRotation;
    }

    public float getChance() {
        return chance;
    }

    public int getSpacingBlocks() {
        return spacingBlocks;
    }

    public int getSeparationBlocks() {
        return separationBlocks;
    }

    public boolean hasRandomRotation() {
        return randomRotation;
    }
}
