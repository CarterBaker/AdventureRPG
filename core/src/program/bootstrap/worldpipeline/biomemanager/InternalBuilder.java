package program.bootstrap.worldpipeline.biomemanager;

import java.io.File;
import program.bootstrap.worldpipeline.biome.BiomeData;
import program.bootstrap.worldpipeline.biome.BiomeHandle;
import program.core.engine.BuilderPackage;
import program.core.util.FileUtility;
import program.core.util.RegistryUtility;
import program.core.util.mathematics.extras.Color;

class InternalBuilder extends BuilderPackage {

    // Build \\

    BiomeHandle build(File file, File root) {

        String biomeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short biomeID = RegistryUtility.toShortID(biomeName);

        BiomeData biomeData = new BiomeData(biomeName, biomeID, Color.WHITE);

        BiomeHandle biomeHandle = create(BiomeHandle.class);
        biomeHandle.constructor(biomeData);

        return biomeHandle;
    }
}