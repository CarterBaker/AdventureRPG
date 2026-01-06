package com.AdventureRPG.WorldPipeline.blocks;

import java.util.List;

import com.AdventureRPG.core.engine.settings.EngineSetting;
import com.AdventureRPG.core.shaderpipeline.materialmanager.MaterialManager;
import com.AdventureRPG.core.shaderpipeline.texturemanager.TextureManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Loader {

    public static Block[] LoadBlocks(
            Gson gson,
            TextureManager textureManager,
            MaterialManager materialManager) {

        FileHandle file = Gdx.files.internal(EngineSetting.BLOCK_JSON_PATH);
        String json = file.readString("UTF-8");

        List<Builder> builders = gson.fromJson(json, new TypeToken<List<Builder>>() {
        }.getType());

        Block[] result = new Block[builders.size()];

        for (int i = 0; i < builders.size(); i++)
            result[i] = builders.get(i).build(
                    textureManager,
                    materialManager,
                    i);

        return result;
    }
}
