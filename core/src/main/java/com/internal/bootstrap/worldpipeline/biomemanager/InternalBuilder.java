package com.internal.bootstrap.worldpipeline.biomemanager;

import java.io.File;
import com.internal.bootstrap.worldpipeline.biome.BiomeData;
import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.RegistryUtility;
import com.internal.core.util.mathematics.extras.Color;

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