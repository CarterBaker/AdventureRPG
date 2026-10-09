package application.bootstrap.worldpipeline.blockmanager;

import java.io.File;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.itempipeline.tooltypemanager.ToolTypeManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.block.BlockData;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockRotationType;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class BlockBuilder extends BuilderPackage {

    /*
     * Parses block ARPG into BlockData wrapped in a BlockHandle, validating
     * geometry type, textures, durability, tooling and, for liquids, viscosity.
     * Each face's map color is its texture's average albedo, and undefined on
     * an untextured face.
     * Every breakable solid block must name the tool that breaks it and the
     * item texture its block piece is drawn with, since breaking it hands
     * those pieces out. Bootstrap only.
     */

    // Internal
    private BlockManager blockManager;
    private TextureManager textureManager;
    private MaterialManager materialManager;
    private ToolTypeManager toolTypeManager;

    // Base \\

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
        this.textureManager = get(TextureManager.class);
        this.materialManager = get(MaterialManager.class);
        this.toolTypeManager = get(ToolTypeManager.class);
    }

    // Build \\

    ObjectArrayList<BlockHandle> build(File file, File root) {

        String pathPrefix = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(file);
        ArpgArrayStruct blockArray = ArpgUtility.validateArray(rootArpg, "blocks");

        ObjectArrayList<BlockHandle> blocks = new ObjectArrayList<>();

        for (int i = 0; i < blockArray.size(); i++) {
            ArpgObjectStruct blockArpg = blockArray.get(i).getAsObject();
            BlockHandle block = parseBlock(blockArpg, pathPrefix);
            if (block != null)
                blocks.add(block);
        }

        return blocks;
    }

    // Parse \\

    private BlockHandle parseBlock(ArpgObjectStruct blockArpg, String pathPrefix) {

        // Identity
        String localName = ArpgUtility.validateString(blockArpg, "name");
        String blockName = pathPrefix + "/" + localName;
        short blockID = blockManager.registerBlockName(blockName);

        // Geometry
        String typeStr = ArpgUtility.getString(blockArpg, "type", "FULL");
        DynamicGeometryType blockType = parseBlockType(typeStr);

        // Rotation
        BlockRotationType rotationType = BlockRotationType.NONE;
        if (blockArpg.has("rotation")) {
            try {
                rotationType = BlockRotationType.valueOf(
                        blockArpg.get("rotation").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                throwException("Invalid rotation type in block: " + blockName);
            }
        }

        // Natural
        boolean natural = ArpgUtility.getBoolean(blockArpg, "natural", false);

        // Material
        int materialID = -1;
        if (blockArpg.has("material")) {
            String materialPath = blockArpg.get("material").getAsString();
            materialID = materialManager.getMaterialIDFromMaterialName(materialPath);
        }

        // Textures
        int[] textures = new int[Direction3Vector.LENGTH];
        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            textures[i] = -1;

        if (blockArpg.has("texture")) {
            int textureID = textureManager.getTextureHandleFromTextureName(
                    blockArpg.get("texture").getAsString()).getTileID();
            for (int i = 0; i < Direction3Vector.LENGTH; i++)
                textures[i] = textureID;
        }

        for (Direction3Vector dir : Direction3Vector.VALUES) {
            String key = dir.name().toLowerCase() + "Tex";
            if (blockArpg.has(key))
                textures[dir.ordinal()] = textureManager.getTextureHandleFromTextureName(
                        blockArpg.get(key).getAsString()).getTileID();
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

        // Map Colors
        int[] faceMapColors = resolveFaceMapColors(textures);

        // Breaking
        int breakTier = ArpgUtility.getInt(blockArpg, "break_tier", 0);
        int durability = ArpgUtility.getInt(blockArpg, "durability", 1);

        short requiredToolTypeID = EngineSetting.TOOL_NONE;
        if (blockArpg.has("required_tool")) {
            String toolPath = blockArpg.get("required_tool").getAsString();
            requiredToolTypeID = toolTypeManager.getToolTypeIDFromToolTypeName(toolPath);
        }

        String itemTextureName = ArpgUtility.getString(
                blockArpg, "item_texture", EngineSetting.BLOCK_ITEM_TEXTURE_NONE);

        if (isBreakableSolid(blockType, breakTier))
            validatePieceSource(blockName, requiredToolTypeID, itemTextureName);
        else if (!itemTextureName.isEmpty())
            throwException("Block \"" + blockName + "\" names an \"item_texture\" but cannot be broken into pieces.");

        // Physics — viscosity (Pa·s) is required for LIQUID blocks, since the
        // physics pipeline has no sane fallback for how fast an undefined
        // liquid should flow. Optional and stored as-is for anything else.
        float viscosity = EngineSetting.BLOCK_VISCOSITY_UNDEFINED;

        if (blockType == DynamicGeometryType.LIQUID) {
            if (!blockArpg.has("viscosity"))
                throwException("Liquid block \"" + blockName + "\" is missing required \"viscosity\" (Pa\u00b7s).");
            viscosity = blockArpg.get("viscosity").getAsFloat();
        } else if (blockArpg.has("viscosity")) {
            viscosity = blockArpg.get("viscosity").getAsFloat();
        }

        // Construct
        BlockData blockData = new BlockData(
                blockName, localName, blockID,
                blockType, rotationType, natural,
                materialID,
                textures[Direction3Vector.NORTH.ordinal()],
                textures[Direction3Vector.EAST.ordinal()],
                textures[Direction3Vector.SOUTH.ordinal()],
                textures[Direction3Vector.WEST.ordinal()],
                textures[Direction3Vector.UP.ordinal()],
                textures[Direction3Vector.DOWN.ordinal()],
                faceMapColors,
                breakTier, requiredToolTypeID, durability,
                itemTextureName,
                viscosity);

        BlockHandle blockHandle = create(BlockHandle.class);
        blockHandle.constructor(blockData);

        return blockHandle;
    }

    // Map Colors \\

    private int[] resolveFaceMapColors(int[] textures) {

        int[] faceMapColors = new int[Direction3Vector.LENGTH];

        for (int i = 0; i < Direction3Vector.LENGTH; i++)
            faceMapColors[i] = textures[i] != EngineSetting.BLOCK_TEXTURE_UNDEFINED
                    ? textureManager.getTextureHandleFromTileID(textures[i]).getAverageColor()
                    : EngineSetting.BLOCK_MAP_COLOR_UNDEFINED;

        return faceMapColors;
    }

    // Breaking \\

    private boolean isBreakableSolid(DynamicGeometryType blockType, int breakTier) {
        return breakTier >= 0
                && blockType != DynamicGeometryType.NONE
                && blockType != DynamicGeometryType.LIQUID;
    }

    private void validatePieceSource(String blockName, short requiredToolTypeID, String itemTextureName) {

        if (requiredToolTypeID == EngineSetting.TOOL_NONE)
            throwException("Breakable block \"" + blockName + "\" must name the \"required_tool\" that breaks it.");

        if (itemTextureName.isEmpty())
            throwException("Breakable block \"" + blockName + "\" must name the \"item_texture\" its block piece "
                    + "is drawn with — a texture from the item texture array.");

        textureManager.getTileIDFromTextureName(itemTextureName);
    }

    // Utility \\

    private DynamicGeometryType parseBlockType(String typeStr) {

        DynamicGeometryType blockType = null;

        try {
            blockType = DynamicGeometryType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throwException("Invalid block type: " + typeStr);
        }

        if (blockType == DynamicGeometryType.PARTIAL)
            throwException("Block type PARTIAL is reserved for cells subdivided into sub-blocks — "
                    + "declare the block FULL and subdivide it in the world instead");

        return blockType;
    }
}