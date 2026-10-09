package application.bootstrap.worldpipeline.biomemanager;

import java.io.File;

import application.bootstrap.worldpipeline.biome.BiomeData;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import engine.root.BuilderPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;

class BiomeBuilder extends BuilderPackage {

    /*
     * Parses biome ARPG into BiomeData wrapped in a BiomeHandle through
     * BiomeArpgUtility, the one definition of the format, under the ID
     * BiomeManager assigns its name. Everything is validated at load, so a
     * malformed biome fails at boot.
     */

    // Internal
    private BiomeManager biomeManager;

    // Base \\

    @Override
    protected void get() {
        this.biomeManager = get(BiomeManager.class);
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

        BiomeHandle biomeHandle = create(BiomeHandle.class);
        biomeHandle.constructor(biomeData);

        return biomeHandle;
    }
}
