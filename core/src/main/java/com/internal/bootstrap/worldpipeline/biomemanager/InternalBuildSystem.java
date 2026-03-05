package com.internal.bootstrap.worldpipeline.biomemanager;

import java.io.File;

import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.RegistryUtility;

class InternalBuildSystem extends BuilderPackage {

    // Build \\

    BiomeHandle build(File jsonFile, File root) {
        String biomeName = FileUtility.getPathWithFileNameWithoutExtension(root, jsonFile);
        short biomeID = RegistryUtility.toShortID(biomeName);
        BiomeHandle biome = create(BiomeHandle.class);
        biome.constructor(biomeName, biomeID);
        return biome;
    }
}