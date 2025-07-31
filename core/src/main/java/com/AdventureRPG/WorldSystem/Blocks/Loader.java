package com.AdventureRPG.WorldSystem.Blocks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.util.List;

public class Loader {

    public static Block[] LoadBlocks() {
        // Load file
        FileHandle file = Gdx.files.internal("blocks.json");
        String json = file.readString("UTF-8");

        // Use GSON with custom Block deserializer
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Block.class, new BlockDeserializer())
                .create();

        List<Block> blockList = gson.fromJson(json, new TypeToken<List<Block>>() {
        }.getType());

        // Allocate based on max ID
        int maxID = 0;
        for (Block b : blockList)
            if (b.ID > maxID)
                maxID = b.ID;

        Block[] result = new Block[maxID + 1];
        for (Block b : blockList) {
            if (result[b.ID] != null)
                throw new RuntimeException("Duplicate block ID detected: " + b.ID + " (" + b.name + ")");
            result[b.ID] = b;
        }

        return result;
    }
}
