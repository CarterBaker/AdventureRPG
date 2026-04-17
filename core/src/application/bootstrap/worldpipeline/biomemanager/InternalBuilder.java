package application.bootstrap.worldpipeline.biomemanager;

import java.io.File;

import application.bootstrap.worldpipeline.biome.BiomeData;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.util.io.FileUtility;
import engine.util.registry.RegistryUtility;

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