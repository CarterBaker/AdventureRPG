package com.internal.bootstrap.worldpipeline.blockmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometry;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.JsonUtility;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InternalBuildSystem extends SystemPackage {

    // Internal
    private TextureManager textureManager;
    private int blockCount;

    // Base \\

    @Override
    protected void create() {
        this.blockCount = 0;
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
    }

    // Compile \\

    ObjectArrayList<BlockHandle> compileBlocks(File jsonFile) {

        JsonObject rootJson = JsonUtility.loadJsonObject(jsonFile);
        JsonArray blockArray = JsonUtility.validateArray(rootJson, "blocks");

        ObjectArrayList<BlockHandle> blocks = new ObjectArrayList<>();

        for (int i = 0; i < blockArray.size(); i++) {

            JsonObject blockJson = blockArray.get(i).getAsJsonObject();
            BlockHandle block = parseBlock(blockJson, jsonFile);

            if (block != null)
                blocks.add(block);
        }

        return blocks;
    }

    // Parse \\

    private BlockHandle parseBlock(JsonObject blockJson, File jsonFile) {

        // Parse name
        String blockName = JsonUtility.validateString(blockJson, "name");

        // Parse type (default to SOLID if not specified)
        String typeStr = blockJson.has("type") ? blockJson.get("type").getAsString() : "SOLID";
        DynamicGeometry blockType = parseBlockType(typeStr);

        // Parse textures
        int upTexture = -1;
        int downTexture = -1;
        int northTexture = -1;
        int southTexture = -1;
        int eastTexture = -1;
        int westTexture = -1;

        // Single texture for all faces
        if (blockJson.has("texture")) {
            String texturePath = blockJson.get("texture").getAsString();
            int textureID = textureManager.getTileIDFromTextureName(texturePath);

            upTexture = textureID;
            downTexture = textureID;
            northTexture = textureID;
            southTexture = textureID;
            eastTexture = textureID;
            westTexture = textureID;
        }

        // Individual face textures (override the single texture if specified)
        if (blockJson.has("upTex"))
            upTexture = textureManager.getTileIDFromTextureName(blockJson.get("upTex").getAsString());

        if (blockJson.has("downTex"))
            downTexture = textureManager.getTileIDFromTextureName(blockJson.get("downTex").getAsString());

        if (blockJson.has("northTex"))
            northTexture = textureManager.getTileIDFromTextureName(blockJson.get("northTex").getAsString());

        if (blockJson.has("southTex"))
            southTexture = textureManager.getTileIDFromTextureName(blockJson.get("southTex").getAsString());

        if (blockJson.has("eastTex"))
            eastTexture = textureManager.getTileIDFromTextureName(blockJson.get("eastTex").getAsString());

        if (blockJson.has("westTex"))
            westTexture = textureManager.getTileIDFromTextureName(blockJson.get("westTex").getAsString());

        // Fill missing textures with highest defined texture
        int highestTexture = findHighestDefinedTexture(
                upTexture, downTexture, northTexture, southTexture, eastTexture, westTexture);

        if (highestTexture == -1) {
            upTexture = -1;
            downTexture = -1;
            northTexture = -1;
            southTexture = -1;
            eastTexture = -1;
            westTexture = -1;
        }

        else { // Autofill missing faces with the best available texture

            if (upTexture == -1)
                upTexture = highestTexture;
            if (downTexture == -1)
                downTexture = highestTexture;
            if (northTexture == -1)
                northTexture = highestTexture;
            if (southTexture == -1)
                southTexture = highestTexture;
            if (eastTexture == -1)
                eastTexture = highestTexture;
            if (westTexture == -1)
                westTexture = highestTexture;
        }

        // Create block
        BlockHandle block = create(BlockHandle.class);
        block.constructor(
                blockName,
                blockCount++,
                blockType,
                upTexture,
                downTexture,
                northTexture,
                southTexture,
                eastTexture,
                westTexture);

        return block;
    }

    // Utility \\

    private DynamicGeometry parseBlockType(String typeStr) {

        try {
            return DynamicGeometry.valueOf(typeStr.toUpperCase());
        }

        catch (IllegalArgumentException e) {
            throwException("Invalid block type: " + typeStr);
            return null;
        }
    }

    private int findHighestDefinedTexture(int... textures) {

        int highest = -1;

        for (int texture : textures)
            if (texture > highest)
                highest = texture;

        return highest;
    }
}