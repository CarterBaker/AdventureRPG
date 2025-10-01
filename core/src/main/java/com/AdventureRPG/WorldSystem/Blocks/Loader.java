package com.AdventureRPG.WorldSystem.Blocks;

import java.util.List;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.reflect.TypeToken;

public class Loader {

    public static Block[] LoadBlocks(GameManager gameManager, WorldSystem worldSystem) {

        FileHandle file = Gdx.files.internal("blocks.json");
        String json = file.readString("UTF-8");

        List<Builder> builders = gameManager.gson.fromJson(json, new TypeToken<List<Builder>>() {
        }.getType());

        Block[] result = new Block[builders.size()];

        for (int i = 0; i < builders.size(); i++)
            result[i] = builders.get(i).build(worldSystem, i);

        return result;
    }
}
