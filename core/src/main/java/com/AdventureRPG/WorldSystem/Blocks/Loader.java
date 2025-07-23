package com.AdventureRPG.WorldSystem.Blocks;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;


public class Loader {

    public static Block[] LoadBlocks() {
        // 1. Load file from assets
        FileHandle file = Gdx.files.internal("blocks.json");
        String json = file.readString("UTF-8");

        // 2. Parse JSON
        Gson gson = new Gson();
        List<Block> blockList = gson.fromJson(json, new TypeToken<List<Block>>() {}.getType());

        // 3. Allocate the final array
        int maxID = 0;
        for (Block b : blockList)
            if (b.ID > maxID) maxID = b.ID;

        Block[] result = new Block[maxID + 1];
        for (Block b : blockList)
            result[b.ID] = b;

        // 4. Self-clean: all locals are GC eligible here, nothing retained
        return result;
    }
}
