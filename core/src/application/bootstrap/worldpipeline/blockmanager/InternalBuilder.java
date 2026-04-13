package application.bootstrap.worldpipeline.blockmanager;

import java.io.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.itempipeline.tooltypemanager.ToolTypeManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.block.BlockData;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockRotationType;
import application.core.engine.BuilderPackage;
import application.core.settings.EngineSetting;
import application.core.util.FileUtility;
import application.core.util.JsonUtility;
import application.core.util.RegistryUtility;
import application.core.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

    // Internal
    private TextureManager textureManager;
    private MaterialManager materialManager;
    private ToolTypeManager toolTypeManager;

    // Base \\

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.materialManager = get(MaterialManager.class);
        this.toolTypeManager = get(ToolTypeManager.class);
    }

    // Build \\

    ObjectArrayList<BlockHandle> build(File file, File root) {

        String pathPrefix = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        JsonObject rootJson = JsonUtility.loadJsonObject(file);
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

        // Identity
        String localName = JsonUtility.validateString(blockJson, "name");
        String blockName = pathPrefix + "/" + localName;
        short blockID = RegistryUtility.toShortID(blockName);

        // Geometry
        String typeStr = JsonUtility.getString(blockJson, "type", "FULL");
        DynamicGeometryType blockType = parseBlockType(typeStr);

        // Rotation
        BlockRotationType rotationType = BlockRotationType.NONE;
        if (blockJson.has("rotation")) {
            try {
                rotationType = BlockRotationType.valueOf(
                        blockJson.get("rotation").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                throwException("Invalid rotation type in block: " + blockName);
            }
        }

        // Material
        int materialID = -1;
        if (blockJson.has("material")) {
            String materialPath = blockJson.get("material").getAsString();
            materialID = materialManager.getMaterialIDFromMaterialName(materialPath);
        }

        // Textures
        int[] textures = new int[Direction3Vector.LENGTH];
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            textures[i] = -1;

        if (blockJson.has("texture")) {
            int textureID = textureManager.getTextureHandleFromTextureName(
                    blockJson.get("texture").getAsString()).getTileID();
            for (int i = 0; i < Direction3Vector.LENGTH; i++)
                textures[i] = textureID;
        }

        for (Direction3Vector dir : Direction3Vector.VALUES) {
            String key = dir.name().toLowerCase() + "Tex";
            if (blockJson.has(key))
                textures[dir.ordinal()] = textureManager.getTextureHandleFromTextureName(
                        blockJson.get(key).getAsString()).getTileID();
        }

        int lastDefined = -1;
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            if (textures[i] != -1) {
                lastDefined = textures[i];
                break;
            }

        if (lastDefined != -1)
            for (int i = 0; i < Direction3Vector.LENGTH; i++) {
                if (textures[i] == -1)
                    textures[i] = lastDefined;
                else
                    lastDefined = textures[i];
            }

        // Breaking
        int breakTier = JsonUtility.getInt(blockJson, "break_tier", 0);
        int durability = JsonUtility.getInt(blockJson, "durability", 1);

        short requiredToolTypeID = EngineSetting.TOOL_NONE;
        if (blockJson.has("required_tool")) {
            String toolPath = blockJson.get("required_tool").getAsString();
            requiredToolTypeID = toolTypeManager.getToolTypeIDFromToolTypeName(toolPath);
        }

        // Construct
        BlockData blockData = new BlockData(
                blockName, blockID,
                blockType, rotationType,
                materialID,
                textures[Direction3Vector.NORTH.ordinal()],
                textures[Direction3Vector.EAST.ordinal()],
                textures[Direction3Vector.SOUTH.ordinal()],
                textures[Direction3Vector.WEST.ordinal()],
                textures[Direction3Vector.UP.ordinal()],
                textures[Direction3Vector.DOWN.ordinal()],
                breakTier, requiredToolTypeID, durability);

        BlockHandle blockHandle = create(BlockHandle.class);
        blockHandle.constructor(blockData);

        return blockHandle;
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