package com.AdventureRPG.WorldSystem.Blocks;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

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
            if (b.id > maxID)
                maxID = b.id;

        Block[] result = new Block[maxID + 1];
        for (Block b : blockList) {
            if (result[b.id] != null)
                throw new RuntimeException("Duplicate block ID detected: " + b.id + " (" + b.name + ")");
            result[b.id] = b;
        }

        return result;
    }
}
