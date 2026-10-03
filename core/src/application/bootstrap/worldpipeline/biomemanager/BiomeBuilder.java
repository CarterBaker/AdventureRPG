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
     * BiomeArpgUtility, the one definition of the format. Everything is
     * validated at load, so a malformed biome fails at boot.
     */

    // Build \\

    BiomeHandle build(File file, File root) {

        String biomeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        BiomeData biomeData;

        try {
            biomeData = BiomeArpgUtility.parse(biomeName, arpg);
        } catch (InternalException e) {
            return throwException(e.getMessage(), e.getCause());
        }

        BiomeHandle biomeHandle = create(BiomeHandle.class);
        biomeHandle.constructor(biomeData);

        return biomeHandle;
    }
}
