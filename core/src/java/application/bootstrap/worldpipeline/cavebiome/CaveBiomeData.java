package application.bootstrap.worldpipeline.cavebiome;

import application.bootstrap.worldpipeline.biome.BiomeCoveringStruct;
import application.bootstrap.worldpipeline.biome.BiomeVeinStruct;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CaveBiomeData extends DataPackage {

    /*
     * Persistent cave biome record: registry and display names, the rock it
     * lines its caves with and how deep that lining runs into the walls, the
     * blocks its floors, ceilings and still water beds are made of, the
     * coverings it lays over floors, walls and ceilings, the stalactites and
     * stalagmites it grows and the mineral veins threading its rock. A null
     * covering lays none.
     */

    private final String caveBiomeName;
    private final String displayName;
    private final short caveBiomeID;

    private final String rockBlockName;
    private final String floorBlockName;
    private final String ceilingBlockName;
    private final String bedBlockName;
    private final int shellBlocks;

    private final BiomeCoveringStruct floorCovering;
    private final BiomeCoveringStruct wallCovering;
    private final BiomeCoveringStruct ceilingCovering;

    private final CaveSpeleothemStruct speleothems;
    private final ObjectArrayList<BiomeVeinStruct> veins;

    public CaveBiomeData(
            String caveBiomeName,
            String displayName,
            short caveBiomeID,
            String rockBlockName,
            String floorBlockName,
            String ceilingBlockName,
            String bedBlockName,
            int shellBlocks,
            BiomeCoveringStruct floorCovering,
            BiomeCoveringStruct wallCovering,
            BiomeCoveringStruct ceilingCovering,
            CaveSpeleothemStruct speleothems,
            ObjectArrayList<BiomeVeinStruct> veins) {

        this.caveBiomeName = caveBiomeName;
        this.displayName = displayName;
        this.caveBiomeID = caveBiomeID;

        this.rockBlockName = rockBlockName;
        this.floorBlockName = floorBlockName;
        this.ceilingBlockName = ceilingBlockName;
        this.bedBlockName = bedBlockName;
        this.shellBlocks = shellBlocks;

        this.floorCovering = floorCovering;
        this.wallCovering = wallCovering;
        this.ceilingCovering = ceilingCovering;

        this.speleothems = speleothems;
        this.veins = veins;
    }

    // Accessible \\

    public String getCaveBiomeName() {
        return caveBiomeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public short getCaveBiomeID() {
        return caveBiomeID;
    }

    public String getRockBlockName() {
        return rockBlockName;
    }

    public String getFloorBlockName() {
        return floorBlockName;
    }

    public String getCeilingBlockName() {
        return ceilingBlockName;
    }

    public String getBedBlockName() {
        return bedBlockName;
    }

    public int getShellBlocks() {
        return shellBlocks;
    }

    public BiomeCoveringStruct getFloorCovering() {
        return floorCovering;
    }

    public BiomeCoveringStruct getWallCovering() {
        return wallCovering;
    }

    public BiomeCoveringStruct getCeilingCovering() {
        return ceilingCovering;
    }

    public CaveSpeleothemStruct getSpeleothems() {
        return speleothems;
    }

    public ObjectArrayList<BiomeVeinStruct> getVeins() {
        return veins;
    }
}
