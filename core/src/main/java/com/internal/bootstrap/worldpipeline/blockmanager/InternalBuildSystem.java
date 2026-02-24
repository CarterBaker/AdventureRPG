package com.internal.bootstrap.worldpipeline.blockmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.block.BlockRotationType;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InternalBuildSystem extends SystemPackage {

    // Internal
    private TextureManager textureManager;
    private MaterialManager materialManager;
    private int blockCount;

    // Base \\

    @Override
    protected void create() {
        this.blockCount = 0;
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.materialManager = get(MaterialManager.class);
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

        // Parse geometry type
        String typeStr = blockJson.has("type") ? blockJson.get("type").getAsString() : "FULL";
        DynamicGeometryType blockType = parseBlockType(typeStr);

        // Parse rotation type
        BlockRotationType rotationType = BlockRotationType.NONE;
        if (blockJson.has("rotation")) {
            try {
                rotationType = BlockRotationType.valueOf(
                        blockJson.get("rotation").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                throwException("Invalid rotation type in block: " + blockName);
            }
        }

        // Parse material
        int materialID = -1;
        if (blockJson.has("material")) {
            String materialPath = blockJson.get("material").getAsString();
            materialID = materialManager.getMaterialIDFromMaterialName(materialPath);
        }

        // Build texture array indexed by Direction3Vector ordinal
        int[] textures = new int[Direction3Vector.LENGTH];
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            textures[i] = -1;

        // Single texture for all faces
        if (blockJson.has("texture")) {
            int textureID = textureManager.getTileIDFromTextureName(
                    blockJson.get("texture").getAsString());
            for (int i = 0; i < Direction3Vector.LENGTH; i++)
                textures[i] = textureID;
        }

        // Per-face overrides
        for (Direction3Vector dir : Direction3Vector.VALUES) {
            String key = dir.name().toLowerCase() + "Tex";
            if (blockJson.has(key))
                textures[dir.ordinal()] = textureManager.getTileIDFromTextureName(
                        blockJson.get(key).getAsString());
        }

        // Cascade — fill undefined faces from the previous defined face
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

        // Create block
        BlockHandle block = create(BlockHandle.class);
        block.constructor(
                blockName,
                blockCount++,
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