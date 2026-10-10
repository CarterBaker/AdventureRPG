package application.bootstrap.worldpipeline.biomemanager;

import java.io.File;

import application.bootstrap.worldpipeline.biome.BiomeCaveBiomeStruct;
import application.bootstrap.worldpipeline.biome.BiomeData;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.BiomeTreeStruct;
import application.bootstrap.worldpipeline.cavebiomemanager.CaveBiomeManager;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import engine.root.BuilderPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class BiomeBuilder extends BuilderPackage {

    /*
     * Parses biome ARPG into BiomeData wrapped in a BiomeHandle through
     * BiomeArpgUtility, the one definition of the format, under the ID
     * BiomeManager assigns its name. Everything is validated at load, so a
     * malformed biome fails at boot — every tree it grows and every cave
     * biome it holds beneath it included, each loaded on demand if it is not
     * yet.
     */

    // Internal
    private BiomeManager biomeManager;
    private TreeManager treeManager;
    private CaveBiomeManager caveBiomeManager;

    // Base \\

    @Override
    protected void get() {
        this.biomeManager = get(BiomeManager.class);
        this.treeManager = get(TreeManager.class);
        this.caveBiomeManager = get(CaveBiomeManager.class);
    }

    // Build \\

    BiomeHandle build(File file, File root) {

        String biomeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        BiomeData biomeData;

        try {
            biomeData = BiomeArpgUtility.parse(biomeName, biomeManager.registerBiomeName(biomeName), arpg);
        } catch (InternalException e) {
            return throwException(e.getMessage(), e.getCause());
        }

        ObjectArrayList<BiomeTreeStruct> trees = biomeData.getTrees();

        for (int i = 0; i < trees.size(); i++)
            treeManager.getTreeHandleFromTreeName(trees.get(i).getTreeName());

        ObjectArrayList<BiomeCaveBiomeStruct> caveBiomes = biomeData.getCaveBiomes();

        for (int i = 0; i < caveBiomes.size(); i++)
            caveBiomeManager.getCaveBiomeHandleFromCaveBiomeName(caveBiomes.get(i).getCaveBiomeName());

        BiomeHandle biomeHandle = create(BiomeHandle.class);
        biomeHandle.constructor(biomeData);

        return biomeHandle;
    }
}
