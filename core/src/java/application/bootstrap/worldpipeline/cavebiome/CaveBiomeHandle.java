package application.bootstrap.worldpipeline.cavebiome;

import application.bootstrap.worldpipeline.biome.BiomeCoveringStruct;
import application.bootstrap.worldpipeline.biome.BiomeVeinStruct;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CaveBiomeHandle extends HandlePackage {

    /*
     * Persistent cave biome record. Wraps CaveBiomeData and delegates all
     * access through it. Registered in CaveBiomeManager from bootstrap to
     * shutdown.
     */

    // Internal
    private CaveBiomeData caveBiomeData;

    // Constructor \\

    public void constructor(CaveBiomeData caveBiomeData) {
        this.caveBiomeData = caveBiomeData;
    }

    // Accessible \\

    public CaveBiomeData getCaveBiomeData() {
        return caveBiomeData;
    }

    public String getCaveBiomeName() {
        return caveBiomeData.getCaveBiomeName();
    }

    public String getDisplayName() {
        return caveBiomeData.getDisplayName();
    }

    public short getCaveBiomeID() {
        return caveBiomeData.getCaveBiomeID();
    }

    public String getRockBlockName() {
        return caveBiomeData.getRockBlockName();
    }

    public String getFloorBlockName() {
        return caveBiomeData.getFloorBlockName();
    }

    public String getCeilingBlockName() {
        return caveBiomeData.getCeilingBlockName();
    }

    public String getBedBlockName() {
        return caveBiomeData.getBedBlockName();
    }

    public int getShellBlocks() {
        return caveBiomeData.getShellBlocks();
    }

    public BiomeCoveringStruct getFloorCovering() {
        return caveBiomeData.getFloorCovering();
    }

    public BiomeCoveringStruct getWallCovering() {
        return caveBiomeData.getWallCovering();
    }

    public BiomeCoveringStruct getCeilingCovering() {
        return caveBiomeData.getCeilingCovering();
    }

    public CaveSpeleothemStruct getSpeleothems() {
        return caveBiomeData.getSpeleothems();
    }

    public ObjectArrayList<BiomeVeinStruct> getVeins() {
        return caveBiomeData.getVeins();
    }
}
