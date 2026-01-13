package com.internal.bootstrap.worldpipeline.biomemanager;

import java.io.File;

import com.google.gson.JsonObject;
import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

public class InternalBuildSystem extends SystemPackage {

    // Internal
    private int biomeCount;

    // Base \\

    @Override
    protected void create() {
        this.biomeCount = 0;
    }

    // Compile \\

    BiomeHandle compileBiome(File jsonFile) {

        JsonObject rootJson = JsonUtility.loadJsonObject(jsonFile);

        // Get biome name from the file name
        String biomeName = FileUtility.getFileName(jsonFile);

        // Create biome
        BiomeHandle biome = create(BiomeHandle.class);
        biome.constructor(biomeName, biomeCount++);

        return biome;
    }
}