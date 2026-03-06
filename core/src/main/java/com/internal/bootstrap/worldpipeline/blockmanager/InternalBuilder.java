package com.internal.bootstrap.worldpipeline.blockmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.block.BlockRotationType;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.RegistryUtility;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

    // Internal
    private TextureManager textureManager;
    private MaterialManager materialManager;

    // Base \\

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.materialManager = get(MaterialManager.class);
    }

    // Build \\

    ObjectArrayList<BlockHandle> build(File jsonFile, File root) {

        String pathPrefix = FileUtility.getPathWithFileNameWithoutExtension(root, jsonFile);
        JsonObject rootJson = JsonUtility.loadJsonObject(jsonFile);
        JsonArray blockArray = JsonUtility.validateArray(rootJson, "blocks");

        ObjectArrayList<BlockHandle> blocks = new ObjectArrayList<>();

        for (int i = 0; i < blockArray.size(); i++) {
            JsonObject blockJson = blockArray.get(i).getAsJsonObject();
            BlockHandle block = parseBlock(blockJson, pathPrefix);
            if (block != null)
                blocks.add(block);
        }

        return blocks;
    }

    // Parse \\

    private BlockHandle parseBlock(JsonObject blockJson, String pathPrefix) {

        String localName = JsonUtility.validateString(blockJson, "name");
        String blockName = pathPrefix + "/" + localName;
        short blockID = RegistryUtility.toShortID(blockName);

        String typeStr = blockJson.has("type") ? blockJson.get("type").getAsString() : "FULL";
        DynamicGeometryType blockType = parseBlockType(typeStr);

        BlockRotationType rotationType = BlockRotationType.NONE;
        if (blockJson.has("rotation")) {
            try {
                rotationType = BlockRotationType.valueOf(
                        blockJson.get("rotation").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                throwException("Invalid rotation type in block: " + blockName);
            }
        }

        int materialID = -1;
        if (blockJson.has("material")) {
            String materialPath = blockJson.get("material").getAsString();
            materialID = materialManager.getMaterialIDFromMaterialName(materialPath);
        }

        int[] textures = new int[Direction3Vector.LENGTH];
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            textures[i] = -1;

        if (blockJson.has("texture")) {
            int textureID = textureManager.getHandleFromTextureName(
                    blockJson.get("texture").getAsString()).getTileID();
            for (int i = 0; i < Direction3Vector.LENGTH; i++)
                textures[i] = textureID;
        }

        for (Direction3Vector dir : Direction3Vector.VALUES) {
            String key = dir.name().toLowerCase() + "Tex";
            if (blockJson.has(key))
                textures[dir.ordinal()] = textureManager.getHandleFromTextureName(
                        blockJson.get(key).getAsString()).getTileID();
        }

        int lastDefined = -1;
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            if (textures[i] != -1) {
                lastDefined = textures[i];
                break;
            }

        if (lastDefined != -1) {
            for (int i = 0; i < Direction3Vector.LENGTH; i++) {
                if (textures[i] == -1)
                    textures[i] = lastDefined;
                else
                    lastDefined = textures[i];
            }
        }

        BlockHandle block = create(BlockHandle.class);
        block.constructor(
                blockName,
                blockID,
                blockType,
                rotationType,
                materialID,
                textures[Direction3Vector.NORTH.ordinal()],
                textures[Direction3Vector.EAST.ordinal()],
                textures[Direction3Vector.SOUTH.ordinal()],
                textures[Direction3Vector.WEST.ordinal()],
                textures[Direction3Vector.UP.ordinal()],
                textures[Direction3Vector.DOWN.ordinal()]);

        return block;
    }

    // Utility \\

    private DynamicGeometryType parseBlockType(String typeStr) {
        try {
            return DynamicGeometryType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throwException("Invalid block type: " + typeStr);
            return null;
        }
    }
}