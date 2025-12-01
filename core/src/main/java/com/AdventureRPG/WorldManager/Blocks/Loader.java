package com.AdventureRPG.WorldManager.Blocks;

import java.util.List;

import com.AdventureRPG.Core.RenderPipeline.MaterialManager.MaterialSystem;
import com.AdventureRPG.Core.RenderPipeline.TextureManager.TextureManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Loader {

    public static Block[] LoadBlocks(
            Gson gson,
            TextureManager textureManager,
            MaterialSystem materialSystem) {

        FileHandle file = Gdx.files.internal("blocks.json");
        String json = file.readString("UTF-8");

        List<Builder> builders = gson.fromJson(json, new TypeToken<List<Builder>>() {
        }.getType());

        Block[] result = new Block[builders.size()];

        for (int i = 0; i < builders.size(); i++)
            result[i] = builders.get(i).build(
                    textureManager,
                    materialSystem,
                    i);

        return result;
    }
}
