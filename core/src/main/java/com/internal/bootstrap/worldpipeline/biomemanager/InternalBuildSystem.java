package com.internal.bootstrap.worldpipeline.biomemanager;

import java.io.File;

import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.RegistryUtility;

public class InternalBuildSystem extends SystemPackage {

    // Compile \

    BiomeHandle compileBiome(File jsonFile, File root) {
        // e.g. root=biomes/ file=biomes/overworld/plains.json -> "overworld/plains"
        String biomeName = FileUtility.getPathWithFileNameWithoutExtension(root, jsonFile);
        short biomeID = RegistryUtility.toShortID(biomeName);

        BiomeHandle biome = create(BiomeHandle.class);
        biome.constructor(biomeName, biomeID);
        return biome;
    }

}