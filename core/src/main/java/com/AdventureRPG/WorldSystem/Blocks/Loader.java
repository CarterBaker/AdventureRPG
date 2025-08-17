package com.AdventureRPG.WorldSystem.Blocks;

import java.util.List;

import com.AdventureRPG.TextureManager.TextureManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

// TODO: AI largely made most of this. It needs intense scrutiny when I have time
public class Loader {

    // debug
    private static final boolean debug = true; // TODO: Remove debug line

    public static Block[] LoadBlocks(TextureManager textureManager) {

        FileHandle file = Gdx.files.internal("blocks.json");
        String json = file.readString("UTF-8");

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Builder.class, new BlockDeserializer())
                .create();

        List<Builder> builders = gson.fromJson(json, new TypeToken<List<Builder>>() {
        }.getType());

        Block[] result = new Block[builders.size()];

        for (int i = 0; i < builders.size(); i++)
            result[i] = builders.get(i).build(textureManager, i);

        if (debug) // TODO: Remove debug line
            for (Block block : result) {
                System.out.println("______________________________");
                System.out.println(block.name);
                System.out.println(block.id);
                System.out.println("");
                System.out.println(block.up);
                System.out.println(block.north);
                System.out.println(block.south);
                System.out.println(block.east);
                System.out.println(block.west);
                System.out.println(block.down);
                System.out.println("");
                System.out.println(block.state.toString());
            }

        return result;
    }
}
